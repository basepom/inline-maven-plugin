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
package org.basepom.mojo.inliner.jarjar.transform.jar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

import org.basepom.mojo.inliner.jarjar.transform.Transformable;

public class JarProcessorChain implements JarProcessor {

    private final List<JarProcessor> processors;

    public JarProcessorChain(@Nonnull Iterable<? extends JarProcessor> processors) {
        this.processors = StreamSupport.stream(processors.spliterator(), false).collect(Collectors.toUnmodifiableList());
    }

    public JarProcessorChain(@Nonnull JarProcessor... processors) {
        this.processors = Arrays.stream(processors).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Result scan(Transformable struct) throws IOException {
        for (JarProcessor processor : processors) {
            if (processor.scan(struct) == Result.DISCARD) {
                return Result.DISCARD;
            }
        }
        return Result.KEEP;
    }

    @Override
    public Result process(Transformable struct) throws IOException {
        for (JarProcessor processor : processors) {
            if (processor.process(struct) == Result.DISCARD) {
                return Result.DISCARD;
            }
        }
        return Result.KEEP;
    }
}
