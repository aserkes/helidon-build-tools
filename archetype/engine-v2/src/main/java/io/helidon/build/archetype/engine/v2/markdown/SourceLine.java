package io.helidon.build.archetype.engine.v2.markdown;

/**
 * A line or part of a line from the input source.
 */
public class SourceLine {

    private final CharSequence content;
    private final SourceSpan sourceSpan;

    public static SourceLine of(CharSequence content, SourceSpan sourceSpan) {
        return new SourceLine(content, sourceSpan);
    }

    private SourceLine(CharSequence content, SourceSpan sourceSpan) {
        if (content == null) {
            throw new NullPointerException("content must not be null");
        }
        this.content = content;
        this.sourceSpan = sourceSpan;
    }

    public CharSequence getContent() {
        return content;
    }

    public SourceSpan getSourceSpan() {
        return sourceSpan;
    }

    public SourceLine substring(int beginIndex, int endIndex) {
        CharSequence newContent = content.subSequence(beginIndex, endIndex);
        SourceSpan newSourceSpan = null;
        if (sourceSpan != null) {
            int columnIndex = sourceSpan.getColumnIndex() + beginIndex;
            int length = endIndex - beginIndex;
            if (length != 0) {
                newSourceSpan = SourceSpan.of(sourceSpan.getLineIndex(), columnIndex, length);
            }
        }
        return SourceLine.of(newContent, newSourceSpan);
    }
}
