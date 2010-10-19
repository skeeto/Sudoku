package com.nullprogram.sudoku;

import java.util.Stack;
import java.util.Random;
import java.util.Collections;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JComponent;

/**
 * A Sudoku board capable of generating puzzles and interacting.
 */
public class Sudoku extends JComponent {

    private static final long serialVersionUID = 5546778616302943600L;

    private static final float FONT_SIZE = 24f;
    private static final int CELL_SIZE = 40;
    private static final int PADDING = 10;

    /* Work grid and the displayed grid. */
    private byte[][] grid;
    private byte[][] display;

    private Random rng;
    private Position origin = new Position((byte) 0, (byte) 0);
    private Stack<Position> positions;

    /**
     * Create a new Sudoku board.
     */
    public Sudoku() {
        grid = new byte[9][9];
        display = new byte[9][9];
        int side = CELL_SIZE * 9 + PADDING * 2;
        Dimension size = new Dimension(side, side);
        setPreferredSize(size);
        setMinimumSize(size);
        setOpaque(true);
        setBackground(Color.white);
        rng = new Random();
        initPositions();
        System.out.println("Generating ...");
        if (generate()) {
            System.out.println("Done.");
        } else {
            System.out.println("Fail.");
        }
        System.out.println("Givens: " + filled());
        swap();
    }

    /**
     * The main function.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        JFrame frame = new JFrame("Sudoku");
        frame.add(new Sudoku());
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    /**
     * Draw a Sudoku board on this component.
     *
     * @param g the graphics to be painted
     */
    public final void paintComponent(final Graphics g) {
        super.paintComponent(g);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        /* Draw the grid. */
        g.setColor(Color.black);
        int max = 9 * CELL_SIZE + PADDING + 1;
        for (int i = 0; i <= 9; i++) {
            int d = i * CELL_SIZE + PADDING;
            g.drawLine(d, PADDING, d, max);
            g.drawLine(PADDING, d, max, d);
        }
        for (int i = 0; i <= 9; i += 3) {
            int d = i * CELL_SIZE + PADDING;
            g.drawLine(d - 1, PADDING - 1, d - 1, max);
            g.drawLine(d + 1, PADDING + 1, d + 1, max);
            g.drawLine(PADDING - 1, d - 1, max, d - 1);
            g.drawLine(PADDING + 1, d + 1, max, d + 1);
        }

        /* Set the font. */
        g.setFont(g.getFont().deriveFont(FONT_SIZE));
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        /* Draw the numbers. */
        FontMetrics fm = g.getFontMetrics();
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int val = display[x][y];
                if (val > 0) {
                    int xx = x * CELL_SIZE + PADDING + CELL_SIZE / 2;
                    int yy = y * CELL_SIZE + PADDING + CELL_SIZE / 2;
                    String num = "" + val;
                    g.drawString(num, xx - fm.stringWidth(num) / 2,
                                 yy + fm.getAscent() / 2);
                }
            }
        }
    }

    /**
     * Create a symmetrical order the positions.
     */
    private void initPositions() {
        Stack<Position> tmp = new Stack<Position>();
        positions = new Stack<Position>();
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < y; x++) {
                Position pos = new Position(x, y);
                tmp.push(pos);
            }
        }
        for (byte i = 0; i < 4; i++) {
            tmp.push(new Position(i, i));
        }
        Collections.shuffle(tmp);
        while (!tmp.empty()) {
            Position pos = tmp.pop();
            Position mirror = mirror(pos);
            positions.push(pos);
            positions.push(mirror);
        }
    }

    /**
     * Return mirror of position.
     *
     * @param pos position to mirror
     * @return mirrored position
     */
    private Position mirror(final Position pos) {
        return new Position((byte) (8 - pos.getX()), (byte) (8 - pos.getY()));
    }

    /**
     * Return the number of filled positions.
     *
     * @return number of filled positions
     */
    private int filled() {
        int count = 0;
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (grid[x][y] > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get the value at position.
     *
     * @param p position
     * @return the value at the position
     */
    private byte get(final Position p) {
        return grid[p.getX()][p.getY()];
    }

    /**
     * Get the value at position.
     *
     * @param p position
     * @param val the new value
     */
    private void set(final Position p, final byte val) {
        grid[p.getX()][p.getY()] = val;
    }

    /**
     * Reset the position to empty.
     *
     * @param p position to reset.
     */
    private void unset(final Position p) {
        grid[p.getX()][p.getY()] = (byte) 0;
    }

    /**
     * Generate a new Sudoku puzzle.
     *
     * @return true if build was successful
     */
    private boolean generate() {
        Position pos = positions.pop();
        boolean[] possible = possible(pos);
        for (byte i = 0; i < 10; i++) {
            if (possible[i]) {
                set(pos, i);
                int solutions = numSolutions();
                if (solutions > 1) {
                    /* Keep filling in. */
                    if (generate()) {
                        return true;
                    }
                } else if (solutions == 1) {
                    /* Done, exactly one solution left. */
                    return true;
                }
            }
        }
        /* Failed to generate a sudoku from here. */
        unset(pos);
        positions.push(pos);
        return false;
    }

    /**
     * Return the number of solutions on the current board.
     *
     * We only care if this is 0, 1, or greater than 1, so it will
     * never actually return higher than 2.
     *
     * @return number of solutions
     */
    private int numSolutions() {
        Position pos = null;
        for (byte y = 0; pos == null && y < 9; y++) {
            for (byte x = 0; pos == null && x < 9; x++) {
                if (grid[x][y] == 0) {
                    pos = new Position(x, y);
                }
            }
        }
        if (pos == null) {
            /* Board is full i.e. solved. */
            return 1;
        }

        boolean[] possible = possible(pos);
        int count = 0;
        for (byte i = 0; i < 10; i++) {
            if (possible[i]) {
                set(pos, i);
                count += numSolutions();
                if (count > 1) {
                    unset(pos);
                    return 2;
                }
            }
        }
        unset(pos);
        return count;
    }

    /**
     * Possible values for given position.
     *
     * @param pos the position to check
     * @return list of value possibilities
     */
    private boolean[] possible(final Position pos) {
        boolean[] possible = new boolean[10];
        for (int i = 1; i < 10; i++) {
            possible[i] = true;
        }
        for (int x = 0; x < 9; x++) {
            possible[grid[x][pos.getY()]] = false;
        }
        for (int y = 0; y < 9; y++) {
            possible[grid[pos.getX()][y]] = false;
        }
        int xx = (pos.getX() / 3) * 3;
        int yy = (pos.getY() / 3) * 3;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                possible[grid[xx + x][yy + y]] = false;
            }
        }
        possible[0] = false;
        return possible;
    }

    /**
     * Swap the display and working grids.
     */
    private void swap() {
        byte[][] tmp = grid;
        grid = display;
        display = tmp;
    }
}
