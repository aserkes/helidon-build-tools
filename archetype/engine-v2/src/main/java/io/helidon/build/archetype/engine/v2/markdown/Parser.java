package io.helidon.build.archetype.engine.v2.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses input text to a tree of nodes.
 */
public class Parser {

    private final List<BlockParserFactory> blockParserFactories;
    private final List<DelimiterProcessor> delimiterProcessors;
    private final InlineParserFactory inlineParserFactory;
    private final List<PostProcessor> postProcessors;
    private final IncludeSourceSpans includeSourceSpans;
    
    private Parser(Builder builder) {
        this.blockParserFactories = DocumentParser.calculateBlockParserFactories(builder.blockParserFactories, builder.enabledBlockTypes);
        this.inlineParserFactory = builder.getInlineParserFactory();
        this.postProcessors = builder.postProcessors;
        this.delimiterProcessors = builder.delimiterProcessors;
        this.includeSourceSpans = builder.includeSourceSpans;

        // Try to construct an inline parser. Invalid configuration might result in an exception, which we want to
        // detect as soon as possible.
        this.inlineParserFactory.create(new InlineParserContextImpl(delimiterProcessors, new LinkReferenceDefinitions()));
    }

    /**
     * Parse the specified input text into a tree of nodes.
     *
     * @param input the text to parse - must not be null
     * @return the root node
     */
    public Node parse(String input) {
        if (input == null) {
            throw new NullPointerException("input must not be null");
        }
        DocumentParser documentParser = createDocumentParser();
        Node document = documentParser.parse(input);
        return postProcess(document);
    }

    private DocumentParser createDocumentParser() {
        return new DocumentParser(blockParserFactories, inlineParserFactory, delimiterProcessors, includeSourceSpans);
    }

    private Node postProcess(Node document) {
        for (PostProcessor postProcessor : postProcessors) {
            document = postProcessor.process(document);
        }
        return document;
    }

    /**
     * Create a new builder for configuring a {@link Parser}.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring a {@link Parser}.
     */
    public static class Builder {
        private final List<BlockParserFactory> blockParserFactories = new ArrayList<>();
        private final List<DelimiterProcessor> delimiterProcessors = new ArrayList<>();
        private final List<PostProcessor> postProcessors = new ArrayList<>();
        private Set<Class<? extends Block>> enabledBlockTypes = DocumentParser.getDefaultBlockParserTypes();
        private InlineParserFactory inlineParserFactory;
        private IncludeSourceSpans includeSourceSpans = IncludeSourceSpans.NONE;

        /**
         * @return the configured {@link Parser}
         */
        public Parser build() {
            return new Parser(this);
        }

        /**
         * @param extensions extensions to use on this parser
         * @return {@code this}
         */
        public Parser.Builder extensions(Iterable<? extends Extension> extensions) {
            if (extensions == null) {
                throw new NullPointerException("extensions must not be null");
            }
            for (Extension extension : extensions) {
                if (extension instanceof Parser.ParserExtension) {
                    Parser.ParserExtension parserExtension = (Parser.ParserExtension) extension;
                    parserExtension.extend(this);
                }
            }
            return this;
        }

        /**
         * Describe the list of markdown features the parser will recognize and parse.
         */
        public Parser.Builder enabledBlockTypes(Set<Class<? extends Block>> enabledBlockTypes) {
            if (enabledBlockTypes == null) {
                throw new NullPointerException("enabledBlockTypes must not be null");
            }
            this.enabledBlockTypes = enabledBlockTypes;
            return this;
        }

        /**
         * Whether to calculate {@link SourceSpan} for {@link Node}.
         * <p>
         * By default, source spans are disabled.
         *
         * @param includeSourceSpans which kind of source spans should be included
         * @return {@code this}
         * @since 0.16.0
         */
        public Parser.Builder includeSourceSpans(IncludeSourceSpans includeSourceSpans) {
            this.includeSourceSpans = includeSourceSpans;
            return this;
        }

        /**
         * Adds a custom delimiter processor.
         * <p>
         * Note that multiple delimiter processors with the same characters can be added, as long as they have a
         * different minimum length. In that case, the processor with the shortest matching length is used. Adding more
         * than one delimiter processor with the same character and minimum length is invalid.
         *
         * @param delimiterProcessor a delimiter processor implementation
         * @return {@code this}
         */
        public Parser.Builder customDelimiterProcessor(
                DelimiterProcessor delimiterProcessor) {
            if (delimiterProcessor == null) {
                throw new NullPointerException("delimiterProcessor must not be null");
            }
            delimiterProcessors.add(delimiterProcessor);
            return this;
        }

        public Parser.Builder postProcessor(PostProcessor postProcessor) {
            if (postProcessor == null) {
                throw new NullPointerException("postProcessor must not be null");
            }
            postProcessors.add(postProcessor);
            return this;
        }

        private InlineParserFactory getInlineParserFactory() {
            return new InlineParserFactory() {
                @Override
                public InlineParser create(InlineParserContext inlineParserContext) {
                    return new InlineParser(inlineParserContext);
                }
            };
        }
    }

    /**
     * Extension for {@link Parser}.
     */
    public interface ParserExtension extends Extension {
        void extend(Builder parserBuilder);
    }
}
