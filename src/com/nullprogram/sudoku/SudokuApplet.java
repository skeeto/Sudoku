package com.nullprogram.sudoku;

import javax.swing.JApplet;

/**
 * Rather than a frame, put the game inside of an applet.
 */
public class SudokuApplet extends JApplet {

    private static final long serialVersionUID = 2598809732909851801L;

    private Sudoku sudoku;

    /** {@inheritDoc} */
    public final void init() {
        sudoku = new Sudoku();
        add(sudoku);
    }

    /** {@inheritDoc} */
    public final void start() {
        sudoku.createSudoku(Sudoku.EASY);
    }
}
