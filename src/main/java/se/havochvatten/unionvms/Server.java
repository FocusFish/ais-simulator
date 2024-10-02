package se.havochvatten.unionvms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private final int port;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int nthPos = Config.getNthPos();
                System.out.println("New client connected " + clientSocket + "... using nth_pos = " + nthPos);
                if (port == Config.PORT) {
                    new Thread(new Worker(clientSocket, nthPos)).start();
                } else {
                    new Thread(new ConfigWorker(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            System.out.println("Got error in server: " + e.getLocalizedMessage());
        }
    }
}
