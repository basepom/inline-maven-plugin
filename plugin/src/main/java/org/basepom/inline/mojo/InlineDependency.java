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

import com.google.common.base.Joiner;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

public final class InlineDependency {

    private String artifactId = null;
    private String groupId = null;
    private boolean hideClasses = false;
    private boolean transitive = false;

    public InlineDependency() {
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isHideClasses() {
        return hideClasses;
    }

    public InlineDependency setHideClasses(boolean hideClasses) {
        this.hideClasses = hideClasses;
        return this;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public InlineDependency setTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    public boolean matchDependency(Dependency dependency) {
        return getArtifactId().equals(dependency.getArtifact().getArtifactId()) && getGroupId().equals(dependency.getArtifact().getGroupId());
    }

    public boolean matchArtifact(Artifact artifact) {
        return getArtifactId().equals(artifact.getArtifactId()) && getGroupId().equals(artifact.getGroupId());
    }

    @Override
    public String toString() {
        String flags = Joiner.on(", ").skipNulls().join(
                hideClasses ? "hide classes" : null,
                transitive ? "inline transitive" : null);
        return String.format("%s:%s [%s]", groupId, artifactId, flags);
    }
}