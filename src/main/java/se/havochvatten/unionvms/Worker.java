package se.havochvatten.unionvms;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Worker implements Runnable {

    private final Socket clientSocket;
    private final AISEncoder encoder;
    private final int nthPosition;

    public Worker(Socket clientSocket, int nthPosition) {
        this.clientSocket = clientSocket;
        this.encoder = new AISEncoder();
        this.nthPosition = nthPosition;
    }

    @Override
    public void run() {
        try {
            while (true) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                String file = Config.getSimFile();
                if (file.endsWith("csv")) {
                    replayCsvFile(file, out);
                } else if (file.endsWith("zip")) {
                    replayZipFile(file, out);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Got exception in worker: " + e.getLocalizedMessage());
        }
        System.out.println("Worker exiting for " + clientSocket);
    }

    private void replayCsvFile(String file, PrintWriter out) throws IOException, InterruptedException {
        try (Reader csvFileReader = new FileReader(file)) {
            Iterable<CSVRecord> aisMessages = CSVFormat.DEFAULT.builder().setHeader().build().parse(csvFileReader);
            replayMessages(aisMessages, out);
        }
    }

    private void replayZipFile(String file, PrintWriter out) throws IOException, InterruptedException {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                try (InputStream inputStream = zipFile.getInputStream(entry);
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                    Iterable<CSVRecord> aisMessages = CSVFormat.DEFAULT.builder().setHeader().build().parse(inputStreamReader);
                    replayMessages(aisMessages, out);
                }
            }
        }
    }

    private void replayMessages(Iterable<CSVRecord> aisMessages, PrintWriter out) throws InterruptedException, IOException {
        long pos = -1;
        for (CSVRecord aisMessage : aisMessages) {
            if (out.checkError()) {
                clientSocket.close();
                return;
            }

            pos++;

            String timestamp = aisMessage.get("# Timestamp");
            LocalTime now = LocalTime.now(ZoneId.of("UTC"));
            LocalTime aisTimestamp = LocalTime.parse(timestamp.split("\\s+")[1], DateTimeFormatter.ofPattern("HH:mm:ss"));

            if (shouldSkipMessage(aisTimestamp, now, pos)) {
                continue;
            }

            out.println(encoder.encode(aisMessage));
            if (aisTimestamp.getMinute() % 5 == 0) {
                out.println(encoder.encodeType5(aisMessage));
            }
        }
    }

    private boolean shouldSkipMessage(LocalTime aisTimestamp, LocalTime now, long pos) throws InterruptedException {
        if (Config.isSimulateStuckSocket()) {
            return true;
        }

        if (aisTimestamp.isBefore(now.minusSeconds(5))) {
            return true;
        }

        if (aisTimestamp.isAfter(now)) {
            Thread.sleep(1000);
            return true;
        }

        return pos % nthPosition != 0;
    }
}
