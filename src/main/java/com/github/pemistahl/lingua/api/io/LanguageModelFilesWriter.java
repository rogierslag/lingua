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

package com.github.pemistahl.lingua.api.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.internal.Ngram;
import com.github.pemistahl.lingua.internal.TrainingDataLanguageModel;
import com.github.pemistahl.lingua.internal.io.FilesWriter;

public class LanguageModelFilesWriter extends FilesWriter {

    public static void createAndWriteLanguageModelFiles(
        Path inputFilePath,
        Charset inputFileCharset,
        Path outputDirectoryPath,
        Language language,
        String charClass) throws IOException {

        checkInputFilePath(inputFilePath);
        checkOutputDirectoryPath(outputDirectoryPath);

        TrainingDataLanguageModel unigramModel =
            createLanguageModel(inputFilePath, inputFileCharset, language, 1, charClass, Collections.emptyMap());
        TrainingDataLanguageModel bigramModel =
            createLanguageModel(inputFilePath, inputFileCharset, language, 2, charClass,
                unigramModel.getAbsoluteFrequencies());
        // ... similarly for trigram, quadrigram, and fivegram models

        writeLanguageModel(unigramModel, outputDirectoryPath, "unigrams.json");
        writeLanguageModel(bigramModel, outputDirectoryPath, "bigrams.json");
        // ... similarly for trigram, quadrigram, and fivegram models
    }

    private static TrainingDataLanguageModel createLanguageModel(
        Path inputFilePath,
        Charset inputFileCharset,
        Language language,
        int ngramLength,
        String charClass,
        Map<Ngram, Integer> lowerNgramAbsoluteFrequencies) throws IOException {

        TrainingDataLanguageModel model;
        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, inputFileCharset)) {
            model = TrainingDataLanguageModel.fromText(
                reader.lines()::iterator,
                language,
                ngramLength,
                charClass,
                lowerNgramAbsoluteFrequencies);
        }
        return model;
    }

    private static void writeLanguageModel(
        TrainingDataLanguageModel model,
        Path outputDirectoryPath,
        String fileName) throws IOException {

        Path modelFilePath = outputDirectoryPath.resolve(fileName);

        Files.deleteIfExists(modelFilePath);

        try (BufferedWriter writer = Files.newBufferedWriter(modelFilePath)) {
            writer.write(model.toJson());
        }
    }
}
