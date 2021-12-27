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
package org.basepom.mojo.inliner.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.common.io.CharStreams;
import org.junit.jupiter.api.Test;

public class TestPomUtil {

    @Test
    public void testRoundTrip() throws Exception {
        String pomContents = CharStreams.toString(new InputStreamReader(TestPomUtil.class.getResourceAsStream("/testPom.xml")));

        PomUtil pomUtil = new PomUtil(pomContents);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(pomContents.length() + 20)) {
            pomUtil.writePom(byteArrayOutputStream);
            String result = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);

            assertEquals(pomContents.replace("\n", ""),
                    result.replace("\n", ""));
        }
    }
}
