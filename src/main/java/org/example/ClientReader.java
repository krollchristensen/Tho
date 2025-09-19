package org.example;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientReader implements Runnable {
    private final BufferedReader in;
    private final ConcurrentHashMap<String, FileOutputStream> downloads = new ConcurrentHashMap<>();

    public interface OnAccepted { void onAccepted(String transferId); }
    private final OnAccepted onAccepted;

    public ClientReader(BufferedReader in, OnAccepted onAccepted) {
        this.in = in;
        this.onAccepted = onAccepted;
    }

    public void registerDownload(String transferId, FileOutputStream fos) {
        downloads.put(transferId, fos);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Map<String,Object> msg = Json.parse(line);
                String type = (String) msg.get("type");
                if (type == null) continue;

                switch (type) {
                    case "INFO" -> {
                        String status = (String) msg.get("status");
                        String transferId = (String) msg.get("transferId");
                        System.out.println("INFO: " + status + (transferId != null ? " ("+transferId+")" : ""));
                        if ("ACCEPTED".equals(status) && transferId != null) onAccepted.onAccepted(transferId);
                    }
                    case "FILE_OFFER_PROMPT" -> {
                        System.out.println("filtilbud fra " + msg.get("from") + ": " + msg.get("filename")
                                + " (" + msg.get("size") + " bytes), transferId=" + msg.get("transferId"));
                        System.out.println("brug: /accept " + msg.get("transferId") + " <sti> eller /reject " + msg.get("transferId"));
                    }
                    case "FILE_CHUNK" -> {
                        String transferId = (String) msg.get("transferId");
                        String data = (String) msg.get("data");
                        FileOutputStream fos = downloads.get(transferId);
                        if (fos != null && data != null) {
                            byte[] bytes = Base64.getDecoder().decode(data);
                            fos.write(bytes);
                        }
                    }
                    case "FILE_END" -> {
                        String transferId = (String) msg.get("transferId");
                        FileOutputStream fos = downloads.remove(transferId);
                        if (fos != null) {
                            fos.flush();
                            fos.close();
                            System.out.println("download færdig for " + transferId);
                        }
                    }
                    case "ERROR" -> System.out.println("fejl: " + msg.get("message"));
                    default -> { /* ignorér andre */ }
                }
            }
        } catch (IOException e) {
            System.out.println("forbindelsen lukket.");
        }
    }
}
