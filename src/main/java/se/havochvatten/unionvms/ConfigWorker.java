package se.havochvatten.unionvms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConfigWorker implements Runnable {
    public static final String SIMULATE_STUCK_SOCKET = "simulate_stuck_socket";

    private final Socket clientSocket;

    public ConfigWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            out.println("Welcome to the config channel. Type 'help' for available commands.");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if ("sim_stuck".equals(inputLine)) {
                    boolean simulateStuckSocket = Config.isSimulateStuckSocket();
                    if (simulateStuckSocket) {
                        System.setProperty(SIMULATE_STUCK_SOCKET, "false");
                        System.out.println("will disable socket stuck simulation");
                        out.println("will disable socket stuck simulation");
                    } else {
                        System.setProperty(SIMULATE_STUCK_SOCKET, "true");
                        System.out.println("will enable socket stuck simulation");
                        out.println("will enable socket stuck simulation");
                    }
                } else if ("help".equals(inputLine)) {
                    out.println("You can turn on/off simulating stuck socket with sim_stuck");
                }
            }
        } catch (IOException e) {
            System.out.println("Got exception in config worker: " + e.getLocalizedMessage());
        }
        System.out.println("Config worker exiting for " + clientSocket);
    }
}
