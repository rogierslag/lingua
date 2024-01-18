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

package com.github.pemistahl.lingua.internal.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

public abstract class FilesWriter {

    public static void checkInputFilePath(Path inputFilePath) throws IOException {
        if (!inputFilePath.isAbsolute()) {
            throw new IllegalArgumentException("Input file path '" + inputFilePath + "' is not absolute");
        }
        if (!Files.exists(inputFilePath)) {
            throw new NoSuchFileException("Input file '" + inputFilePath + "' does not exist");
        }
        if (!Files.isRegularFile(inputFilePath)) {
            throw new FileNotFoundException(
                "Input file path '" + inputFilePath + "' does not represent a regular file");
        }
    }

    public static void checkOutputDirectoryPath(Path outputDirectoryPath) throws IOException {
        if (!outputDirectoryPath.isAbsolute()) {
            throw new IllegalArgumentException("Output directory path '" + outputDirectoryPath + "' is not absolute");
        }
        if (!Files.exists(outputDirectoryPath)) {
            throw new NotDirectoryException("Output directory '" + outputDirectoryPath + "' does not exist");
        }
        if (!Files.isDirectory(outputDirectoryPath)) {
            throw new NotDirectoryException(
                "Output directory path '" + outputDirectoryPath + "' does not represent a directory");
        }
    }
}
