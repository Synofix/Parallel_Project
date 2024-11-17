import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Server {
    private static final int PORT = 5556;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                executor.submit(new MatrixMultiplicationHandler(clientSocket));
            }
        }
    }
}

class MatrixMultiplicationHandler implements Runnable {
    private final Socket clientSocket;
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool();

    MatrixMultiplicationHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            // Receive multiple matrices
            int[][][] matrices = (int[][][]) in.readObject();
            System.out.println("Received " + matrices.length + " matrices for parallel multiplication.");

            // Measure parallel multiplication time
            long startParallel = System.nanoTime();
            int[][] resultMatrix = multiplyMatricesParallel(matrices);
            long endParallel = System.nanoTime();
            long parallelTime = endParallel - startParallel;
            System.out.println("Parallel multiplication completed in " + parallelTime / 1_000_000 + " ms.");

            // Measure serial multiplication time for baseline
            long startSerial = System.nanoTime();
            int[][] serialResult = multiplyMatricesSerial(matrices);
            long endSerial = System.nanoTime();
            long serialTime = endSerial - startSerial;
            long serialTimeInMs = serialTime / 1_000_000;
            System.out.println("Serial multiplication completed in " + serialTimeInMs + " ms.");

            // Calculate speedup and efficiency
            double speedUp = (double) serialTime / parallelTime;
            double efficiency = speedUp / Runtime.getRuntime().availableProcessors();

            System.out.println("Speedup: " + speedUp);
            System.out.println("Efficiency: " + efficiency);

            // Send results and performance metrics to client
            out.writeObject(resultMatrix);
            System.out.println("Result and metrics sent to client.");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    private int[][] multiplyMatricesSerial(int[][][] matrices) {
        int[][] result = matrices[0];

        for (int i = 1; i < matrices.length; i++) {
            result = multiplyMatricesSerial(result, matrices[i]);
        }
        return result;
    }

    private int[][] multiplyMatricesSerial(int[][] A, int[][] B) {
        int n = A.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = 0;
                for (int k = 0; k < n; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    private int[][] multiplyMatricesParallel(int[][][] matrices) {
        int[][] result = matrices[0];

        for (int i = 1; i < matrices.length; i++) {
            result = forkJoinPool.invoke(new MatrixMultiplicationTask(result, matrices[i]));
        }
        return result;
    }

    // RecursiveTask for parallel matrix multiplication
    private static class MatrixMultiplicationTask extends RecursiveTask<int[][]> {
        private final int[][] A;
        private final int[][] B;

        MatrixMultiplicationTask(int[][] A, int[][] B) {
            this.A = A;
            this.B = B;
        }

        @Override
        protected int[][] compute() {
            int n = A.length;
            if (n <= 64) {  // Base case for small matrices
                return multiplyMatrices(A, B);
            }

            int newSize = n / 2;
            int[][] a11 = new int[newSize][newSize];
            int[][] a12 = new int[newSize][newSize];
            int[][] a21 = new int[newSize][newSize];
            int[][] a22 = new int[newSize][newSize];
            int[][] b11 = new int[newSize][newSize];
            int[][] b12 = new int[newSize][newSize];
            int[][] b21 = new int[newSize][newSize];
            int[][] b22 = new int[newSize][newSize];

            // Submatrices
            divide(A, a11, 0, 0);
            divide(A, a12, 0, newSize);
            divide(A, a21, newSize, 0);
            divide(A, a22, newSize, newSize);
            divide(B, b11, 0, 0);
            divide(B, b12, 0, newSize);
            divide(B, b21, newSize, 0);
            divide(B, b22, newSize, newSize);

            // Fork tasks
            MatrixMultiplicationTask m1 = new MatrixMultiplicationTask(add(a11, a22), add(b11, b22));
            MatrixMultiplicationTask m2 = new MatrixMultiplicationTask(add(a21, a22), b11);
            MatrixMultiplicationTask m3 = new MatrixMultiplicationTask(a11, subtract(b12, b22));
            MatrixMultiplicationTask m4 = new MatrixMultiplicationTask(a22, subtract(b21, b11));
            MatrixMultiplicationTask m5 = new MatrixMultiplicationTask(add(a11, a12), b22);
            MatrixMultiplicationTask m6 = new MatrixMultiplicationTask(subtract(a21, a11), add(b11, b12));
            MatrixMultiplicationTask m7 = new MatrixMultiplicationTask(subtract(a12, a22), add(b21, b22));

            invokeAll(m1, m2, m3, m4, m5, m6, m7);

            // Combine results
            int[][] c11 = add(subtract(add(m1.join(), m4.join()), m5.join()), m7.join());
            int[][] c12 = add(m3.join(), m5.join());
            int[][] c21 = add(m2.join(), m4.join());
            int[][] c22 = add(subtract(add(m1.join(), m3.join()), m2.join()), m6.join());

            int[][] result = new int[n][n];
            combine(c11, result, 0, 0);
            combine(c12, result, 0, newSize);
            combine(c21, result, newSize, 0);
            combine(c22, result, newSize, newSize);

            return result;
        }

        private int[][] multiplyMatrices(int[][] A, int[][] B) {
            int n = A.length;
            int[][] result = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    result[i][j] = 0;
                    for (int k = 0; k < n; k++) {
                        result[i][j] += A[i][k] * B[k][j];
                    }
                }
            }
            return result;
        }

        private void divide(int[][] parent, int[][] child, int row, int col) {
            for (int i = 0; i < child.length; i++) {
                for (int j = 0; j < child.length; j++) {
                    child[i][j] = parent[i + row][j + col];
                }
            }
        }

        private void combine(int[][] child, int[][] parent, int row, int col) {
            for (int i = 0; i < child.length; i++) {
                for (int j = 0; j < child.length; j++) {
                    parent[i + row][j + col] = child[i][j];
                }
            }
        }

        private int[][] add(int[][] A, int[][] B) {
            int n = A.length;
            int[][] result = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    result[i][j] = A[i][j] + B[i][j];
                }
            }
            return result;
        }

        private int[][] subtract(int[][] A, int[][] B) {
            int n = A.length;
            int[][] result = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    result[i][j] = A[i][j] - B[i][j];
                }
            }
            return result;
        }
    }
}