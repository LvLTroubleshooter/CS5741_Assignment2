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

    public static void main(String[] args) {
        int size = 1000;
        int steps = 400;
        double pGrow = 0.01;
        double pBurn = 0.1;

        ID25287575_Assignment2 sim =
                new ID25287575_Assignment2(size, steps, pGrow, pBurn);

        System.out.println("Forest sim created: size=" + size + ", steps=" + steps);
    }

}