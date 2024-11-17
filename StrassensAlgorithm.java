public class StrassensAlgorithm {

    public static int[][] multiply(int[][] A, int[][] B) {
        int n = A.length;

        // Base case
        if (n == 1) {
            int[][] result = {{A[0][0] * B[0][0]}};
            return result;
        }

        // Split matrices into quarters
        int newSize = n / 2;
        int[][] A11 = new int[newSize][newSize];
        int[][] A12 = new int[newSize][newSize];
        int[][] A21 = new int[newSize][newSize];
        int[][] A22 = new int[newSize][newSize];

        int[][] B11 = new int[newSize][newSize];
        int[][] B12 = new int[newSize][newSize];
        int[][] B21 = new int[newSize][newSize];
        int[][] B22 = new int[newSize][newSize];

        // Divide A and B into submatrices
        splitMatrix(A, A11, 0, 0);
        splitMatrix(A, A12, 0, newSize);
        splitMatrix(A, A21, newSize, 0);
        splitMatrix(A, A22, newSize, newSize);

        splitMatrix(B, B11, 0, 0);
        splitMatrix(B, B12, 0, newSize);
        splitMatrix(B, B21, newSize, 0);
        splitMatrix(B, B22, newSize, newSize);

        // Compute the seven products using recursion
        int[][] M1 = multiply(addMatrices(A11, A22), addMatrices(B11, B22));
        int[][] M2 = multiply(addMatrices(A21, A22), B11);
        int[][] M3 = multiply(A11, subtractMatrices(B12, B22));
        int[][] M4 = multiply(A22, subtractMatrices(B21, B11));
        int[][] M5 = multiply(addMatrices(A11, A12), B22);
        int[][] M6 = multiply(subtractMatrices(A21, A11), addMatrices(B11, B12));
        int[][] M7 = multiply(subtractMatrices(A12, A22), addMatrices(B21, B22));

        // Calculate final quadrants of the result matrix
        int[][] C11 = addMatrices(subtractMatrices(addMatrices(M1, M4), M5), M7);
        int[][] C12 = addMatrices(M3, M5);
        int[][] C21 = addMatrices(M2, M4);
        int[][] C22 = addMatrices(subtractMatrices(addMatrices(M1, M3), M2), M6);

        // Combine submatrices into the result matrix
        int[][] C = new int[n][n];
        joinMatrix(C11, C, 0, 0);
        joinMatrix(C12, C, 0, newSize);
        joinMatrix(C21, C, newSize, 0);
        joinMatrix(C22, C, newSize, newSize);

        return C;
    }

    // Helper method to add two matrices
    private static int[][] addMatrices(int[][] A, int[][] B) {
        int n = A.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    // Helper method to subtract two matrices
    private static int[][] subtractMatrices(int[][] A, int[][] B) {
        int n = A.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    // Helper method to split a matrix into submatrices
    private static void splitMatrix(int[][] parent, int[][] child, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            System.arraycopy(parent[i + row], col, child[i], 0, child.length);
        }
    }

    // Helper method to join submatrices into a larger matrix
    private static void joinMatrix(int[][] child, int[][] parent, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            System.arraycopy(child[i], 0, parent[i + row], col, child.length);
        }
    }
}