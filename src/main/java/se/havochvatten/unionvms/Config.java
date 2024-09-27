package se.havochvatten.unionvms;

public class Config {
    static final int PORT = 8040;
    static final int CONFIG_PORT = 8041;

    private Config() {
        // ignore
    }

    public static int getNthPos() {
        return Integer.parseInt(System.getProperty("ais_nth_pos", "1"));
    }

    public static String getSimFile() {
        return System.getProperty("sim_file", "aisdk_20190513.csv");
    }

    public static boolean isSimulateStuckSocket() {
        return Boolean.parseBoolean(System.getProperty("simulate_stuck_socket", "false"));
    }
}
