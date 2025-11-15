# CS5741 Assignment 2 – Forest Fire Simulation

Author: **Ismail Bahlaoui (ID25287575)**  
Module: **CS5741 – Concurrency & Parallelism**

This project implements a 2D forest fire simulation in Java, including a sequential baseline and a parallel version that uses data parallelism (row-block workers) and task parallelism (a statistics thread).  
The program prints runtime measurements, speedup, efficiency, the Karp–Flatt metric, and theoretical speedups from Amdahl’s and Gustafson’s laws.

## 1\. Requirements

- JDK 17 or later
- Terminal access to `javac` and `java`

## 2\. Compile

```
javac ID25287575_Assignment2.java
```

## 3\. Run

### Default configuration

Uses:

- size = 1000
- steps = 400
- pGrow = 0.01
- pBurn = 0.10

Run:

```
java ID25287575_Assignment2.java
```

### Custom configuration

```
java ID25287575_Assignment2.java <size> <steps> <pGrow> <pBurn>
```

Example:

```
java ID25287575_Assignment2.java 1000 400 0.01 0.10
```

## 4\. Program Output

The program prints:

- Average runtime `t(p)` for p = 1, 2, 4, 8
- Speedup `S(p)`
- Efficiency `E(p)`
- Karp–Flatt metric `ε(p)`
- Estimated parallel fraction from Amdahl’s Law
- Theoretical Amdahl and Gustafson speedups for comparison

## 5\. Reproducibility Notes

- Randomness is controlled by fixed seeds:
    - Initial grid uses the `seed` passed to `initialize`
    - Sequential run uses `Random(1234L)`
    - Parallel workers use seeds `2000L + threadId`
    - Main experiment uses `seed = 42L`

- Timing uses:
    - 1 warm-up run
    - 5 measured runs
    - Average of measured runs

<br>