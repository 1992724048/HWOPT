package com.server.world.misc;

public class PrecomputedDensity {
	private static double[] data;
	private static int minY, sizeX, sizeY, sizeZ;
	
	public static void set(double[] d, int my, int sx, int sy, int sz) {
		data = d;
		minY = my;
		sizeX = sx;
		sizeY = sy;
		sizeZ = sz;
	}
	
	public static double get(int x, int y, int z) {
		if (data == null) return Double.NaN;
		int idx = ((y - minY) * sizeZ + z) * sizeX + x;
		return idx >= 0 && idx < data.length ? data[idx] : Double.NaN;
	}
	
	public static boolean isActive() {
		return data != null;
	}
	
	public static void clear() {
		data = null;
	}
}
