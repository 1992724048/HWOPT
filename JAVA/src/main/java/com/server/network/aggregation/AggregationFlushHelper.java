package com.server.network.aggregation;

public class AggregationFlushHelper {
    public static int getFlushPeriodInMilliseconds() { return 20; }
    public static int getFlushCountInSeconds() { return Math.max(1000 / getFlushPeriodInMilliseconds(), 1); }
    public static int getThresholdCount1s() { return 40; }
}
