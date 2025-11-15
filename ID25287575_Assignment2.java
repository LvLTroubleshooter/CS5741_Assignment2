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

    // one parallel run with numThreads workers + 1 stats task, returns time in ms
    public double runParallelOnceMs(int numThreads, long seed) throws InterruptedException {
        initialize(seed);

        // barrier for all workers + stats thread
        int parties = numThreads + 1;
        StepBarrier barrier = new StepBarrier(parties);

        // localCounts[thread][step][state]
        long[][][] localCounts = new long[numThreads][steps][3];
        // globalCounts[step][state] (not printed but used by stats task)
        long[][] globalCounts = new long[steps][3];

        Thread[] workers = new Thread[numThreads];

        // block decomposition of rows
        int baseRows = size / numThreads;
        int extra = size % numThreads;
        int startRow = 0;

        for (int t = 0; t < numThreads; t++) {
            int rows = baseRows + (t < extra ? 1 : 0);
            int endRow = startRow + rows;

            Worker worker = new Worker(
                    t,
                    startRow,
                    endRow,
                    barrier,
                    localCounts[t],
                    steps,
                    size,
                    this,
                    2000L + t
            );

            workers[t] = new Thread(worker, "worker-" + t);
            startRow = endRow;
        }

        // stats thread for task parallelism (aggregation)
        StatsTask statsTask = new StatsTask(localCounts, globalCounts, barrier, steps, numThreads);
        Thread statsThread = new Thread(statsTask, "stats");

        long tStart = System.nanoTime();

        statsThread.start();
        for (Thread w : workers) {
            w.start();
        }

        for (Thread w : workers) {
            w.join();
        }
        statsThread.join();

        long tEnd = System.nanoTime();
        return (tEnd - tStart) / 1_000_000.0;
    }

    // worker: data-parallel update on a block of rows
    private static class Worker implements Runnable {

        private final int id;
        private final int startRow;
        private final int endRow;
        private final StepBarrier barrier;
        private final long[][] localCounts; // [step][state]
        private final int steps;
        private final int size;
        private final ID25287575_Assignment2 sim;
        private final Random rnd;

        Worker(int id,
               int startRow,
               int endRow,
               StepBarrier barrier,
               long[][] localCounts,
               int steps,
               int size,
               ID25287575_Assignment2 sim,
               long seed) {

            this.id = id;
            this.startRow = startRow;
            this.endRow = endRow;
            this.barrier = barrier;
            this.localCounts = localCounts;
            this.steps = steps;
            this.size = size;
            this.sim = sim;
            this.rnd = new Random(seed);
        }

        @Override
        public void run() {
            try {
                for (int step = 0; step < steps; step++) {

                    // update our rows and count locally
                    for (int r = startRow; r < endRow; r++) {
                        for (int c = 0; c < size; c++) {
                            int newState = sim.updateCellRandom(r, c, sim.currentGrid, rnd);
                            sim.nextGrid[r][c] = newState;
                            localCounts[step][newState]++;
                        }
                    }

                    // first barrier: workers + stats sync here
                    barrier.await();

                    // one thread does the swap
                    if (id == 0) {
                        synchronized (barrier) {
                            sim.swapGrids();
                        }
                    }

                    // second barrier: wait until swap done
                    barrier.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // stats thread: aggregates local counts per step
    private static class StatsTask implements Runnable {

        private final long[][][] localCounts; // [thread][step][state]
        private final long[][] globalCounts;  // [step][state]
        private final StepBarrier barrier;
        private final int steps;
        private final int numThreads;

        StatsTask(long[][][] localCounts,
                  long[][] globalCounts,
                  StepBarrier barrier,
                  int steps,
                  int numThreads) {

            this.localCounts = localCounts;
            this.globalCounts = globalCounts;
            this.barrier = barrier;
            this.steps = steps;
            this.numThreads = numThreads;
        }

        @Override
        public void run() {
            try {
                for (int step = 0; step < steps; step++) {
                    // wait until workers finished this step
                    barrier.await();

                    long empty = 0;
                    long tree = 0;
                    long burning = 0;

                    for (int t = 0; t < numThreads; t++) {
                        empty   += localCounts[t][step][EMPTY];
                        tree    += localCounts[t][step][TREE];
                        burning += localCounts[t][step][BURNING];
                    }

                    globalCounts[step][EMPTY]   = empty;
                    globalCounts[step][TREE]    = tree;
                    globalCounts[step][BURNING] = burning;

                    // wait again so workers do not start too early
                    barrier.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // simple barrier built with synchronized + wait/notifyall
    private static class StepBarrier {

        private final int parties;
        private int arrived = 0;
        private int generation = 0;

        StepBarrier(int parties) {
            this.parties = parties;
        }

        public synchronized void await() throws InterruptedException {
            int currentGen = generation;
            arrived++;
            if (arrived == parties) {
                arrived = 0;
                generation++;
                notifyAll();
            } else {
                while (currentGen == generation) {
                    wait();
                }
            }
        }
    }

    // average sequential: warmupruns warmup, then measuredruns runs
    private static double averageSequentialMs(ID25287575_Assignment2 sim,
                                              long seed,
                                              int warmupRuns,
                                              int measuredRuns) {

        for (int i = 0; i < warmupRuns; i++) {
            sim.runSequentialOnceMs(seed);
        }

        double sumMs = 0.0;
        for (int i = 0; i < measuredRuns; i++) {
            sumMs += sim.runSequentialOnceMs(seed);
        }
        return sumMs / measuredRuns;
    }

    // average parallel: warmupruns warmup, then measuredruns runs
    private static double averageParallelMs(ID25287575_Assignment2 sim,
                                            int numThreads,
                                            long seed,
                                            int warmupRuns,
                                            int measuredRuns) throws InterruptedException {

        for (int i = 0; i < warmupRuns; i++) {
            sim.runParallelOnceMs(numThreads, seed);
        }

        double sumMs = 0.0;
        for (int i = 0; i < measuredRuns; i++) {
            sumMs += sim.runParallelOnceMs(numThreads, seed);
        }
        return sumMs / measuredRuns;
    }

    // main: runs timing and prints analysis values
    public static void main(String[] args) throws InterruptedException {
        int size = 1000;
        int steps = 400;
        double pGrow = 0.01;
        double pBurn = 0.1;


        if (args.length >= 4) {
            size  = Integer.parseInt(args[0]);
            steps = Integer.parseInt(args[1]);
            pGrow = Double.parseDouble(args[2]);
            pBurn = Double.parseDouble(args[3]);
        }

        ID25287575_Assignment2 sim =
                new ID25287575_Assignment2(size, steps, pGrow, pBurn);

        int warmupRuns   = 1;
        int measuredRuns = 5;
        long seed = 42L;

        // timing sequential
        double seqTimeAvgMs = averageSequentialMs(sim, seed, warmupRuns, measuredRuns);

        // timing parallel
        int[] threadCounts = {2, 4, 8};
        double[] parTimeAvgMs = new double[threadCounts.length];

        for (int i = 0; i < threadCounts.length; i++) {
            parTimeAvgMs[i] = averageParallelMs(sim, threadCounts[i], seed, warmupRuns, measuredRuns);
        }

        System.out.println();
        System.out.println("timing (ms, average):");
        System.out.println("p   t(p)");
        System.out.printf("%-3d %.3f%n", 1, seqTimeAvgMs);
        for (int i = 0; i < threadCounts.length; i++) {
            System.out.printf("%-3d %.3f%n", threadCounts[i], parTimeAvgMs[i]);
        }

        // measured speedup, efficiency, karp-flatt
        double[] speedupMeasured = new double[threadCounts.length];
        double[] efficiency = new double[threadCounts.length];
        double[] epsilonKarpFlatt = new double[threadCounts.length];

        System.out.println();
        System.out.println("speedup, efficiency, karp-flatt:");
        System.out.println("p   s(p)      e(p)      eps(p)");

        for (int i = 0; i < threadCounts.length; i++) {
            int P = threadCounts[i];
            speedupMeasured[i] = seqTimeAvgMs / parTimeAvgMs[i];
            efficiency[i] = speedupMeasured[i] / P;
            epsilonKarpFlatt[i] =
                    (1.0 / speedupMeasured[i] - 1.0 / P) / (1.0 - 1.0 / P);
            System.out.printf(
                    "%-3d %-9.3f %-9.3f %-10.5f%n",
                    P,
                    speedupMeasured[i],
                    efficiency[i],
                    epsilonKarpFlatt[i]
            );
        }

        // estimate parallel fraction p (amdahl) using largest p
        int Pref = threadCounts[threadCounts.length - 1];
        double SrefMeasured = speedupMeasured[threadCounts.length - 1];
        double pEstAmdahl =
                (1.0 - 1.0 / SrefMeasured) / (1.0 - 1.0 / Pref);

        System.out.println();
        System.out.println("estimated parallel fraction (amdahl):");
        System.out.printf("p_est ≈ %.4f%n", pEstAmdahl);

        // theoretical amdahl and gustafson speedups
        System.out.println();
        System.out.println("theoretical speedup (amdahl & gustafson):");
        System.out.println("p   s_amdahl   s_gustafson");

        for (int P : threadCounts) {
            double speedupAmdahlTheory =
                    1.0 / ((1.0 - pEstAmdahl) + pEstAmdahl / P);
            double speedupGustafsonTheory =
                    (1.0 - pEstAmdahl) + pEstAmdahl * P;
            System.out.printf(
                    "%-3d %-10.3f %-12.3f%n",
                    P,
                    speedupAmdahlTheory,
                    speedupGustafsonTheory
            );
        }
    }

}