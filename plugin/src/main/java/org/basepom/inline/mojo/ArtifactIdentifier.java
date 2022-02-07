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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Splitter;
import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public final class ArtifactIdentifier {

    private final String artifactId;
    private final String groupId;

    public ArtifactIdentifier(String artifact) {
        checkNotNull(artifact, "artifact is null");
        List<String> elements = Splitter.on(':').trimResults().splitToList(artifact);
        checkState(elements.size() == 2, "artifact format is <groupId>:<artifactId> (got %s)", artifact);
        this.groupId = elements.get(0);
        this.artifactId = elements.get(1);
    }

    public ArtifactIdentifier(Dependency dependency) {
        checkNotNull(dependency, "dependency is null");

        this.groupId = dependency.getArtifact().getGroupId();
        this.artifactId = dependency.getArtifact().getArtifactId();
    }

    public ArtifactIdentifier(Artifact artifact) {
        this.groupId = artifact.getGroupId();
        this.artifactId = artifact.getArtifactId();
    }

    public ArtifactIdentifier(DependencyNode dependencyNode) {
        checkNotNull(dependencyNode, "dependencyNode is null");

        this.groupId = dependencyNode.getArtifact().getGroupId();
        this.artifactId = dependencyNode.getArtifact().getArtifactId();
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean matchDependency(Dependency dependency) {
        return Optional.ofNullable(dependency)
                .map(Dependency::getArtifact)
                .map(a -> match(a.getGroupId(), a.getArtifactId()))
                .orElse(false);
    }

    public boolean matchArtifact(Artifact artifact) {
        return Optional.of(artifact)
                .map(a -> match(a.getGroupId(), a.getArtifactId()))
                .orElse(false);
    }

    private boolean match(String groupId, String artifactId) {
        return (getArtifactId().equals("*") || getArtifactId().equals(artifactId))
                && (getGroupId().equals("*") || getGroupId().equals(groupId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactIdentifier that = (ArtifactIdentifier) o;
        return artifactId.equals(that.artifactId) && groupId.equals(that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, groupId);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", groupId, artifactId);
    }
}
