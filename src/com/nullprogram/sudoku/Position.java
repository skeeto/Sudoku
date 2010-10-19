package com.nullprogram.sudoku;

public class Position {

    private byte x;
    private byte y;

    public Position(byte xPos, byte yPos) {
        x = xPos;
        y = yPos;
    }

    public byte getX() {
        return x;
    }

    public byte getY() {
        return y;
    }

    public Position clone() {
        return new Position(x, y);
    }
}
