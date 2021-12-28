/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.basepom.mojo.inliner.jarjar.transform.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;

public class RulesFileParser {

    public interface Output {

        void addClassDelete(@Nonnull ClassDelete classDelete);

        void addClassRename(@Nonnull ClassRename classRename);

        void addClassKeep(@Nonnull ClassKeep classKeep);

        void addClassKeepTransitive(@Nonnull ClassKeepTransitive classKeepTransitive);
    }

    private RulesFileParser() {
    }

    public static void parse(@Nonnull Output output, @Nonnull File file) throws IOException {
        parse(output, new FileReader(file, StandardCharsets.UTF_8));
    }

    public static void parse(@Nonnull Output output, @Nonnull String value) throws IOException {
        parse(output, new StringReader(value));
    }

    @Nonnull
    private static List<String> split(@Nonnull String line) {
        StringTokenizer tok = new StringTokenizer(line);
        List<String> out = new ArrayList<>();
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.startsWith("#")) {
                break;
            }
            out.add(token);
        }
        return out;
    }

    private static void parse(@Nonnull Output output, @Nonnull @WillClose Reader r) throws IOException {
        try (r) {
            BufferedReader br = new BufferedReader(r);
            int lineNumber = 1;
            String line;
            while ((line = br.readLine()) != null) {
                List<String> words = split(line);
                if (words.isEmpty()) {
                    continue;
                }
                if (words.size() < 2) {
                    throw error(lineNumber, words, "not enough words on line.");
                }
                String type = words.get(0);

                switch (type) {
                    case "rule":
                        if (words.size() < 3) {
                            throw error(lineNumber, words, "'rule' requires 2 arguments.");
                        }
                        output.addClassRename(new ClassRename(words.get(1), words.get(2)));
                        break;
                    case "zap":
                        output.addClassDelete(new ClassDelete(words.get(1)));
                        break;
                    case "keep":
                        output.addClassKeepTransitive(new ClassKeepTransitive(words.get(1)));
                        break;
                    default:
                        throw error(lineNumber, words, "Unrecognized keyword " + type);
                }
                lineNumber++;
            }
        }
    }

    @Nonnull
    private static IllegalArgumentException error(@Nonnegative int lineNumber, @Nonnull List<String> words, @Nonnull String reason) {
        throw new IllegalArgumentException("Error on line " + lineNumber + ": " + words + ": " + reason);
    }
}
