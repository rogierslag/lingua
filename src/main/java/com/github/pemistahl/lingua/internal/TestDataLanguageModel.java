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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TestDataLanguageModel {
    private final Set<Ngram> ngrams;

    public TestDataLanguageModel(Set<Ngram> ngrams) {
        this.ngrams = ngrams;
    }

    public static TestDataLanguageModel fromText(String text, int ngramLength) {
        if (ngramLength < 1 || ngramLength > 5) {
            throw new IllegalArgumentException("Ngram length " + ngramLength + " is not in range 1..5");
        }

        Set<Ngram> ngrams = new HashSet<>();
        Pattern letterRegex = Pattern.compile("\\p{L}+");
        for (int i = 0; i <= text.length() - ngramLength; i++) {
            String textSlice = text.substring(i, i + ngramLength);
            if (letterRegex.matcher(textSlice).matches()) {
                Ngram ngram = new Ngram(textSlice);
                ngrams.add(ngram);
            }
        }
        return new TestDataLanguageModel(ngrams);
    }

    public Set<Ngram> getNgrams() {
        return ngrams;
    }
}
