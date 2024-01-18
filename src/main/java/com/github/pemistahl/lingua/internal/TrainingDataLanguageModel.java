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

package com.github.pemistahl.lingua.internal;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.pemistahl.lingua.api.Language;

public class TrainingDataLanguageModel {

    private final Language language;
    private final Map<Ngram, Integer> absoluteFrequencies;
    private final Map<Ngram, Fraction> relativeFrequencies;

    public TrainingDataLanguageModel(Language language, Map<Ngram, Integer> absoluteFrequencies,
        Map<Ngram, Fraction> relativeFrequencies) {
        this.language = language;
        this.absoluteFrequencies = absoluteFrequencies;
        this.relativeFrequencies = relativeFrequencies;
    }

    public String toJson() {
        // FIXME
        // Gson gson = new Gson();
        // return gson.toJson(new JsonLanguageModel(language, convertNgramsForJson()));
        return "FIXME";
    }

    private Map<Fraction, String> convertNgramsForJson() {
        Map<Fraction, StringBuilder> ngramsForJson = new HashMap<>();
        for (Map.Entry<Ngram, Fraction> entry : relativeFrequencies.entrySet()) {
            Ngram ngram = entry.getKey();
            Fraction fraction = entry.getValue();

            StringBuilder ngramsBuilder = ngramsForJson.computeIfAbsent(fraction, k -> new StringBuilder());
            if (ngramsBuilder.length() > 0) {
                ngramsBuilder.append(" ");
            }
            ngramsBuilder.append(ngram.toString());
        }

        Map<Fraction, String> ngramsJson = new HashMap<>();
        for (Map.Entry<Fraction, StringBuilder> entry : ngramsForJson.entrySet()) {
            ngramsJson.put(entry.getKey(), entry.getValue().toString());
        }

        return ngramsJson;
    }

    public static TrainingDataLanguageModel fromText(
        Iterable<String> text,
        Language language,
        int ngramLength,
        String charClass,
        Map<Ngram, Integer> lowerNgramAbsoluteFrequencies) {

        if (ngramLength < 1 || ngramLength > 5) {
            throw new IllegalArgumentException("Ngram length " + ngramLength + " is not in range 1..5");
        }

        Map<Ngram, Integer> absoluteFrequencies = computeAbsoluteFrequencies(text, ngramLength, charClass);
        Map<Ngram, Fraction> relativeFrequencies =
            computeRelativeFrequencies(ngramLength, absoluteFrequencies, lowerNgramAbsoluteFrequencies);

        return new TrainingDataLanguageModel(language, absoluteFrequencies, relativeFrequencies);
    }

    private static Map<Ngram, Integer> computeAbsoluteFrequencies(Iterable<String> text, int ngramLength, String charClass) {
        Map<Ngram, Integer> absoluteFrequencies = new HashMap<>();
        Pattern regex = Pattern.compile("[" + charClass + "]+");

        for (String line : text) {
            String lowerCasedLine = line.toLowerCase();
            for (int i = 0; i <= lowerCasedLine.length() - ngramLength; i++) {
                String textSlice = lowerCasedLine.substring(i, i + ngramLength);
                if (regex.matcher(textSlice).matches()) {
                    Ngram ngram = new Ngram(textSlice);
                    absoluteFrequencies.put(ngram, absoluteFrequencies.getOrDefault(ngram, 0) + 1);
                }
            }
        }

        return absoluteFrequencies;
    }

    private static Map<Ngram, Fraction> computeRelativeFrequencies(int ngramLength, Map<Ngram, Integer> absoluteFrequencies, Map<Ngram, Integer> lowerNgramAbsoluteFrequencies) {
        Map<Ngram, Fraction> ngramProbabilities = new HashMap<>();
        int totalNgramFrequency = absoluteFrequencies.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<Ngram, Integer> entry : absoluteFrequencies.entrySet()) {
            Ngram ngram = entry.getKey();
            int frequency = entry.getValue();

            int denominator;
            if (ngramLength == 1 || lowerNgramAbsoluteFrequencies.isEmpty()) {
                denominator = totalNgramFrequency;
            } else {
                String lowerNgramValue = ngram.getValue().substring(0, ngramLength - 1);
                Ngram lowerOrderNgram = new Ngram(lowerNgramValue);
                denominator = lowerNgramAbsoluteFrequencies.getOrDefault(lowerOrderNgram, 0);
            }

            Fraction fraction = new Fraction(frequency, denominator);
            ngramProbabilities.put(ngram, fraction);
        }

        return ngramProbabilities;
    }

    public Map<Ngram, Integer> getAbsoluteFrequencies() {
        return absoluteFrequencies;
    }
    public Language getLanguage() {
        return language;
    }

    public Map<Ngram, Fraction> getRelativeFrequencies() {
        return relativeFrequencies;
    }
}
