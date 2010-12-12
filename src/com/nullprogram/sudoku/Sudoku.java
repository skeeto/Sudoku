package com.nullprogram.sudoku;

import java.util.Stack;
import java.util.Arrays;
import java.util.Random;
import java.util.Collections;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JComponent;

/**
 * A Sudoku board capable of generating puzzles and interacting.
 */
public class Sudoku extends JComponent
    implements KeyListener, MouseListener, Runnable {

    private static final long serialVersionUID = 5546778616302943600L;

    private static final float FONT_SIZE = 24f;
    private static final int CELL_SIZE = 40;
    private static final int PADDING = 10;
    private static final int TIMEOUT = 1500;

    /** The easiest difficulty (32 givens). */
    public static final int EASY = 32;

    /** A medium difficulty (28 givens). */
    public static final int MEDIUM = 28;

    /** A hard difficulty (24 givens). */
    public static final int HARD = 24;

    /* Work grid and the displayed grid. */
    private byte[][] grid;
    private byte[][] display;
    private byte[][] orig;
    private boolean[][] valid;

    private Random rng;
    private Position origin = new Position((byte) 0, (byte) 0);
    private Stack<Position> positions;
    private Stack<Position> used;
    private Position selected;
    private boolean playing = false;

    /* Generation variables. */
    private int givensGoal;
    private long initTime;
    private volatile boolean generating = false;
    private Timer timer;
    private int animateState;
    private int animateStateMax = 32;
    private int fade;

    /**
     * Create a new Sudoku board.
     */
    public Sudoku() {
        grid = new byte[9][9];
        display = new byte[9][9];
        orig = new byte[9][9];
        valid = new boolean[9][9];
        checkValid();
        int side = CELL_SIZE * 9 + PADDING * 2;
        Dimension size = new Dimension(side, side);
        setPreferredSize(size);
        setMinimumSize(size);
        setOpaque(true);
        setBackground(Color.white);
        rng = new Random();
        addKeyListener(this);
        addMouseListener(this);
        selected = origin;
    }

    /**
     * Create a new Sudoku puzzle.
     *
     * @param difficulty the sudoku's difficulty
     */
    public final void createSudoku(final int difficulty) {
        playing = false;
        givensGoal = difficulty;
        setBackground(Color.WHITE);
        if (!generating) {
            animateState = 0;
            ActionListener listener = new ActionListener() {
                public void actionPerformed(final ActionEvent evt) {
                    animateState++;
                    if (animateState >= animateStateMax) {
                        animateState = 0;
                    }
                    repaint();
                }
            };
            timer = new Timer(50, listener);
            timer.start();
            generating = true;
            clear(display);
            clear(orig);
            clear(grid);
            checkValid();
            (new Thread(this)).start();
            repaint();
        }
    }

    /** {@inheritDoc} */
    public final void run() {
        boolean failed;
        do {
            failed = false;
            try {
                clear(grid);
                initPositions();
                initTime = System.currentTimeMillis();
                System.out.println("Generating ...");
                System.out.println("result = " + generate());
                System.out.println("Eliminating ...");
                eliminate();
            } catch (TimeoutException e) {
                System.out.println("Timeout.");
                failed = true;
            }
        } while (failed || (filled() != givensGoal));
        System.out.println("Givens: " + filled() + " (" + givensGoal + ")");
        copy(grid, orig);
        swap();
        copy(orig, grid);
        checkValid();
        generating = false;
        playing = true;
        timer.stop();
        repaint();
    }

    /**
     * The main function.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        Sudoku sudoku = new Sudoku();
        JFrame frame = new JFrame("Sudoku");
        frame.add(sudoku);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
        sudoku.createSudoku(Sudoku.EASY);
        sudoku.requestFocusInWindow();
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

        if (!generating) {
            paintMarks(g);
            paintGrid(g);
            paintSelector(g);
            paintNumbers(g);
        } else {
            double angle = animateState * Math.PI * 2f / animateStateMax;
            int x = (int) (Math.cos(angle) * 4.5) + 4;
            int y = (int) (Math.sin(angle) * 4.5) + 4;
            g.setColor(Color.GREEN);
            g.fillRect(x * CELL_SIZE + PADDING,
                       y * CELL_SIZE + PADDING,
                       CELL_SIZE, CELL_SIZE);
            g.fillRect((8 - x) * CELL_SIZE + PADDING,
                       (8 - y) * CELL_SIZE + PADDING,
                       CELL_SIZE, CELL_SIZE);
            paintGrid(g);

            String message = "Generating";
            g.setFont(g.getFont().deriveFont(FONT_SIZE - 4));
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth(message);
            int height = fm.getHeight();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(getWidth() / 2 - width / 2 - PADDING,
                       getHeight() / 2 - height / 2 - PADDING,
                       width + PADDING * 2, height + PADDING * 2);
            g.setColor(Color.BLACK);
            g.drawString(message, getWidth() / 2 - width / 2,
                         getHeight() / 2 + height / 2);
        }
    }

    /**
     * Draw indiviual cell markings.
     *
     * @param g the graphics to paint
     */
    private void paintMarks(final Graphics g) {
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int marked = orig[x][y];
                if (marked > 0) {
                    g.setColor(Color.LIGHT_GRAY);
                } else if (!valid[x][y]) {
                    g.setColor(Color.YELLOW);
                } else {
                    g.setColor(getBackground());
                }
                g.fillRect(x * CELL_SIZE + PADDING,
                           y * CELL_SIZE + PADDING,
                           CELL_SIZE, CELL_SIZE);
            }
        }
    }

    /**
     * Draw the grid lines.
     *
     * @param g the graphics to paint
     */
    private void paintGrid(final Graphics g) {
        g.setColor(Color.BLACK);
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
    }

    /**
     * Draw the selector box.
     *
     * @param g the graphics to paint
     */
    private void paintSelector(final Graphics g) {
        if (!playing) {
            return;
        }
        if (selected != null) {
            g.setColor(Color.RED);
            int padding = 3;
            int thickness = 2;
            for (int i = padding; i < padding + thickness; i++) {
                g.drawRect(selected.getX() * CELL_SIZE + PADDING + i,
                           selected.getY() * CELL_SIZE + PADDING + i,
                           CELL_SIZE - i * 2, CELL_SIZE - i * 2);
            }
        }
    }

    /**
     * Draw the numbers.
     *
     * @param g the graphics to paint
     */
    private void paintNumbers(final Graphics g) {
        /* Set the font. */
        g.setFont(g.getFont().deriveFont(FONT_SIZE));
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        /* Draw the numbers. */
        g.setColor(Color.BLACK);
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
     * Clear a grid.
     *
     * @param array grid to be cleared
     */
    private void clear(final byte[][] array) {
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < 9; x++) {
                array[x][y] = 0;
            }
        }
    }

    /**
     * Create a symmetrical order the positions.
     */
    private void initPositions() {
        positions = new Stack<Position>();
        used = new Stack<Position>();
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < y; x++) {
                Position pos = new Position(x, y);
                positions.push(pos);
            }
        }
        for (byte i = 0; i < 4; i++) {
            positions.push(new Position(i, i));
        }
        Collections.shuffle(positions);
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
     * Set the value at position.
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
     * User wishes to set value at position.
     *
     * @param p position to set
     * @param val value to set it to
     */
    private void userSet(final Position p, final byte val) {
        if (!playing) {
            return;
        }
        int x = p.getX();
        int y = p.getY();
        if (orig[x][y] == 0) {
            display[x][y] = val;
            grid[x][y] = val;
        }
        checkValid();
        checkComplete();
    }

    /**
     * Check the validity of the current board.
     */
    private void checkValid() {
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < 9; x++) {
                Position pos = new Position(x, y);
                byte val = get(pos);
                if (val > 0) {
                    boolean[] possible = possible(grid, pos);
                    valid[x][y] = possible[val];
                } else {
                    valid[x][y] = true;
                }
            }
        }
    }

    /**
     * Check if the sudoku is complete.
     */
    private void checkComplete() {
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < 9; x++) {
                if (!valid[x][y] || (display[x][y] == 0)) {
                    return;
                }
            }
        }
        /* It is complete. */
        playing = false;
        fadeOut();
    }

    /**
     * Start fading the screen.
     */
    private void fadeOut() {
        fade = 0xFF;
        ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                fade -= 2;
                if (fade < 192) {
                    fade = 192;
                    ((Timer) e.getSource()).stop();
                }
                setBackground(new Color(fade, fade, fade));
                repaint();
            }
        };
        (new Timer(50, listener)).start();
    }

    /**
     * Auto-solve the currently selected position.
     */
    public final void hint() {
        if (selected != null) {
            byte[][] work = new byte[9][9];
            copy(orig, work);
            solve(work, 0);
            userSet(selected, work[selected.getX()][selected.getY()]);
        }
    }

    /**
     * Generate a new Sudoku puzzle.
     *
     * @return true if build was successful
     * @throws TimeoutException if generation took too long
     */
    private boolean generate() throws TimeoutException {
        Position pos1 = positions.pop();
        Position pos2 = mirror(pos1);
        used.push(pos1);
        boolean[] possible1 = possible(grid, pos1);
        for (byte i : rand10()) {
            if (possible1[i]) {
                set(pos1, i);
                for (byte j : rand10()) {
                    boolean[] possible2 = possible(grid, pos2);
                    if (possible2[j]) {
                        set(pos2, j);
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
            }
        }
        /* Failed to generate a sudoku from here. */
        unset(pos1);
        unset(pos2);
        positions.push(pos1);
        used.pop();
        return false;
    }

    /**
     * Try to eliminate some hints.
     *
     * @throws TimeoutException if generation took too long
     */
    private void eliminate() throws TimeoutException {
        Collections.shuffle(used);
        while (!used.empty() && filled() > givensGoal) {
            Position pos1 = used.pop();
            Position pos2 = mirror(pos1);
            byte val1 = get(pos1);
            byte val2 = get(pos2);
            unset(pos1);
            unset(pos2);
            if (numSolutions() > 1) {
                set(pos1, val1);
                set(pos2, val2);
            }
        }
    }

    /**
     * Return the number of solutions on the current board.
     *
     * We only care if this is 0, 1, or greater than 1, so it will
     * never actually return higher than 2.
     *
     * @return number of solutions
     * @throws TimeoutException if generation took too long
     */
    private int numSolutions() throws TimeoutException {
        if ((rng.nextInt(200) == 1)
                && (System.currentTimeMillis() - initTime) > TIMEOUT) {
            throw new TimeoutException();
        }
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

        boolean[] possible = possible(grid, pos);
        int count = 0;
        for (byte i : rand10()) {
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
     * Determine the grid's difficulty.
     *
     * @return the sudoku's difficulty
     */
    private int difficulty() {
        byte[][] work = new byte[9][9];
        copy(grid, work);
        return solve(work, 0) - (9 * 9 - filled());
    }

    /**
     * Return a randomly sorted array of 0-9.
     *
     * @return random list of 0-9
     */
    private static Byte[] rand10() {
        Byte[] nums = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        Collections.shuffle(Arrays.asList(nums));
        return nums;
    }

    /**
     * Solve a grid.
     *
     * @param work the sudoku to solve
     * @param depth current depth
     * @return the depth of the call stack at solution
     */
    private int solve(final byte[][] work, final int depth) {
        Position pos = null;
        for (byte y = 0; pos == null && y < 9; y++) {
            for (byte x = 0; pos == null && x < 9; x++) {
                if (work[x][y] == 0) {
                    pos = new Position(x, y);
                }
            }
        }
        if (pos == null) {
            return depth + 1;
        }
        int x = pos.getX();
        int y = pos.getY();

        boolean[] possible = possible(work, pos);
        for (byte i : rand10()) {
            if (possible[i]) {
                work[x][y] = i;
                int val = solve(work, depth + 1);
                if (val > 0) {
                    return val + 1;
                }
            }
        }
        work[x][y] = 0;
        return -1;
    }

    /**
     * Possible values for given position.
     *
     * @param work the sudoku board to use
     * @param pos the position to check
     * @return list of value possibilities
     */
    private static boolean[] possible(final byte[][] work,
                                      final Position pos) {
        boolean[] possible = new boolean[10];
        for (int i = 1; i < 10; i++) {
            possible[i] = true;
        }
        for (int x = 0; x < 9; x++) {
            if (x != pos.getX()) {
                possible[work[x][pos.getY()]] = false;
            }
        }
        for (int y = 0; y < 9; y++) {
            if (y != pos.getY()) {
                possible[work[pos.getX()][y]] = false;
            }
        }
        int xx = (pos.getX() / 3) * 3;
        int yy = (pos.getY() / 3) * 3;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if ((xx + x != pos.getX()) && (yy + y != pos.getY())) {
                    possible[work[xx + x][yy + y]] = false;
                }
            }
        }
        possible[0] = false;
        return possible;
    }

    /**
     * The number of possible values for a given position.
     *
     * @param pos the position to check
     * @return number of possible values
     */
    private int numPossible(final Position pos) {
        boolean[] possible = possible(grid, pos);
        int count = 0;
        for (int i = 1; i < 10; i++) {
            if (possible[i]) {
                count++;
            }
        }
        return count;
    }

    /**
     * Swap the display and working grids.
     *
     * This is so the display is always working with a consistent grid.
     */
    private void swap() {
        byte[][] tmp = grid;
        grid = display;
        display = tmp;
    }

    /**
     * Copy one grid to another.
     *
     * @param from origin grid
     * @param to destination grid
     */
    private void copy(final byte[][] from, final byte[][] to) {
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < 9; x++) {
                to[x][y] = from[x][y];
            }
        }
    }

    /**
     * Move the selected position in a direction.
     *
     * @param xDiff change in x-direction
     * @param yDiff change in y-direction
     */
    private void moveSelected(final int xDiff, final int yDiff) {
        if (selected != null) {
            byte x = (byte) (selected.getX() + xDiff);
            byte y = (byte) (selected.getY() + yDiff);
            setSelected(new Position(x, y));
        }
    }

    /**
     * Set the selected position to this position, if it is valid.
     *
     * @param pos position to set
     * @return true if position was selected
     */
    private boolean setSelected(final Position pos) {
        byte x = pos.getX();
        byte y = pos.getY();
        if ((x < 9) && (x >= 0) && (y < 9) && (y >= 0)) {
            selected = pos;
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    public final void keyTyped(final KeyEvent e) {
        char c = e.getKeyChar();
        if (!generating && (selected != null) && (c >= 48) && (c <= 57)) {
            /* Number 1..9 */
            int x = selected.getX();
            int y = selected.getY();
            userSet(selected, (byte) (c - 48));
        } else if (c == 'E') {
            createSudoku(EASY);
        } else if (c == 'M') {
            createSudoku(MEDIUM);
        } else if (c == 'H') {
            createSudoku(HARD);
        } else if (c == 'h') {
            hint();
        }
        repaint();
    }

    /** {@inheritDoc} */
    public final void keyPressed(final KeyEvent e) {
        if (e.isActionKey()) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                moveSelected(-1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                moveSelected(1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                moveSelected(0, -1);
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                moveSelected(0, 1);
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_DELETE
                || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (selected != null) {
                userSet(selected, (byte) 0);
            }
        }
        repaint();
    }

    /** {@inheritDoc} */
    public void keyReleased(final KeyEvent e) {
        /* Do nothing. */
    }

    /** {@inheritDoc} */
    public final void mouseReleased(final MouseEvent e) {
        double px = e.getPoint().getX();
        double py = e.getPoint().getY();
        int x = (int) Math.floor((px - PADDING) / CELL_SIZE);
        int y = (int) Math.floor((py - PADDING) / CELL_SIZE);
        if (!setSelected(new Position((byte) x, (byte) y))) {
            selected = null;
        }
        repaint();
    }

    /** {@inheritDoc} */
    public final void mouseClicked(final MouseEvent e) {
        requestFocusInWindow();
        requestFocus();
    }

    /** {@inheritDoc} */
    public void mouseExited(final MouseEvent e) {
        /* Do nothing */
    }

    /** {@inheritDoc} */
    public void mouseEntered(final MouseEvent e) {
        /* Do nothing */
    }

    /** {@inheritDoc} */
    public void mousePressed(final MouseEvent e) {
        /* Do nothing */
    }
}
