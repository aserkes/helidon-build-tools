/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.common.markdown;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 */
class CoreHtmlNodeRenderer implements NodeRenderer, Visitor {

    private final HtmlNodeRendererContext context;
    private final HtmlWriter html;

    CoreHtmlNodeRenderer(HtmlNodeRendererContext context) {
        this.context = context;
        this.html = context.writer();
    }

    public HtmlNodeRendererContext context() {
        return context;
    }

    @Override
    public Set<Class<? extends Node>> nodeTypes() {
        return new HashSet<>(Arrays.asList(
                Document.class,
                Paragraph.class,
                FencedCodeBlock.class,
                Link.class,
                Emphasis.class,
                StrongEmphasis.class,
                Text.class,
                Code.class
        ));
    }

    @Override
    public void render(Node node) {
        node.accept(this);
    }

    @Override
    public void visit(Document document) {
        visitChildren(document);
    }

    @Override
    public void visit(Paragraph paragraph) {
        html.line();
        html.tag("p", attrs(paragraph, "p"));
        visitChildren(paragraph);
        html.tag("/p");
        html.line();
    }

    @Override
    public void visit(FencedCodeBlock fencedCodeBlock) {
        String literal = fencedCodeBlock.literal();
        Map<String, String> attributes = new LinkedHashMap<>();
        String info = fencedCodeBlock.info();
        if (info != null && !info.isEmpty()) {
            int space = info.indexOf(" ");
            String language;
            if (space == -1) {
                language = info;
            } else {
                language = info.substring(0, space);
            }
            attributes.put("class", "language-" + language);
        }
        renderCodeBlock(literal, fencedCodeBlock, attributes);
    }

    @Override
    public void visit(Link link) {
        Map<String, String> attrs = new LinkedHashMap<>();
        String url = link.destination();

        url = context.urlSanitizer().sanitizeLinkUrl(url);

        url = context.encodeUrl(url);
        attrs.put("href", url);
        if (link.title() != null) {
            attrs.put("title", link.title());
        }
        html.tag("a", attrs(link, "a", attrs));
        visitChildren(link);
        html.tag("/a");
    }

    @Override
    public void visit(Emphasis emphasis) {
        html.tag("em", attrs(emphasis, "em"));
        visitChildren(emphasis);
        html.tag("/em");
    }

    @Override
    public void visit(StrongEmphasis strongEmphasis) {
        html.tag("strong", attrs(strongEmphasis, "strong"));
        visitChildren(strongEmphasis);
        html.tag("/strong");
    }

    @Override
    public void visit(Text text) {
        html.text(text.literal());
    }

    @Override
    public void visit(Code code) {
        html.tag("code", attrs(code, "code"));
        html.text(code.literal());
        html.tag("/code");
    }

    private void visitChildren(Node parent) {
        Node node = parent.firstChild();
        while (node != null) {
            Node next = node.next();
            context.render(node);
            node = next;
        }
    }

    private void renderCodeBlock(String literal, Node node, Map<String, String> attributes) {
        html.line();
        html.tag("pre", attrs(node, "pre"));
        html.tag("code", attrs(node, "code", attributes));
        html.text(literal);
        html.tag("/code");
        html.tag("/pre");
        html.line();
    }

    private Map<String, String> attrs(Node node, String tagName) {
        return attrs(node, tagName, Collections.<String, String>emptyMap());
    }

    private Map<String, String> attrs(Node node, String tagName, Map<String, String> defaultAttributes) {
        return context.extendAttributes(node, tagName, defaultAttributes);
    }
}
