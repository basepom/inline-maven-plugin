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
package org.basepom.jarjar.strings;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.annotation.Nonnull;

import org.basepom.jarjar.IoUtil;
import org.basepom.jarjar.classpath.ClassPath;
import org.basepom.jarjar.classpath.ClassPathArchive;
import org.basepom.jarjar.classpath.ClassPathResource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringDumper {
    private static final Logger LOG = LoggerFactory.getLogger(StringDumper.class);

    public void run(Appendable out, ClassPath classPath) throws IOException {
        StringReader stringReader = new DumpStringReader(out);
        for (ClassPathArchive classPathArchive : classPath) {
            for (ClassPathResource classPathResource : classPathArchive) {
                try (InputStream in = classPathResource.openStream()) {
                    new ClassReader(in).accept(stringReader, 0);
                } catch (Exception e) {
                    LOG.error(format("Error reading %s", classPathResource), e);
                }
                IoUtil.flush(out);
            }
        }
    }

    private static class DumpStringReader extends StringReader {

        private final Appendable out;
        private String className;

        DumpStringReader(@Nonnull Appendable out) {
            this.out = out;
        }

        @Override
        public void visitString(@Nonnull String className, @Nonnull String value, int line) {
            if (value.length() > 0) {
                try {
                    if (!className.equals(this.className)) {
                        this.className = className;
                        out.append(className.replace('/', '.'));
                    }
                    out.append("\t");
                    if (line >= 0) {
                        out.append(String.valueOf(line)).append(": ");
                    }
                    out.append(escapeStringLiteral(value));
                    out.append("\n");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Nonnull
    private static String escapeStringLiteral(@Nonnull String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0, size = value.length(); i < size; i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(ch);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
