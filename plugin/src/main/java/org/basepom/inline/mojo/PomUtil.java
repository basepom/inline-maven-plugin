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

import static java.lang.String.format;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.StAXStreamBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

final class PomUtil {

    private final Document pomDocument;

    PomUtil(String pomText) throws XMLStreamException, JDOMException {
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        final XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(pomText));
        final StAXStreamBuilder builder = new StAXStreamBuilder();
        this.pomDocument = builder.build(reader);
    }

    void writePom(Writer writer) throws IOException {
        // this is a hack because jdom2 has the annoying habit of removing a newline between
        // a leading comment and the root element.
        if (pomDocument.getContentSize() > 1
                && pomDocument.getContent(0).getCType() == CType.Comment
                && pomDocument.getContent(1).getCType() == CType.Element) {
            this.pomDocument.addContent(1, new Text("\n"));
        }

        XMLOutputter xml = new XMLOutputter();
        xml.setFormat(Format.getPrettyFormat()
                .setLineSeparator(LineSeparator.SYSTEM));
        xml.output(pomDocument, writer);
        writer.flush();
    }

    void removeDependency(Dependency dependency) {
        Namespace pomNs = Namespace.getNamespace("pom", pomDocument.getRootElement().getNamespaceURI());
        XPathFactory xpathFactory = XPathFactory.instance();
        final var artifact = dependency.getArtifact();
        String xpathExpression;

        if (Strings.emptyToNull(artifact.getClassifier()) == null) {
            xpathExpression =
                    format("//pom:dependencies/pom:dependency[pom:artifactId[text() = '%s'] and pom:groupId[text() = '%s'] and not(pom:classifier) ]",
                            artifact.getArtifactId(), artifact.getGroupId());
        } else {
            xpathExpression =
                    format("//pom:dependencies/pom:dependency[pom:artifactId[text() = '%s'] and pom:groupId[text() = '%s'] and pom:classifier[text() = '%s']]",
                            artifact.getArtifactId(), artifact.getGroupId(), artifact.getClassifier());
        }

        XPathExpression<Content> dependencies = xpathFactory.compile(xpathExpression, Filters.content(), null, pomNs);
        List<Content> contents = dependencies.evaluate(pomDocument.getRootElement());
        for (Content content : contents) {
            content.getParentElement().removeContent(content);
        }
    }

    void addDependency(Dependency dependency) {
        Namespace pomNs = Namespace.getNamespace("pom", pomDocument.getRootElement().getNamespaceURI());
        XPathFactory xpathFactory = XPathFactory.instance();

        XPathExpression<Content> dependencies = xpathFactory.compile("/pom:project/pom:dependencies", Filters.content(), null, pomNs);
        Content content = Iterables.getOnlyElement(dependencies.evaluate(pomDocument.getRootElement()));

        Namespace newNs = pomDocument.getRootElement().getNamespace();

        Element dependencyElement = new Element("dependency", newNs);
        dependencyElement.addContent(new Element("groupId", newNs).addContent(dependency.getArtifact().getGroupId()));
        dependencyElement.addContent(new Element("artifactId", newNs).addContent(dependency.getArtifact().getArtifactId()));
        dependencyElement.addContent(new Element("version", newNs).addContent(dependency.getArtifact().getVersion()));

        String scope = dependency.getScope();
        if (Strings.emptyToNull(scope) != null && !JavaScopes.COMPILE.equals(scope)) {
            dependencyElement.addContent(new Element("scope", newNs).addContent(scope));
        }

        if (dependency.isOptional()) {
            dependencyElement.addContent(new Element("optional", newNs).addContent("true"));
        }

        ((Element) content).addContent(dependencyElement);
    }
}
