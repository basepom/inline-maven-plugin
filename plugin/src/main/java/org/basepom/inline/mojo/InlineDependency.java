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
package org.basepom.inline.mojo;

import static com.google.common.base.Preconditions.checkState;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

public final class InlineDependency {

    private ArtifactIdentifier artifactIdentifier = null;

    private boolean transitive = true;
    private boolean optionals = false;

    public void setArtifact(String artifact) {
        this.artifactIdentifier = new ArtifactIdentifier(artifact);
    }

    public ArtifactIdentifier getArtifactIdentifier() {
        checkState(artifactIdentifier != null, "no artifact has been set!");
        return artifactIdentifier;
    }

    public boolean isInlineTransitive() {
        return transitive;
    }

    public InlineDependency setInlineTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    public boolean isInlineOptionals() {
        return optionals;
    }

    public InlineDependency setInlineOptionals(boolean optionals) {
        this.optionals = optionals;
        return this;
    }

    public boolean matchDependency(Dependency dependency) {
        return getArtifactIdentifier().matchDependency(dependency);
    }

    public boolean matchArtifact(Artifact artifact) {
        return getArtifactIdentifier().matchArtifact(artifact);
    }

    @Override
    public String toString() {
        String flags = transitive ? "inline transitive" : "";
        return String.format("%s [%s]", artifactIdentifier, flags);
    }
}
