package com.server.network.util;

import java.util.concurrent.atomic.AtomicLong;

public class NetworkTrafficTracker {
    private static final AtomicLong bytesSent = new AtomicLong();
    private static final AtomicLong bytesReceived = new AtomicLong();
    private static final AtomicLong packetsSent = new AtomicLong();
    private static final AtomicLong packetsReceived = new AtomicLong();
    private static long lastSampleTime = System.nanoTime();
    private static long lastBytesSent;
    private static long lastBytesReceived;
    private static volatile double sendSpeed; // bytes/sec
    private static volatile double recvSpeed; // bytes/sec

    private static final AtomicLong bytesSentRaw = new AtomicLong();
    private static final AtomicLong bytesReceivedRaw = new AtomicLong();

    public static void recordSent(int bytes, int rawBytes) {
        bytesSent.addAndGet(bytes);
        bytesSentRaw.addAndGet(rawBytes);
        packetsSent.incrementAndGet();
    }

    public static void recordReceived(int bytes, int rawBytes) {
        bytesReceived.addAndGet(bytes);
        bytesReceivedRaw.addAndGet(rawBytes);
        packetsReceived.incrementAndGet();
    }

    public static void recordSent(int bytes) { recordSent(bytes, bytes); }
    public static void recordReceived(int bytes) { recordReceived(bytes, bytes); }

    public static void tick() {
        long now = System.nanoTime();
        long elapsed = now - lastSampleTime;
        if (elapsed > 1_000_000_000L) { // 1 second
            double sec = elapsed / 1_000_000_000.0;
            long curSent = bytesSent.get();
            long curRecv = bytesReceived.get();
            sendSpeed = (curSent - lastBytesSent) / sec;
            recvSpeed = (curRecv - lastBytesReceived) / sec;
            lastBytesSent = curSent;
            lastBytesReceived = curRecv;
            lastSampleTime = now;
        }
    }

    public static long getTotalSent() { return bytesSent.get(); }
    public static long getTotalSentRaw() { return bytesSentRaw.get(); }
    public static long getTotalReceived() { return bytesReceived.get(); }
    public static long getTotalReceivedRaw() { return bytesReceivedRaw.get(); }

    public static double getSentRatio() {
        long s = bytesSent.get(), r = bytesSentRaw.get();
        return r > 0 ? (double) s / r : 1.0;
    }
    public static double getReceivedRatio() {
        long s = bytesReceived.get(), r = bytesReceivedRaw.get();
        return r > 0 ? (double) s / r : 1.0;
    }
    public static long getPacketsSent() { return packetsSent.get(); }
    public static long getPacketsReceived() { return packetsReceived.get(); }
    public static double getSendSpeed() { return sendSpeed; }
    public static double getRecvSpeed() { return recvSpeed; }

    public static String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024);
        return String.format("%.1f MB", bytes / (1024 * 1024));
    }
}
