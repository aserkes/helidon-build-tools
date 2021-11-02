package io.helidon.build.archetype.engine.v2.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of lines ({@link SourceLine}) from the input source.
 */
public class SourceLines {

    private final List<SourceLine> lines = new ArrayList<>();

    public static SourceLines empty() {
        return new SourceLines();
    }

    public static SourceLines of(SourceLine sourceLine) {
        SourceLines sourceLines = new SourceLines();
        sourceLines.addLine(sourceLine);
        return sourceLines;
    }

    public static SourceLines of(List<SourceLine> sourceLines) {
        SourceLines result = new SourceLines();
        result.lines.addAll(sourceLines);
        return result;
    }

    public void addLine(SourceLine sourceLine) {
        lines.add(sourceLine);
    }

    public List<SourceLine> getLines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String getContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i != 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i).getContent());
        }
        return sb.toString();
    }
}
