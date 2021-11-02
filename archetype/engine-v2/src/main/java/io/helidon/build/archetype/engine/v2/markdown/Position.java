package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Position within a {@link Scanner}. This is intentionally kept opaque so as not to expose the internal structure of
 * the Scanner.
 */
class Position {

    final int lineIndex;
    final int index;

    Position(int lineIndex, int index) {
        this.lineIndex = lineIndex;
        this.index = index;
    }
}