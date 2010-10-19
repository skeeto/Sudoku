package com.nullprogram.sudoku;

import javax.swing.JApplet;

/**
 * Rather than a frame, put the game inside of an applet.
 */
public class SudokuApplet extends JApplet {

    private static final long serialVersionUID = 2598809732909851801L;

    /** {@inheritDoc} */
    public final void init() {
        Sudoku sudoku = new Sudoku();
        add(sudoku);
        sudoku.createSudoku();
        sudoku.requestFocusInWindow();
    }
}
