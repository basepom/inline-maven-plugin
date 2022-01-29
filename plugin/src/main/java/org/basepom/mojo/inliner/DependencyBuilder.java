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
package org.basepom.mojo.inliner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Builds a map of dependencies required by a specific project or another dependency.
 */
public final class DependencyBuilder {

    private static final PluginLog LOG = new PluginLog(DependencyBuilder.class);

    private final MavenProject rootProject;
    private final MavenSession mavenSession;
    private final ProjectBuilder projectBuilder;
    private final ProjectDependenciesResolver projectDependenciesResolver;
    private final List<MavenProject> reactorProjects;

    public DependencyBuilder(MavenProject rootProject, MavenSession mavenSession, ProjectBuilder projectBuilder,
            ProjectDependenciesResolver projectDependenciesResolver, List<MavenProject> reactorProjects) {
        this.rootProject = rootProject;
        this.mavenSession = mavenSession;
        this.projectBuilder = projectBuilder;
        this.projectDependenciesResolver = projectDependenciesResolver;
        this.reactorProjects = ImmutableList.copyOf(reactorProjects);
    }

    /**
     * Create a map of dependencies for a given dependency node (representing an element on the dependency tree).
     *
     * @param dependency     The dependency node to use.
     * @param projectScopeFilter A scope limiting filter to mask out dependencies out of scope.
     * @return A map of dependencies for this given dependency node.
     * @throws DependencyResolutionException Dependency resolution failed.
     * @throws ProjectBuildingException      Maven project could not be built.
     */
    public ImmutableList<Dependency> mapDependency(final Dependency dependency,
            final DependencyFilter projectScopeFilter)
            throws DependencyResolutionException, ProjectBuildingException {
        checkNotNull(dependency, "dependency is null");

        // build the project
        final ProjectBuildingResult result = projectBuilder.build(convertFromAetherDependency(dependency), false, createProjectBuildingRequest());

        // now resolve the project representing the dependency.
        final MavenProject project = result.getProject();
        return mapProject(project, projectScopeFilter);
    }

    /**
     * Create a map of names to dependencies for a given project.
     *
     * @param project     The current maven project.
     * @param scopeFilter A scope limiting filter to mask out dependencies out of scope.
     * @return A map of dependencies for this given dependency node.
     * @throws DependencyResolutionException Dependency resolution failed.
     */
    public ImmutableList<Dependency> mapProject(final MavenProject project,
            final DependencyFilter scopeFilter)
            throws DependencyResolutionException {
        checkNotNull(project, "project is null");

        final DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
        request.setRepositorySession(createProjectBuildingRequest().getRepositorySession());
        request.setMavenProject(project);
        request.setResolutionFilter(scopeFilter);

        DependencyResolutionResult result;

        try {
            result = projectDependenciesResolver.resolve(request);
        } catch (DependencyResolutionException e) {
            result = e.getResult();
            // try to resolve using the reactor projects
            final ImmutableSet<ProjectKey> reactorProjects = this.reactorProjects.stream()
                    .map(ProjectKey::fromProject).collect(toImmutableSet());

            // resolve all dependencies that are matched by the reactor.
            final ImmutableSet<Dependency> reactorDependencies = result.getUnresolvedDependencies().stream()
                    .filter(d -> reactorProjects.contains(ProjectKey.fromDependency(d)))
                    .collect(toImmutableSet());

            result.getUnresolvedDependencies().removeAll(reactorDependencies);
            result.getResolvedDependencies().addAll(reactorDependencies);

            // remove all unresolved system dependencies
            final ImmutableSet<Dependency> systemDependencies = result.getUnresolvedDependencies().stream()
                    .filter(d -> JavaScopes.SYSTEM.equals(d.getScope()))
                    .collect(toImmutableSet());

            result.getUnresolvedDependencies().removeAll(systemDependencies);
            result.getResolvedDependencies().addAll(systemDependencies);

            // remove all unresolved optional dependencies
            final ImmutableSet<Dependency> optionalDependencies = result.getUnresolvedDependencies().stream()
                    .filter(Dependency::isOptional)
                    .collect(toImmutableSet());

            result.getUnresolvedDependencies().removeAll(optionalDependencies);
            result.getResolvedDependencies().addAll(optionalDependencies);

            if (!result.getUnresolvedDependencies().isEmpty()) {
                final Throwable t = Throwables.getRootCause(e);
                RemoteRepository repository = null;

                if (t instanceof NoRepositoryLayoutException) {
                    repository = ((NoRepositoryLayoutException) t).getRepository();
                } else if (t instanceof ArtifactTransferException) {
                    repository = ((ArtifactTransferException) t).getRepository();
                }

                if (repository != null && "legacy".equals(repository.getContentType())) {
                    LOG.warn("Could not access a legacy repository for artifacts:  %s; Reason: %s", result.getUnresolvedDependencies(), t.getMessage());
                } else {
                    throw e;
                }
            }
        }

        return ImmutableList.copyOf(result.getResolvedDependencies());
    }

    static org.apache.maven.artifact.Artifact convertFromAetherDependency(final Dependency dependency) {
        final var mavenArtifact = RepositoryUtils.toArtifact(convertToPomArtifact(dependency.getArtifact()));
        mavenArtifact.setScope(dependency.getScope());
        mavenArtifact.setOptional(dependency.isOptional());

        return mavenArtifact;
    }

    private ProjectBuildingRequest createProjectBuildingRequest() {
        DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(mavenSession.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(rootProject.getRemoteArtifactRepositories());
        return buildingRequest;
    }


    static Artifact convertToPomArtifact(final Artifact artifact) {
        // pom artifact has no classifier. If this is already a pom artifact, don't touch it.
        if (artifact.getClassifier().isEmpty() && "pom".equals(artifact.getExtension())) {
            return artifact;
        }

        // create a POM artifact.
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
    }

    private static final class ProjectKey {

        private final String groupId;
        private final String artifactId;
        private final String version;

        public static ProjectKey fromProject(final MavenProject project) {
            checkNotNull(project, "project; is null");
            return new ProjectKey(project.getGroupId(), project.getArtifactId(), project.getVersion());
        }

        public static ProjectKey fromDependency(final Dependency dependency) {
            checkNotNull(dependency, "artifact; is null");
            return new ProjectKey(dependency.getArtifact().getGroupId(),
                    dependency.getArtifact().getArtifactId(),
                    dependency.getArtifact().getVersion());
        }

        private ProjectKey(final String groupId, final String artifactId, final String version) {
            this.groupId = checkNotNull(groupId, "groupId is null");
            this.artifactId = checkNotNull(artifactId, "artifactId is null");
            this.version = checkNotNull(version, "version is null");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProjectKey that = (ProjectKey) o;
            return groupId.equals(that.groupId) &&
                    artifactId.equals(that.artifactId) &&
                    version.equals(that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, version);
        }
    }
}
