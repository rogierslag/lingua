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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.internal.io.FilesWriter;

public class TestDataFilesWriter extends FilesWriter {

    private static final Pattern MULTIPLE_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("\\p{P}");
    private static final Pattern NUMBERS = Pattern.compile("\\p{N}");

    public static void createAndWriteTestDataFiles(
        Path inputFilePath,
        Charset inputFileCharset,
        Path outputDirectoryPath,
        Language language,
        String charClass,
        int maximumLines) throws IOException {

        checkInputFilePath(inputFilePath);
        checkOutputDirectoryPath(outputDirectoryPath);

        createAndWriteSentencesFile(inputFilePath, inputFileCharset, outputDirectoryPath, language, maximumLines);
        List<String> singleWords =
            createAndWriteSingleWordsFile(inputFilePath, inputFileCharset, outputDirectoryPath, language, charClass,
                maximumLines);
        createAndWriteWordPairsFile(singleWords, outputDirectoryPath, language, maximumLines);
    }

    private static void createAndWriteSentencesFile(
        Path inputFilePath,
        Charset inputFileCharset,
        Path outputDirectoryPath,
        Language language,
        int maximumLines) throws IOException {

        String fileName = language.getIsoCode639_1() + ".txt";
        Path sentencesDirectoryPath = outputDirectoryPath.resolve("sentences");
        Path sentencesFilePath = sentencesDirectoryPath.resolve(fileName);
        int lineCounter = 0;

        Files.createDirectories(sentencesDirectoryPath);
        Files.deleteIfExists(sentencesFilePath);

        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, inputFileCharset);
            BufferedWriter writer = Files.newBufferedWriter(sentencesFilePath)) {
            String line;
            while ((line = reader.readLine()) != null && lineCounter < maximumLines) {
                String processedLine = line.replaceAll(MULTIPLE_WHITESPACE.pattern(), " ").replace("\"", "");
                writer.write(processedLine);
                writer.newLine();
                lineCounter++;
            }
        }
    }

    private static List<String> createAndWriteSingleWordsFile(
        Path inputFilePath,
        Charset inputFileCharset,
        Path outputDirectoryPath,
        Language language,
        String charClass,
        int maximumLines) throws IOException {

        String fileName = language.getIsoCode639_1() + ".txt";
        Path singleWordsDirectoryPath = outputDirectoryPath.resolve("single-words");
        Path singleWordsFilePath = singleWordsDirectoryPath.resolve(fileName);
        Pattern wordRegex = Pattern.compile("[" + charClass + "]{5,}");

        List<String> words = new ArrayList<>();
        int lineCounter = 0;

        Files.createDirectories(singleWordsDirectoryPath);
        Files.deleteIfExists(singleWordsFilePath);

        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, inputFileCharset);
            BufferedWriter writer = Files.newBufferedWriter(singleWordsFilePath)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] singleWords = line
                    .replaceAll(PUNCTUATION.pattern(), "")
                    .replaceAll(NUMBERS.pattern(), "")
                    .replaceAll(MULTIPLE_WHITESPACE.pattern(), " ")
                    .replace("\"", "")
                    .split(" ");

                for (String word : singleWords) {
                    word = word.trim().toLowerCase();
                    if (wordRegex.matcher(word).matches()) {
                        words.add(word);
                        if (lineCounter < maximumLines) {
                            writer.write(word);
                            writer.newLine();
                            lineCounter++;
                        }
                        else {
                            break;
                        }
                    }
                }
            }
        }

        return words;
    }

    private static void createAndWriteWordPairsFile(
        List<String> words,
        Path outputDirectoryPath,
        Language language,
        int maximumLines) throws IOException {

        String fileName = language.getIsoCode639_1() + ".txt";
        Path wordPairsDirectoryPath = outputDirectoryPath.resolve("word-pairs");
        Path wordPairsFilePath = wordPairsDirectoryPath.resolve(fileName);
        Set<String> wordPairs = new HashSet<>();
        int lineCounter = 0;

        Files.createDirectories(wordPairsDirectoryPath);
        Files.deleteIfExists(wordPairsFilePath);

        for (int i = 0; i < words.size() - 1; i += 2) {
            if (words.size() > i + 1) {
                String wordPair = words.get(i) + " " + words.get(i + 1);
                wordPairs.add(wordPair);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(wordPairsFilePath)) {
            for (String wordPair : wordPairs) {
                if (lineCounter < maximumLines) {
                    writer.write(wordPair);
                    writer.newLine();
                    lineCounter++;
                }
                else {
                    break;
                }
            }
        }
    }
}
