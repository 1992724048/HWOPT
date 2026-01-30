package util;

public final class TimeCost {
    
    private long startTime;
    private long endTime;
    private boolean running;
    
    public static TimeCost startNew() {
        TimeCost t = new TimeCost();
        t.start();
        return t;
    }
    
    public void start() {
        running = true;
        startTime = System.nanoTime();
    }
    
    public long stop() {
        if (running) {
            endTime = System.nanoTime();
            running = false;
        }
        return getElapsedNanos();
    }
    
    public long getElapsedNanos() {
        return running ? (System.nanoTime() - startTime) : (endTime - startTime);
    }
    
    public String getFormatted() {
        return formatNanos(getElapsedNanos());
    }
    
    public static String formatNanos(long nanos) {
        if (nanos >= 1_000_000_000L) {
            return String.format("%.3f s", nanos / 1_000_000_000.0);
        }
        if (nanos >= 1_000_000L) {
            return String.format("%.3f ms", nanos / 1_000_000.0);
        }
        if (nanos >= 1_000L) {
            return String.format("%.3f us", nanos / 1_000.0);
        }
        return nanos + " ns";
    }
}
