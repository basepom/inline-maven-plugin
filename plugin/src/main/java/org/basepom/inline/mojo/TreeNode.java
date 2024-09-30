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

import org.basepom.inline.transformer.ClassPathResource;

import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

import com.google.common.base.Strings;

public final class TreeNode {

    private final String name;
    private final ClassPathResource classPathResource;

    private final SortedMap<String, TreeNode> children = new TreeMap<>();
    private boolean written = false;

    public static TreeNode getRootNode() {
        var rootNode = new TreeNode("", null);
        rootNode.written = true;
        return rootNode;
    }

    public TreeNode(String name, ClassPathResource classPathResource) {
        this.name = name;
        this.classPathResource = classPathResource;
    }

    public void addChild(String childName, ClassPathResource classPathResource) {
        children.computeIfAbsent(childName, k -> new TreeNode(childName, classPathResource));
    }

    public TreeNode getChild(String childName) {
        return children.get(childName);
    }

    public String getName() {
        return name;
    }

    public ClassPathResource getClassPathResource() {
        return classPathResource;
    }

    public SortedMap<String, TreeNode> getChildren() {
        return children;
    }

    public void write() {
        written = true;
    }

    public boolean needsWriting() {
        return !written;
    }

    @Override
    public String toString() {
        return render(0);
    }

    private String render(int indent) {
        var result = new StringBuilder(renderNoChildren(indent));
        var indentPadding = Strings.repeat(" ", indent);
        result.append('\n').append(indentPadding).append("children:\n");

        for (TreeNode child : children.values()) {
            result.append(indentPadding);
            result.append(child.render(indent + 2));
            result.append('\n');
        }

        return result.toString();
    }

    private String renderNoChildren(int indent) {
        var result = new StringJoiner(", ", TreeNode.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("written=" + written)
                .add("classPathResource=" + classPathResource);

        return Strings.repeat(" ", indent) + result;
    }
}
