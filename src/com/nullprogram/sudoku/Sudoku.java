package com.nullprogram.sudoku;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JComponent;

public class Sudoku extends JComponent {

    private static final long serialVersionUID = 5546778616302943600L;

    private static final float FONT_SIZE = 24f;
    private static final int CELL_SIZE = 40;
    private static final int PADDING = 10;

    /* Work grid and the displayed grid. */
    private byte[][] grid;
    private byte[][] display;

    public Sudoku() {
        grid = new byte[9][9];
        display = new byte[9][9];
        int side = CELL_SIZE * 9 + PADDING * 2;
        Dimension size = new Dimension(side, side);
        setPreferredSize(size);
        setMinimumSize(size);
        setOpaque(true);
        setBackground(Color.white);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Sudoku");
        frame.add(new Sudoku());
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    public void paintComponent(Graphics g) {
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
     * Swap the display and working grids.
     */
    private void swap() {
        byte[][] tmp = grid;
        grid = display;
        display = grid;
    }
}
