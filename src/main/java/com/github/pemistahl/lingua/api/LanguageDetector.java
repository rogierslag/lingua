/*
 * Copyright © 2018-today Peter M. Stahl pemistahl@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pemistahl.lingua.api;

import static com.github.pemistahl.lingua.internal.Constant.CHARS_TO_LANGUAGES_MAPPING;
import static com.github.pemistahl.lingua.internal.Constant.MULTIPLE_WHITESPACE;
import static com.github.pemistahl.lingua.internal.Constant.NO_LETTER;
import static com.github.pemistahl.lingua.internal.Constant.NUMBERS;
import static com.github.pemistahl.lingua.internal.Constant.PUNCTUATION;
import static com.github.pemistahl.lingua.internal.Constant.isJapaneseAlphabet;
import static com.github.pemistahl.lingua.internal.util.extension.CharExtensions.isLogogram;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import com.github.pemistahl.lingua.internal.Alphabet;
import com.github.pemistahl.lingua.internal.Ngram;
import com.github.pemistahl.lingua.internal.TestDataLanguageModel;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

/**
 * Detects the language of given input text.
 */
public class LanguageDetector {
    private static final int HIGH_ACCURACY_MODE_MAX_TEXT_LENGTH = 120;

    static final Map<Language, Object2FloatMap<String>> unigramLanguageModels = new EnumMap<>(Language.class);
    static final Map<Language, Object2FloatMap<String>> bigramLanguageModels = new EnumMap<>(Language.class);
    static final Map<Language, Object2FloatMap<String>> trigramLanguageModels = new EnumMap<>(Language.class);
    static final Map<Language, Object2FloatMap<String>> quadrigramLanguageModels =
        new EnumMap<>(Language.class);
    static final Map<Language, Object2FloatMap<String>> fivegramLanguageModels = new EnumMap<>(Language.class);

    private final Set<Language> languages;
    private final double minimumRelativeDistance;
    private final boolean isEveryLanguageModelPreloaded;
    private final boolean isLowAccuracyModeEnabled;
    private final int numberOfLoadedLanguages;

    // Assuming Alphabet and other related classes are already defined in Java
    private final Set<Language> languagesWithUniqueCharacters;
    private final Map<Alphabet, Language> oneLanguageAlphabets;

    public LanguageDetector(Set<Language> languages, double minimumRelativeDistance,
        boolean isEveryLanguageModelPreloaded, boolean isLowAccuracyModeEnabled) {
        this.languages = new HashSet<>(languages);
        this.minimumRelativeDistance = minimumRelativeDistance;
        this.isEveryLanguageModelPreloaded = isEveryLanguageModelPreloaded;
        this.isLowAccuracyModeEnabled = isLowAccuracyModeEnabled;
        this.numberOfLoadedLanguages = languages.size();

        this.languagesWithUniqueCharacters = languages.stream()
            .filter(language -> language.getUniqueCharacters().length() > 0)
            .collect(Collectors.toSet());
        this.oneLanguageAlphabets = Alphabet.allSupportingExactlyOneLanguage()
            .entrySet()
            .stream()
            .filter(entry -> languages.contains(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (isEveryLanguageModelPreloaded) {
            preloadLanguageModels();
        }
    }

    public Set<Language> getLanguages() {
        return languages;
    }

    private void preloadLanguageModels() {
        List<Callable<Void>> tasks = new ArrayList<>();

        for (Language language : languages) {
            tasks.add(() -> {
                loadLanguageModels(unigramLanguageModels, language, 1);
                return null;
            });
            tasks.add(() -> {
                loadLanguageModels(bigramLanguageModels, language, 2);
                return null;
            });
            tasks.add(() -> {
                loadLanguageModels(trigramLanguageModels, language, 3);
                return null;
            });
            tasks.add(() -> {
                loadLanguageModels(quadrigramLanguageModels, language, 4);
                return null;
            });
            tasks.add(() -> {
                loadLanguageModels(fivegramLanguageModels, language, 5);
                return null;
            });
        }

        ForkJoinPool.commonPool().invokeAll(tasks);
    }

    private static Object2FloatMap<String> loadLanguageModels(Map<Language, Object2FloatMap<String>> languageModels,
        Language language, int ngramLength) {
        synchronized (languageModels) {
            if (!languageModels.containsKey(language)) {
                // Load the language model for the given language and ngram length
                // This might involve reading from a file or resource and deserializing the content
                Object2FloatMap<String> model = loadLanguageModel(language, ngramLength);
                languageModels.put(language, model);
            }
            return languageModels.get(language);
        }
    }

    private static Object2FloatMap<String> loadLanguageModel(Language language, int ngramLength) {
        String fileName = Ngram.getNgramNameByLength(ngramLength) + "s.json";
        String filePath = "/language-models/" + language.getIsoCode639_1() + "/" + fileName;
        try (InputStream inputStream = Language.class.getResourceAsStream(filePath)) {
            if (inputStream == null) {
                return new Object2FloatOpenHashMap<>();
            }
            // FIXME
            // return TrainingDataLanguageModel.fromJson(inputStream); // Adjust this line according to your JSON
            // parsing logic
            return new Object2FloatOpenHashMap<>();
        }
        catch (IOException e) {
            // Handle exceptions appropriately
            return new Object2FloatOpenHashMap<>();
        }
    }

    /**
     * Detects the language of given input text.
     *
     * @param text The input text to detect the language for.
     * @return The identified language or [Language.UNKNOWN].
     */
    public Language detectLanguageOf(String text) {
        SortedMap<Language, Double> confidenceValues = computeLanguageConfidenceValues(text);

        if (confidenceValues.isEmpty()) {
            return Language.UNKNOWN;
        }

        Iterator<Map.Entry<Language, Double>> iterator = confidenceValues.entrySet().iterator();
        Map.Entry<Language, Double> mostLikelyLanguageEntry = iterator.next();

        Language mostLikelyLanguage = mostLikelyLanguageEntry.getKey();
        if (confidenceValues.size() == 1) {
            return mostLikelyLanguage;
        }

        double mostLikelyLanguageProbability = mostLikelyLanguageEntry.getValue();
        double secondMostLikelyLanguageProbability = iterator.next().getValue();

        if (mostLikelyLanguageProbability == secondMostLikelyLanguageProbability
            || (mostLikelyLanguageProbability - secondMostLikelyLanguageProbability) < minimumRelativeDistance) {
            return Language.UNKNOWN;
        }

        return mostLikelyLanguage;
    }


    /**
     * Computes confidence values for every language considered possible for the given input text.
     * <p>
     * The values that this method computes are part of a **relative** confidence metric, not of an absolute one.
     * Each value is a number between 0.0 and 1.0. The most likely language is always returned with value 1.0.
     * All other languages get values assigned which are lower than 1.0, denoting how less likely those languages
     * are in comparison to the most likely language.
     * <p>
     * The map returned by this method does not necessarily contain all languages which the calling instance of
     * [LanguageDetector] was built from. If the rule-based engine decides that a specific language is truly impossible,
     * then it will not be part of the returned map. Likewise, if no ngram probabilities can be found within the
     * detector's languages for the given input text, the returned map will be empty. The confidence value for
     * each language not being part of the returned map is assumed to be 0.0.
     *
     * @param text The input text to detect the language for.
     * @return A map of all possible languages, sorted by their confidence value in descending order.
     */
    public SortedMap<Language, Double> computeLanguageConfidenceValues(String text) {
        TreeMap<Language, Double> values = new TreeMap<>();
        String cleanedUpText = cleanUpInputText(text);

        if (cleanedUpText.isEmpty() || NO_LETTER.matcher(cleanedUpText).matches()) {
            return values;
        }

        List<String> words = splitTextIntoWords(cleanedUpText);
        Language languageDetectedByRules = detectLanguageWithRules(words);

        if (languageDetectedByRules != Language.UNKNOWN) {
            values.put(languageDetectedByRules, 1.0);
            return values;
        }

        Set<Language> filteredLanguages = filterLanguagesByRules(words);

        if (filteredLanguages.size() == 1) {
            Language filteredLanguage = filteredLanguages.iterator().next();
            values.put(filteredLanguage, 1.0);
            return values;
        }

        if (isLowAccuracyModeEnabled && cleanedUpText.length() < 3) {
            return values;
        }

        List<Integer> ngramSizeRange =
            cleanedUpText.length() >= HIGH_ACCURACY_MODE_MAX_TEXT_LENGTH || isLowAccuracyModeEnabled
                ? Collections.singletonList(3)
                : Arrays.asList(1, 2, 3, 4, 5);

        List<Callable<Map<Language, Double>>> tasks = ngramSizeRange.stream()
            .filter(i -> cleanedUpText.length() >= i)
            .map(i -> (Callable<Map<Language, Double>>) () -> {
                TestDataLanguageModel testDataModel = TestDataLanguageModel.fromText(cleanedUpText, i);
                // Implement computeLanguageProbabilities and other methods used here
                Map<Language, Double> probabilities = computeLanguageProbabilities(testDataModel, filteredLanguages);
                return probabilities; // or any additional logic required
            })
            .collect(Collectors.toList());

        List<Map<Language, Double>> allProbabilities = ForkJoinPool.commonPool().invokeAll(tasks).stream()
            .map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        // Sum up probabilities
        Map<Language, Double> summedUpProbabilities = new HashMap<>();
        for (Map<Language, Double> probabilityMap : allProbabilities) {
            for (Map.Entry<Language, Double> entry : probabilityMap.entrySet()) {
                summedUpProbabilities.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        // Find the highest probability
        double highestProbability = summedUpProbabilities.values().stream()
            .max(Double::compare)
            .orElse(0.0);

        // Calculate confidence values
        SortedMap<Language, Double> confidenceValues = new TreeMap<>(
            Comparator.comparingDouble((Language lang) -> summedUpProbabilities.getOrDefault(lang, 0.0)).reversed()
        );
        for (Map.Entry<Language, Double> entry : summedUpProbabilities.entrySet()) {
            double confidence = highestProbability > 0 ? entry.getValue() / highestProbability : 0.0;
            confidenceValues.put(entry.getKey(), confidence);
        }

        return confidenceValues;
    }

    String cleanUpInputText(String text) {
        return text.trim().toLowerCase()
            .replaceAll(PUNCTUATION.pattern(), "")
            .replaceAll(NUMBERS.pattern(), "")
            .replaceAll(MULTIPLE_WHITESPACE.pattern(), " ");
    }

    List<String> splitTextIntoWords(String text) {
        List<String> words = new ArrayList<>();
        int nextWordStart = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                if (nextWordStart != i) {
                    words.add(text.substring(nextWordStart, i));
                }
                nextWordStart = i + 1;
            }
            else if (isLogogram(c)) {
                if (nextWordStart != i) {
                    words.add(text.substring(nextWordStart, i));
                }

                words.add(Character.toString(c));
                nextWordStart = i + 1;
            }
        }

        if (nextWordStart != text.length()) {
            words.add(text.substring(nextWordStart));
        }
        return words;
    }


    private Map<Language, Integer> countUnigramsOfInputText(TestDataLanguageModel unigramLanguageModel,
        Set<Language> filteredLanguages) {
        Map<Language, Integer> unigramCounts = new HashMap<>();
        for (Language language : filteredLanguages) {
            for (Ngram unigram : unigramLanguageModel.getNgrams()) {
                float probability = lookUpNgramProbability(language, unigram);
                if (probability > 0) {
                    unigramCounts.merge(language, 1, Integer::sum);
                }
            }
        }
        return unigramCounts;
    }

    private Map<Language, Float> sumUpProbabilities(
        List<Map<Language, Float>> probabilities,
        Map<Language, Integer> unigramCountsOfInputText,
        Set<Language> filteredLanguages) {

        Map<Language, Float> summedUpProbabilities = new HashMap<>();
        for (Language language : filteredLanguages) {
            float sum = 0.0f;
            for (Map<Language, Float> probabilityMap : probabilities) {
                sum += probabilityMap.getOrDefault(language, 0.0f);
            }
            summedUpProbabilities.put(language, sum);

            if (unigramCountsOfInputText.containsKey(language)) {
                float normalizedValue = sum / unigramCountsOfInputText.get(language);
                summedUpProbabilities.put(language, normalizedValue);
            }
        }

        // Remove languages with a probability of 0
        summedUpProbabilities.values().removeIf(v -> v == 0.0f);
        return summedUpProbabilities;
    }

    public Language detectLanguageWithRules(List<String> words) {
        Map<Language, Integer> totalLanguageCounts = new HashMap<>();

        for (String word : words) {
            Map<Language, Integer> wordLanguageCounts = new HashMap<>();

            for (char character : word.toCharArray()) {
                boolean isMatch = false;
                for (Map.Entry<Alphabet, Language> entry : oneLanguageAlphabets.entrySet()) {
                    Alphabet alphabet = entry.getKey();
                    Language language = entry.getValue();
                    if (alphabet.matches(character)) {
                        wordLanguageCounts.merge(language, 1, Integer::sum);
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch) {
                    if (Alphabet.HAN.matches(character)) {
                        wordLanguageCounts.merge(Language.CHINESE, 1, Integer::sum);
                    }
                    else if (isJapaneseAlphabet(character)) {
                        wordLanguageCounts.merge(Language.JAPANESE, 1, Integer::sum);
                    }
                    else if (Alphabet.LATIN.matches(character) ||
                        Alphabet.CYRILLIC.matches(character) ||
                        Alphabet.DEVANAGARI.matches(character)) {
                        for (Language lang : languagesWithUniqueCharacters) {
                            if (lang.getUniqueCharacters().contains(String.valueOf(character))) {
                                wordLanguageCounts.merge(lang, 1, Integer::sum);
                            }
                        }
                    }
                }
            }

            if (wordLanguageCounts.isEmpty()) {
                totalLanguageCounts.merge(Language.UNKNOWN, 1, Integer::sum);
            }
            else if (wordLanguageCounts.size() == 1) {
                Language language = wordLanguageCounts.keySet().iterator().next();
                totalLanguageCounts.merge(language, 1, Integer::sum);
            }
            else {
                List<Map.Entry<Language, Integer>> sortedWordLanguageCounts =
                    new ArrayList<>(wordLanguageCounts.entrySet());
                sortedWordLanguageCounts.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                Map.Entry<Language, Integer> mostFrequentEntry = sortedWordLanguageCounts.get(0);
                Map.Entry<Language, Integer> secondMostFrequentEntry = sortedWordLanguageCounts.get(1);

                if (mostFrequentEntry.getValue() > secondMostFrequentEntry.getValue()) {
                    totalLanguageCounts.merge(mostFrequentEntry.getKey(), 1, Integer::sum);
                }
                else {
                    totalLanguageCounts.merge(Language.UNKNOWN, 1, Integer::sum);
                }
            }
        }

        int unknownLanguageCount = totalLanguageCounts.getOrDefault(Language.UNKNOWN, 0);
        if (unknownLanguageCount < (0.5 * words.size())) {
            totalLanguageCounts.remove(Language.UNKNOWN);
        }
        if (totalLanguageCounts.isEmpty()) {
            return Language.UNKNOWN;
        }
        if (totalLanguageCounts.size() == 1) {
            return totalLanguageCounts.keySet().iterator().next();
        }
        if (totalLanguageCounts.size() == 2 &&
            totalLanguageCounts.containsKey(Language.CHINESE) &&
            totalLanguageCounts.containsKey(Language.JAPANESE)) {
            return Language.JAPANESE;
        }

        List<Map.Entry<Language, Integer>> sortedTotalLanguageCounts = new ArrayList<>(totalLanguageCounts.entrySet());
        sortedTotalLanguageCounts.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map.Entry<Language, Integer> mostFrequentEntry = sortedTotalLanguageCounts.get(0);
        Map.Entry<Language, Integer> secondMostFrequentEntry = sortedTotalLanguageCounts.get(1);

        if (mostFrequentEntry.getValue().equals(secondMostFrequentEntry.getValue())) {
            return Language.UNKNOWN;
        }
        else {
            return mostFrequentEntry.getKey();
        }
    }

    public Set<Language> filterLanguagesByRules(List<String> words) {
        Map<Alphabet, Integer> detectedAlphabets = new HashMap<>();

        for (String word : words) {
            for (Alphabet alphabet : Alphabet.values()) {
                if (alphabet.matches(word)) {
                    detectedAlphabets.merge(alphabet, 1, Integer::sum);
                    break;
                }
            }
        }

        if (detectedAlphabets.isEmpty()) {
            return languages;
        }

        if (detectedAlphabets.size() > 1) {
            Set<Integer> distinctAlphabets = new HashSet<>(detectedAlphabets.values());
            if (distinctAlphabets.size() == 1) {
                return languages;
            }
        }

        Alphabet mostFrequentAlphabet =
            Collections.max(detectedAlphabets.entrySet(), Map.Entry.comparingByValue()).getKey();
        Set<Language> filteredLanguages = new HashSet<>();
        for (Language lang : languages) {
            if (lang.getAlphabets().contains(mostFrequentAlphabet)) {
                filteredLanguages.add(lang);
            }
        }

        Map<Language, Integer> languageCounts = new HashMap<>();
        for (Map.Entry<String, Set<Language>> entry : CHARS_TO_LANGUAGES_MAPPING.entrySet()) {
            String characters = entry.getKey();
            Set<Language> relevantLanguages = new HashSet<>(entry.getValue());
            relevantLanguages.retainAll(filteredLanguages);

            for (String word : words) {
                for (char character : characters.toCharArray()) {
                    if (word.indexOf(character) != -1) {
                        for (Language language : relevantLanguages) {
                            languageCounts.merge(language, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        Set<Language> languagesSubset = new HashSet<>();
        for (Map.Entry<Language, Integer> entry : languageCounts.entrySet()) {
            if (entry.getValue() >= words.size() / 2.0) {
                languagesSubset.add(entry.getKey());
            }
        }

        return languagesSubset.isEmpty() ? filteredLanguages : languagesSubset;
    }

    public Map<Language, Double> computeLanguageProbabilities(TestDataLanguageModel testDataModel,
        Set<Language> filteredLanguages) {
        Map<Language, Double> probabilities = new HashMap<>();
        for (Language language : filteredLanguages) {
            probabilities.put(language, computeSumOfNgramProbabilities(language, testDataModel.getNgrams()));
        }
        probabilities.values().removeIf(v -> v < 0.0f);
        return probabilities;
    }

    public double computeSumOfNgramProbabilities(Language language, Set<Ngram> ngrams) {
        double probabilitiesSum = 0.0d;
        for (Ngram ngram : ngrams) {
            for (Ngram elem : ngram.rangeOfLowerOrderNgrams()) {
                float probability = lookUpNgramProbability(language, elem);
                if (probability > 0) {
                    probabilitiesSum += Math.log(probability);
                    break;
                }
            }
        }
        return probabilitiesSum;
    }

    float lookUpNgramProbability(Language language, Ngram ngram) {
        int ngramLength = ngram.getValue().length();
        Map<Language, Object2FloatMap<String>> languageModels;
        switch (ngramLength) {
            case 5:
                languageModels = fivegramLanguageModels;
                break;
            case 4:
                languageModels = quadrigramLanguageModels;
                break;
            case 3:
                languageModels = trigramLanguageModels;
                break;
            case 2:
                languageModels = bigramLanguageModels;
                break;
            case 1:
                languageModels = unigramLanguageModels;
                break;
            case 0:
                throw new IllegalArgumentException("Zerogram detected");
            default:
                throw new IllegalArgumentException("Unsupported ngram length detected: " + ngramLength);
        }

        Object2FloatMap<String> model = loadLanguageModels(languageModels, language, ngramLength);
        return model.getFloat(ngram.getValue());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LanguageDetector)) {
            return false;
        }
        LanguageDetector that = (LanguageDetector) other;
        return Double.compare(that.minimumRelativeDistance, minimumRelativeDistance) == 0
            && isLowAccuracyModeEnabled == that.isLowAccuracyModeEnabled
            && Objects.equals(languages, that.languages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languages, minimumRelativeDistance, isLowAccuracyModeEnabled);
    }


    public int getNumberOfLoadedLanguages() {
        return numberOfLoadedLanguages;
    }
}
