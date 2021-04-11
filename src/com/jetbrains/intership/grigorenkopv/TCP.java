package com.jetbrains.intership.grigorenkopv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCP {
    // CONFIG VALUES
    private static final int SERVER_SOCKET_BACKLOG = 50;
    private static final int SERVER_THREAD_POOL_SIZE = 10;

    // COMMON CODE
    private static void printException(String message, Exception exception, PrintStream printStream) {
        printStream.printf("%s: %s%n", message, exception.getMessage());
    }

    private static void printException(String message, Exception exception) {
        printException(message, exception, System.err);
    }

    public static void main(String[] args) {
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "-c":
                case "client":
                case "--client":
                    client(args[1], args[2]);
                    return;
                case "-s":
                case "server":
                case "--server":
                    server(args[1], args[2]);
                    return;
            }
        }
        System.err.println("Usage: TCP <mode> <host> <port>");
        System.err.println("  mode:");
        System.err.println("    -c, --client, client\t\trun in a client mode");
        System.err.println("    -s, --server, server\t\trun in a server mode");
        System.err.println("  port:");
        System.err.println("    an integer between 0 and 65535 inclusive");
        System.err.println("    (Setting port to 0 in server mode");
        System.err.println("     will result into automatic port selection)");
    }

    // CLIENT CODE
    private static Long inputN(Scanner cin) {
        System.out.println("Please input N to get the Nth Fibonacci number or press Enter to exit.");
        final String input = cin.nextLine();
        try {
            return input.isEmpty() ? null : Long.parseLong(input);
        } catch (NumberFormatException e) {
            printException(input + " is not a valid number", e, System.out);
            return inputN(cin);
        }
    }

    public static byte[] longToBytes(long N) {
        return ByteBuffer.allocate(Long.BYTES).putLong(N).array();
    }

    public static void client(String host, String port) {
        try (final Socket socket = new Socket(host, Integer.parseInt(port))) {
            try (final OutputStream toServer = socket.getOutputStream()) {
                try (final Scanner fromServer = new Scanner(socket.getInputStream())) {
                    final Scanner cin = new Scanner(System.in);
                    Long N;
                    while ((N = inputN(cin)) != null) {
                        toServer.write(longToBytes(N));
                        toServer.flush();
                        System.out.println(fromServer.nextLine());
                    }
                } catch (IOException e) {
                    printException("Couldn't open InputStream from server", e);
                }
            } catch (IOException e) {
                printException("Couldn't open/close OutputStream to server", e);
            }
        } catch (UnknownHostException e) {
            printException("Couldn't resolve host", e);
        } catch (IllegalArgumentException e) {
            printException("Malformed or invalid port number \"" + port + "\"", e);
        } catch (IOException e) {
            printException("Couldn't open/close a socket", e);
        }
    }

    // SERVER CODE
    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.allocate(Long.BYTES).put(bytes).flip().getLong();
    }

    private static boolean readLong(InputStream inputStream, byte[] buffer) throws IOException {
        // returns false if end of stream encountered before it was possible to read enough bytes
        // true otherwise
        assert buffer.length == Long.BYTES;
        int bytesLeft = buffer.length;
        while (bytesLeft > 0) {
            int bytesRead = inputStream.read(buffer, buffer.length - bytesLeft, bytesLeft);
            if (bytesRead == -1) {
                return false;
            } else {
                bytesLeft -= bytesRead;
            }
        }
        return true;
    }

    private static void writeBigInteger(OutputStream outputStream, BigInteger bigInteger) throws IOException {
        outputStream.write(bigInteger.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static void serveClient(final Socket socket) {
        System.err.println("New connection: " + socket);
        final String errorMessagePrefix = "While serving " + socket + ": ";
        try (socket) {
            try (final InputStream fromClient = socket.getInputStream()) {
                try (final OutputStream toClient = socket.getOutputStream()) {
                    boolean keepAlive = true;
                    final byte[] buffer = new byte[Long.BYTES];
                    while (keepAlive) {
                        try {
                            if (keepAlive = readLong(fromClient, buffer)) {
                                try {
                                    writeBigInteger(toClient, Fibonacci.getNth(bytesToLong(buffer)));
                                } catch (IOException e) {
                                    printException(errorMessagePrefix + "Couldn't send the result to client", e);
                                    keepAlive = false;
                                }
                            }
                        } catch (IOException e) {
                            printException(errorMessagePrefix + "Couldn't receive N from client", e);
                            keepAlive = false;
                        }
                    }
                } catch (IOException e) {
                    printException(errorMessagePrefix + "Couldn't open/close OutputStream to client", e);
                }
            } catch (IOException e) {
                printException(errorMessagePrefix + "Couldn't open/close InputStream from client", e);
            }
        } catch (IOException e) {
            printException(errorMessagePrefix + "Couldn't close a socket", e);
        }
        System.err.println("Finished serving: " + socket);
    }

    public static void server(String host, String port) {
        try (final ServerSocket serverSocket =
                     new ServerSocket(Integer.parseInt(port), SERVER_SOCKET_BACKLOG, InetAddress.getByName(host))) {
            System.err.println("Serving at " + serverSocket.getInetAddress() + " on port " + serverSocket.getLocalPort());
            final ExecutorService serverThreadPool = Executors.newFixedThreadPool(SERVER_THREAD_POOL_SIZE);
            try {
                while (true) {
                    try {
                        final Socket clientSocket = serverSocket.accept();
                        serverThreadPool.execute(() -> serveClient(clientSocket));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                serverThreadPool.shutdown();
            }
        } catch (UnknownHostException e) {
            printException("Couldn't resolve host", e);
        } catch (IllegalArgumentException e) {
            printException("Malformed or invalid port number \"" + port + "\"", e);
        } catch (IOException e) {
            printException("Couldn't open/close a socket", e);
        }
    }
}
