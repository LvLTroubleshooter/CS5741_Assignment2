/*
 * id25287575_assignment2.java
 * student id: 25287575
 * name: ismail bahlaoui
 *
 * forest fire simulation for cs5741 assignment 2.
 */

import java.util.Random;

public class ID25287575_Assignment2 {

    // cell states
    private static final int EMPTY   = 0;
    private static final int TREE    = 1;
    private static final int BURNING = 2;

    // simulation settings
    private final int size;   // grid is size x size
    private final int steps;  // number of time steps
    private final double pGrow;
    private final double pBurn;

    // double-buffered grids
    private int[][] currentGrid;
    private int[][] nextGrid;

    public ID25287575_Assignment2(int size, int steps, double pGrow, double pBurn) {
        this.size = size;
        this.steps = steps;
        this.pGrow = pGrow;
        this.pBurn = pBurn;
        this.currentGrid = new int[size][size];
        this.nextGrid = new int[size][size];
    }

    // init forest with trees and some burning cells
    public void initialize(long seed) {
        Random rnd = new Random(seed);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                double x = rnd.nextDouble();
                if (x < 0.6) {
                    currentGrid[r][c] = TREE;
                    if (rnd.nextDouble() < 0.001) {
                        currentGrid[r][c] = BURNING;
                    }
                } else {
                    currentGrid[r][c] = EMPTY;
                }
            }
        }
    }

    // swap grids
    private void swapGrids() {
        int[][] tmp = currentGrid;
        currentGrid = nextGrid;
        nextGrid = tmp;
    }

    // check if cell has at least one burning neighbour (8-neighbourhood, torus)
    private boolean hasBurningNeighbour(int row, int col, int[][] grid) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int rr = (row + dr + size) % size;
                int cc = (col + dc + size) % size;
                if (grid[rr][cc] == BURNING) {
                    return true;
                }
            }
        }
        return false;
    }

    // forest fire update rule with randomness
    private int updateCellRandom(int row, int col, int[][] grid, Random rnd) {
        int state = grid[row][col];

        if (state == BURNING) {
            // burning tree becomes empty
            return EMPTY;
        }

        if (state == TREE) {
            // tree may catch fire if neighbour burning
            if (hasBurningNeighbour(row, col, grid)) {
                if (rnd.nextDouble() < pBurn) {
                    return BURNING;
                }
            }
            return TREE;
        }

        // empty may grow a tree
        if (rnd.nextDouble() < pGrow) {
            return TREE;
        }
        return EMPTY;
    }

    // one sequential run, returns time in ms
    public double runSequentialOnceMs(long seed) {
        initialize(seed);
        Random rnd = new Random(1234L);

        long tStart = System.nanoTime();

        for (int step = 0; step < steps; step++) {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    int newState = updateCellRandom(r, c, currentGrid, rnd);
                    nextGrid[r][c] = newState;
                }
            }
            swapGrids();
        }

        long tEnd = System.nanoTime();
        return (tEnd - tStart) / 1_000_000.0;
    }

    public static void main(String[] args) {
        int size = 50;
        int steps = 400;
        double pGrow = 0.01;
        double pBurn = 0.1;

        ID25287575_Assignment2 sim =
                new ID25287575_Assignment2(size, steps, pGrow, pBurn);

        long seed = 42L;
        double t = sim.runSequentialOnceMs(seed);
        System.out.printf("Sequential run took %.3f ms%n", t);
    }

}