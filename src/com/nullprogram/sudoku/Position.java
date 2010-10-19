package com.nullprogram.sudoku;

/**
 * A position on a Sudoku board.
 */
public class Position {

    private byte x;
    private byte y;

    /**
     * Create a new position.
     *
     * @param xPos x position
     * @param yPos y position
     */
    public Position(final byte xPos, final byte yPos) {
        x = xPos;
        y = yPos;
    }

    /**
     * Get the x position.
     *
     * @return x position
     */
    public final byte getX() {
        return x;
    }

    /**
     * Get the y position.
     *
     * @return y position
     */
    public final byte getY() {
        return y;
    }
}
