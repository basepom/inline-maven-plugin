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
import static org.basepom.inline.mojo.ScopeLimitingFilter.Scope.compile;
import static org.basepom.inline.mojo.ScopeLimitingFilter.Scope.provided;
import static org.basepom.inline.mojo.ScopeLimitingFilter.Scope.runtime;
import static org.basepom.inline.mojo.ScopeLimitingFilter.Scope.system;
import static org.basepom.inline.mojo.ScopeLimitingFilter.Scope.test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;

public final class ScopeLimitingFilter
        implements DependencyFilter {

    public static final String COMPILE_PLUS_RUNTIME = JavaScopes.COMPILE + "+" + JavaScopes.RUNTIME;
    public static final String RUNTIME_PLUS_SYSTEM = JavaScopes.RUNTIME + "+" + JavaScopes.SYSTEM;

    enum Scope {
        compile, runtime, test, provided, system
    }

    private final ImmutableSet<Scope> scopes;

    /**
     * @param scope A maven scope name.
     * @return A filter that matches any dependency that would be visible in the given scope.
     */
    public static ScopeLimitingFilter computeDependencyScope(final String scope) {
        return new ScopeLimitingFilter(computeScopes(scope));
    }

    /**
     * Returns filter that matches any transitive dependency that would be visible in the given scope. This is different from the scope above, as not all scopes
     * are fully transitive (e.g. a test dependency is not transitively visible.
     *
     * @param scope A maven scope name.
     * @return A filter that matches any dependency that would be visible in the given scope.
     */
    public static ScopeLimitingFilter computeTransitiveScope(final String scope) {
        return new ScopeLimitingFilter(computeScopes(computeTransitiveScopes(scope)));
    }

    private ScopeLimitingFilter(Set<Scope> scopes) {
        this.scopes = ImmutableSet.copyOf(scopes);
    }

    private static EnumSet<Scope> computeScopes(final String scope) {
        checkNotNull(scope, "scope is null");

        switch (scope) {
            case JavaScopes.COMPILE:
                return EnumSet.of(compile, system, provided);
            case JavaScopes.PROVIDED:
                return EnumSet.of(provided);
            case JavaScopes.RUNTIME:
                return EnumSet.of(compile, runtime);
            case COMPILE_PLUS_RUNTIME:
                return EnumSet.of(compile, system, provided, runtime);
            case RUNTIME_PLUS_SYSTEM:
                return EnumSet.of(compile, system, runtime);
            case JavaScopes.TEST:
                return EnumSet.allOf(Scope.class);
            default:
                throw new IllegalStateException("Scope '" + scope + "' is unknown!");
        }
    }

    private static String computeTransitiveScopes(final String scope) {
        checkNotNull(scope, "scope is null");

        switch (scope) {
            //  runtime dependencies pull in just runtime deps
            case JavaScopes.RUNTIME:
                // compile and runtime pull in compile and runtime
            case COMPILE_PLUS_RUNTIME:
                // runtime and system pull in runtime and system
            case RUNTIME_PLUS_SYSTEM:
                return scope;
            // test dependencies pull in compile and runtime.
            case JavaScopes.TEST:
                // compile dependencies pull in compile and runtime
            case JavaScopes.COMPILE:
                // provided dependencies pull in just compile and runtime (remove test and provided)
            case JavaScopes.PROVIDED:
                return COMPILE_PLUS_RUNTIME;
            default:
                throw new IllegalStateException("Scope '" + scope + "' is unknown!");
        }
    }

    @Override
    public boolean accept(final DependencyNode node, final List<DependencyNode> parents) {
        checkNotNull(node, "node is null");

        if (node.getDependency() == null) {
            return true;
        }
        final String scope = node.getDependency().getScope();
        switch (scope) {
            case JavaScopes.COMPILE:
                return scopes.contains(compile);
            case JavaScopes.TEST:
                return scopes.contains(test);
            case JavaScopes.RUNTIME:
                return scopes.contains(runtime);
            case JavaScopes.PROVIDED:
                return scopes.contains(provided);
            case JavaScopes.SYSTEM:
                return scopes.contains(system);
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scopes", scopes)
                .toString();
    }
}
