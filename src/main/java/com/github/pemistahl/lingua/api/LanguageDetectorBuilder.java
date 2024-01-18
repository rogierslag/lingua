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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configures and creates an instance of [LanguageDetector].
 */
public class LanguageDetectorBuilder {


    private final List<Language> languages;
    private double minimumRelativeDistance = 0.0;
    private boolean isEveryLanguageModelPreloaded = false;
    private boolean isLowAccuracyModeEnabled = false;

    private LanguageDetectorBuilder(List<Language> languages) {
        this.languages = languages;
    }

    public LanguageDetector build() {
        return new LanguageDetector(new HashSet<>(languages),
            minimumRelativeDistance,
            isEveryLanguageModelPreloaded,
            isLowAccuracyModeEnabled
        );
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public double getMinimumRelativeDistance() {
        return minimumRelativeDistance;
    }

    public boolean isEveryLanguageModelPreloaded() {
        return isEveryLanguageModelPreloaded;
    }

    public boolean isLowAccuracyModeEnabled() {
        return isLowAccuracyModeEnabled;
    }

    /**
     * Sets the desired value for the minimum relative distance measure.
     * <p>
     * By default, *Lingua* returns the most likely language for a given
     * input text. However, there are certain words that are spelled the
     * same in more than one language. The word *prologue*, for instance,
     * is both a valid English and French word. Lingua would output either
     * English or French which might be wrong in the given context.
     * For cases like that, it is possible to specify a minimum relative
     * distance that the logarithmized and summed up probabilities for
     * each possible language have to satisfy.
     * <p>
     * Be aware that the distance between the language probabilities is
     * dependent on the length of the input text. The longer the input
     * text, the larger the distance between the languages. So if you
     * want to classify very short text phrases, do not set the minimum
     * relative distance too high. Otherwise you will get most results
     * returned as [Language.UNKNOWN] which is the return value for cases
     * where language detection is not reliably possible.
     *
     * @param distance A value between 0.0 and 0.99. Defaults to 0.0.
     * @throws IllegalArgumentException if [distance] is not between 0.0 and 0.99.
     */
    public LanguageDetectorBuilder withMinimumRelativeDistance(double distance) {
        if (distance < 0.0 || distance >= 0.99) {
            throw new IllegalArgumentException("minimum relative distance must lie in between 0.0 and 0.99");
        }
        this.minimumRelativeDistance = distance;
        return this;
    }

    /**
     * Preloads all language models when creating the instance of [LanguageDetector].
     * <p>
     * By default, *Lingua* uses lazy-loading to load only those language models
     * on demand which are considered relevant by the rule-based filter engine.
     * For web services, for instance, it is rather beneficial to preload all language
     * models into memory to avoid unexpected latency while waiting for the
     * service response. This method allows to switch between these two loading modes.
     */
    public LanguageDetectorBuilder withPreloadedLanguageModels() {
        this.isEveryLanguageModelPreloaded = true;
        return this;
    }

    /**
     * Disables the high accuracy mode in order to save memory and increase performance.
     * <p>
     * By default, *Lingua's* high detection accuracy comes at the cost of
     * loading large language models into memory which might not be feasible
     * for systems running low on resources.
     * <p>
     * This method disables the high accuracy mode so that only a small subset
     * of language models is loaded into memory. The downside of this approach
     * is that detection accuracy for short texts consisting of less than 120
     * characters will drop significantly. However, detection accuracy for texts
     * which are longer than 120 characters will remain mostly unaffected.
     */
    public LanguageDetectorBuilder withLowAccuracyMode() {
        this.isLowAccuracyModeEnabled = true;
        return this;
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages.
     */
    public static LanguageDetectorBuilder fromAllLanguages() {
        return new LanguageDetectorBuilder(Language.all());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in still spoken languages.
     */
    public static LanguageDetectorBuilder fromAllSpokenLanguages() {
        return new LanguageDetectorBuilder(Language.allSpokenOnes());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages supporting the Arabic script.
     */
    public static LanguageDetectorBuilder fromAllLanguagesWithArabicScript() {
        return new LanguageDetectorBuilder(Language.allWithArabicScript());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages supporting the Cyrillic script.
     */
    public static LanguageDetectorBuilder fromAllLanguagesWithCyrillicScript() {
        return new LanguageDetectorBuilder(Language.allWithCyrillicScript());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages supporting the Devanagari script.
     */
    public static LanguageDetectorBuilder fromAllLanguagesWithDevanagariScript() {
        return new LanguageDetectorBuilder(Language.allWithDevanagariScript());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages supporting the Latin script.
     */
    public static LanguageDetectorBuilder fromAllLanguagesWithLatinScript() {
        return new LanguageDetectorBuilder(Language.allWithLatinScript());
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with all built-in languages except those specified in [languages].
     *
     * @param languages The languages to exclude from the set of built-in languages.
     * @throws IllegalArgumentException if less than two languages are to be used.
     */
    public static LanguageDetectorBuilder fromAllLanguagesWithout(Language... languages) {
        List<Language> languagesToLoad = new ArrayList<>(Arrays.asList(Language.values()));
        languagesToLoad.remove(Language.UNKNOWN);
        languagesToLoad.removeAll(List.of(languages));
        if (languagesToLoad.size() < 2) {
            throw new IllegalArgumentException("LanguageDetector needs at least 2 languages to choose from");
        }
        return new LanguageDetectorBuilder(languagesToLoad);
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with the specified [languages].
     *
     * @param languages The languages to use.
     * @throws IllegalArgumentException if less than two languages are specified.
     */
    public static LanguageDetectorBuilder fromLanguages(Language... languages) {
        List<Language> languagesToLoad = new ArrayList<>(Arrays.asList(languages));
        languagesToLoad.remove(Language.UNKNOWN);
        if (languagesToLoad.size() < 2) {
            throw new IllegalArgumentException("LanguageDetector needs at least 2 languages to choose from");
        }
        return new LanguageDetectorBuilder(new ArrayList<>(languagesToLoad));
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with the languages specified by the respective ISO 639-1 codes.
     *
     * @param isoCodes The ISO 639-1 codes to use.
     * @throws IllegalArgumentException if less than two iso codes are specified.
     */
    public static LanguageDetectorBuilder fromIsoCodes639_1(IsoCode639_1... isoCodes) {
        List<IsoCode639_1> isoCodesToLoad = new ArrayList<>(Arrays.asList(isoCodes));
        isoCodesToLoad.remove(IsoCode639_1.NONE);
        if (isoCodesToLoad.size() < 2) {
            throw new IllegalArgumentException("LanguageDetector needs at least 2 languages to choose from");
        }
        List<Language> languages = new ArrayList<>();
        for (IsoCode639_1 isoCode : isoCodesToLoad) {
            languages.add(Language.getByIsoCode639_1(isoCode));
        }
        return new LanguageDetectorBuilder(languages);
    }

    /**
     * Creates and returns an instance of LanguageDetectorBuilder
     * with the languages specified by the respective ISO 639-3 codes.
     *
     * @param isoCodes The ISO 639-3 codes to use.
     * @throws IllegalArgumentException if less than two iso codes are specified.
     */
    public static LanguageDetectorBuilder fromIsoCodes639_3(IsoCode639_3... isoCodes) {
        List<IsoCode639_3> isoCodesToLoad = new ArrayList<>(Arrays.asList(isoCodes));
        isoCodesToLoad.remove(IsoCode639_3.NONE);
        if (isoCodesToLoad.size() < 2) {
            throw new IllegalArgumentException("LanguageDetector needs at least 2 languages to choose from");
        }
        List<Language> languages = new ArrayList<>();
        for (IsoCode639_3 isoCode : isoCodesToLoad) {
            languages.add(Language.getByIsoCode639_3(isoCode));
        }
        return new LanguageDetectorBuilder(languages);
    }
}
