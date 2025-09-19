package org.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Transfer {
    public final String id;
    public final String from;
    public final String to;
    public final String filename;
    public final long size;
    public final AtomicBoolean accepted = new AtomicBoolean(false);
    public final AtomicLong transferredBytes = new AtomicLong(0);

    public Transfer(String id, String from, String to, String filename, long size) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.filename = filename;
        this.size = size;
    }
}
