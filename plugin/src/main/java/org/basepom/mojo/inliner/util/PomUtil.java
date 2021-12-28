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
package org.basepom.mojo.inliner.util;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.Document;
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

public class PomUtil {

    private final String pomText;
    private final Document pomDocument;

    public PomUtil(String pomText) throws XMLStreamException, JDOMException {
        this.pomText = pomText;

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        final XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(pomText));
        final StAXStreamBuilder builder = new StAXStreamBuilder();
        this.pomDocument = builder.build(reader);
    }

    public void writePom(OutputStream stream) throws IOException {
        // this is a hack because jdom2 has the annoying habit of removing a newline between
        // a leading comment and the root element.
        if (pomDocument.getContentSize() > 1
                && pomDocument.getContent(0).getCType() == CType.Comment
                && pomDocument.getContent(1).getCType() == CType.Element) {
            this.pomDocument.addContent(1, new Text("\n"));
        }

        XMLOutputter xml = new XMLOutputter();
        xml.setFormat(Format.getRawFormat()
                .setLineSeparator(LineSeparator.SYSTEM));
        xml.output(pomDocument, stream);
    }

    public void removeDependency(InlineDependency inlineDependency) {
        Namespace pomNs = Namespace.getNamespace("pom", pomDocument.getRootElement().getNamespaceURI());
        XPathFactory xpathFactory = XPathFactory.instance();
        String xpathExpression = format("//pom:dependencies/pom:dependency[pom:artifactId[text() = '%s'] and pom:groupId[text() = '%s']]", inlineDependency.artifactId(), inlineDependency.groupId());
        XPathExpression<Content> dependencies = xpathFactory.compile(xpathExpression, Filters.content(), null, pomNs);
        List<Content> contents = dependencies.evaluate(pomDocument.getRootElement());
        for (Content content : contents) {
            content.getParentElement().removeContent(content);
        }
    }
}
