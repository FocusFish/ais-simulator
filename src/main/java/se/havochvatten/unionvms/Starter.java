package se.havochvatten.unionvms;

public class Starter {

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server(Config.PORT);
        Server configServer = new Server(Config.CONFIG_PORT);
        System.out.println("Starting server...");
        Thread serverThread = new Thread(server);
        serverThread.start();
        new Thread(configServer).start();

        System.out.println("Server started with config: \nport=" + Config.PORT +
                "\nconfigPort=" + Config.CONFIG_PORT +
                "\nnth_pos=" + Config.getNthPos() +
                "\nsim_file=" + Config.getSimFile() +
                "\nsimulate_stuck_socket=" + Config.isSimulateStuckSocket());
        serverThread.join();
    }
}