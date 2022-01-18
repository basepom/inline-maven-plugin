package org.basepom.jarjar.transform.config;

import static com.google.common.base.Preconditions.checkState;
import static org.basepom.jarjar.ClassNameUtils.toPackage;
import static org.basepom.jarjar.ClassNameUtils.toPath;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public final class Rename {

    private final List<String> sourceElements;
    private final List<String> destinationElements;
    private final boolean hideClasses;

    private Rename(String source, String destination, boolean hideClasses) {
        this.sourceElements = toElements(source);
        this.destinationElements = toElements(destination);
        this.hideClasses = hideClasses;
    }

    private static List<String> toElements(String path) {
        return Splitter.on('/').omitEmptyStrings().splitToList(path);
    }

    private int matchPrefix(List<String> pathElements, List<String> elements) {
        if (elements.size() > pathElements.size()) {
            // source is longer than the whole path...
            return -1;
        }
        for (int i = 0; i < elements.size(); i++) {
            if (!pathElements.get(i).equals(elements.get(i))) {
                return -1;
            }
        }
        return elements.size();
    }

    public static Rename forClassName(String source, String destination, boolean hideClasses) {
        checkState(!source.contains("/"), "package string must not contain '/' (%s)", source);
        checkState(!destination.contains("/"), "package string must not contain '/' (%s)", destination);

        return new Rename(toPath(source), toPath(destination), hideClasses);
    }

    public static Rename forResourcePath(String source, String destination) {
        return new Rename(source, destination, false);
    }

    @CheckForNull
    public String renamePath(@Nonnull String path) {
        List<String> pathElements = toElements(path);
        int index = matchPrefix(pathElements, sourceElements);
        if (index < 0) {
            return null;
        }
        return Joiner.on('/').join(Iterables.concat(destinationElements, pathElements.subList(index, pathElements.size())));
    }

    @CheckForNull
    public String renameClassName(@Nonnull String className) {

        List<String> pathElements = className.indexOf('/') < 0 ? toElements(toPath(className)) : toElements(className);
        int index = matchPrefix(pathElements, sourceElements);
        if (index < 0) {
            return null;
        }
        String name = pathElements.get(pathElements.size() - 1);
        return toPackage(Joiner.on('/').join(Iterables.concat(destinationElements,
                pathElements.subList(index, pathElements.size() - 1),
                ImmutableList.of(hideClasses ? "$" + name : name))));
    }
}
