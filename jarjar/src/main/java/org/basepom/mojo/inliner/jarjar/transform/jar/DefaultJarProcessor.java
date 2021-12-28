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

import javax.annotation.Nonnull;

import org.basepom.mojo.inliner.jarjar.transform.asm.PackageRemapper;
import org.basepom.mojo.inliner.jarjar.transform.asm.RemappingClassTransformer;
import org.basepom.mojo.inliner.jarjar.transform.config.ClassDelete;
import org.basepom.mojo.inliner.jarjar.transform.config.ClassKeep;
import org.basepom.mojo.inliner.jarjar.transform.config.ClassKeepTransitive;
import org.basepom.mojo.inliner.jarjar.transform.config.ClassRename;
import org.basepom.mojo.inliner.jarjar.transform.config.RulesFileParser;

public class DefaultJarProcessor implements RulesFileParser.Output {

    private final ManifestFilterJarProcessor manifestFilterJarProcessor = new ManifestFilterJarProcessor();
    private final ClassFilterJarProcessor classFilterJarProcessor = new ClassFilterJarProcessor();
    private final ClassClosureJarProcessor classClosureFilterJarProcessor = new ClassClosureJarProcessor();
    private final PackageRemapper packageRemapper = new PackageRemapper();
    private final RemappingClassTransformer remappingClassTransformer = new RemappingClassTransformer(packageRemapper);
    private final ResourceRenamerJarProcessor resourceRenamerJarProcessor = new ResourceRenamerJarProcessor(packageRemapper);

    public JarProcessorChain getJarProcessor() {
        return new JarProcessorChain(new DirectoryFilterJarProcessor(),
                this.manifestFilterJarProcessor,
                this.classFilterJarProcessor,
                this.classClosureFilterJarProcessor,
                new ClassTransformerJarProcessor(this.remappingClassTransformer),
                this.resourceRenamerJarProcessor);
    }

    @Override
    public void addClassDelete(@Nonnull ClassDelete classDelete) {
        classFilterJarProcessor.addClassDelete(classDelete);
    }

    @Override
    public void addClassRename(@Nonnull ClassRename classRename) {
        packageRemapper.addRule(classRename);
    }

    @Override
    public void addClassKeep(@Nonnull ClassKeep classKeep) {
        classFilterJarProcessor.addClassKeep(classKeep);
    }

    @Override
    public void addClassKeepTransitive(@Nonnull ClassKeepTransitive classKeepTransitive) {
        classClosureFilterJarProcessor.addKeep(classKeepTransitive);
    }

    public void setSkipManifest(boolean value) {
        manifestFilterJarProcessor.setEnabled(value);
    }
}
