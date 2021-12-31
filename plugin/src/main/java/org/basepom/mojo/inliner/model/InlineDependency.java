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
package org.basepom.mojo.inliner.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

public final class InlineDependency {
    private String artifactId = null;
    private String groupId = null;
    private ImmutableList<Relocation> relocations = ImmutableList.of();

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

    public ImmutableList<Relocation> getRelocations() {
        return relocations;
    }

    public void setRelocations(List<Relocation> relocations) {
        this.relocations = ImmutableList.copyOf(relocations);
    }

    public boolean matchDependency(Dependency dependency) {
        return getArtifactId().equals(dependency.getArtifactId()) && getGroupId().equals(dependency.getGroupId());
    }

    public boolean matchArtifact(Artifact artifact) {
        return getArtifactId().equals(artifact.getArtifactId()) && getGroupId().equals(artifact.getGroupId());
    }
}
