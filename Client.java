import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {
    public static void main(String[] args) throws IOException {
        String serverIP = "192.168.1.64"; // Server IP
        int serverPort = 5556; // Server port
        int matrixSize = 8; 
        int matrixCount = 2; // Any power of 2

        // Inside MatrixClient main method
        int[][][] matrices = new int[matrixCount][matrixSize][matrixSize];

        System.out.println("Generated matrices.");

        for (int i = 0; i < matrixCount; i++) {
            matrices[i] = generateMatrix(matrixSize);
        }

        try (Socket socket = new Socket(serverIP, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Send matrices to server
            out.writeObject(matrices);
            System.out.println("Matrices sent to server.");


            // Receive result matrix from server
            int[][] resultMatrix = (int[][]) in.readObject();
            System.out.println("Received Result Matrix:");
            printMatrix(resultMatrix); 
            //Uncomment to see the full matrix
        } catch (ClassNotFoundException e) {
            System.err.println("Error reading result matrix: " + e.getMessage());
        }
    }

    private static int[][] generateMatrix(int size) {
        Random random = new Random();
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }
        return matrix;
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }
}
