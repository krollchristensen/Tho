import org.example.ClientReader;
import org.example.Json;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class ChatClient {
    private final String host;
    private final int port;
    private String username;
    private BufferedReader in;
    private PrintWriter out;

    private File pendingFile; // sendes når vi får ACCEPTED

    public ChatClient(String host, int port) {
        this.host = host; this.port = port;
    }

    public void start(String username) throws IOException {
        this.username = username;
        Socket socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        // login
        send(Json.obj("type","LOGIN","from",username));

        ClientReader reader = new ClientReader(in, this::onAccepted);
        Thread t = new Thread(reader, "client-reader");
        t.setDaemon(true);
        t.start();

        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            help();
            String line;
            while ((line = console.readLine()) != null) {
                if (line.equals("/quit")) break;

                if (line.startsWith("/sendfile ")) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length < 3) { System.out.println("brug: /sendfile <to> <path>"); continue; }
                    String to = parts[1];
                    File f = new File(parts[2]);
                    if (!f.exists() || !f.isFile()) { System.out.println("fil findes ikke"); continue; }
                    long size = f.length();
                    send(Json.obj("type","FILE_OFFER","from",username,"to",to,"filename",f.getName(),"size",size));
                    pendingFile = f;
                } else if (line.startsWith("/accept ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 3) { System.out.println("brug: /accept <transferId> <saveAsPath>"); continue; }
                    String transferId = parts[1];
                    String saveAs = parts[2];
                    try {
                        FileOutputStream fos = new FileOutputStream(saveAs);
                        reader.registerDownload(transferId, fos);
                        send(Json.obj("type","FILE_ACCEPT","from",username,"transferId",transferId));
                        System.out.println("accepteret " + transferId + ", gemmes som " + saveAs);
                    } catch (FileNotFoundException e) {
                        System.out.println("kan ikke skrive til " + saveAs);
                    }
                } else if (line.startsWith("/reject ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) { System.out.println("brug: /reject <transferId>"); continue; }
                    String transferId = parts[1];
                    send(Json.obj("type","FILE_REJECT","from",username,"transferId",transferId));
                    System.out.println("afvist " + transferId);
                } else if (line.equals("/help")) {
                    help();
                } else {
                    System.out.println("ukendt kommando, skriv /help");
                }
            }
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void onAccepted(String transferId) {
        if (pendingFile == null) return;
        File f = pendingFile;
        pendingFile = null;
        streamFile(transferId, f);
    }

    private void streamFile(String transferId, File file) {
        System.out.println("sender fil: " + file.getName() + " (" + file.length() + " bytes)");
        final int CHUNK = 32 * 1024;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            byte[] buf = new byte[CHUNK];
            long seq = 0;
            int n;
            while ((n = is.read(buf)) != -1) {
                String b64 = java.util.Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(buf, n));
                send(Json.obj(
                        "type","FILE_CHUNK",
                        "from", username,
                        "transferId", transferId,
                        "seq", seq++,
                        "data", b64
                ));
            }
            send(Json.obj("type","FILE_END","from",username,"transferId",transferId));
            System.out.println("fil sendt.");
        } catch (IOException e) {
            System.out.println("kunne ikke sende fil: " + e.getMessage());
        }
    }

    private void send(Map<String, Object> json) { out.println(Json.stringify(json)); }

    private void help() {
        System.out.println("kommandoer:");
        System.out.println("/sendfile <to> <path>");
        System.out.println("/accept <transferId> <saveAsPath>");
        System.out.println("/reject <transferId>");
        System.out.println("/help");
        System.out.println("/quit");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("brug: java ChatClient <host> <port> <username>");
            return;
        }
        new ChatClient(args[0], Integer.parseInt(args[1])).start(args[2]);
    }
}
