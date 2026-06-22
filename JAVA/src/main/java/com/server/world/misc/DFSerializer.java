package com.server.world.misc;

import library.dll.BlendedNoiseNative;
import library.dll.NormalNoiseNative;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DFSerializer {
	
	static final int CONSTANT = 0, ADD = 1, MUL = 2, MIN = 3, MAX = 4;
	static final int TRANSFORM = 5, RANGE_CHOICE = 6, BLENDED_NOISE = 7, NOISE = 8, Y_GRADIENT = 9;
	
	public static record SerializedTree(double[] nodes, long[] bnPtrs, long[] noisePtrs) {
	}
	
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> MH_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Field>> FH_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, Field> PTR_CACHE = new ConcurrentHashMap<>();
	
	private static Method cachedMethod(Class<?> cls, String name) throws NoSuchMethodException {
		var m = MH_CACHE.get(cls);
		if (m != null) {
			var r = m.get(name);
			if (r != null) return r;
		}
		var r = cls.getMethod(name);
		r.setAccessible(true);
		MH_CACHE.computeIfAbsent(cls, k -> new ConcurrentHashMap<>()).put(name, r);
		return r;
	}
	
	private static Field cachedField(Class<?> cls, String name) throws NoSuchFieldException {
		var m = FH_CACHE.get(cls);
		if (m != null) {
			var r = m.get(name);
			if (r != null) return r;
		}
		var r = cls.getDeclaredField(name);
		r.setAccessible(true);
		FH_CACHE.computeIfAbsent(cls, k -> new ConcurrentHashMap<>()).put(name, r);
		return r;
	}
	
	private static long segmentAddress(Object proxy) {
		var pc = proxy.getClass();
		var f = PTR_CACHE.get(pc);
		if (f == null) {
			try {
				f = pc.getDeclaredField("ptr");
				f.setAccessible(true);
				PTR_CACHE.put(pc, f);
			} catch (Exception e) {
				return 0;
			}
		}
		try {
			return ((MemorySegment) f.get(proxy)).address();
		} catch (Exception e) {
			return 0;
		}
	}
	
	public static SerializedTree serialize(DensityFunction root) throws Exception {
		List<Double> nodeList = new ArrayList<>();
		List<Long> bnList = new ArrayList<>();
		List<Long> noiseList = new ArrayList<>();
		serializeNode(root, nodeList, bnList, noiseList);
		int nc = nodeList.size();
		double[] nodes = new double[nc];
		for (int i = 0; i < nc; i++) nodes[i] = nodeList.get(i);
		int bc = bnList.size();
		long[] bn = new long[bc];
		for (int i = 0; i < bc; i++) bn[i] = bnList.get(i);
		int nnc = noiseList.size();
		long[] nn = new long[nnc];
		for (int i = 0; i < nnc; i++) nn[i] = noiseList.get(i);
		return new SerializedTree(nodes, bn, nn);
	}
	
	private static int serializeNode(Object df, List<Double> out, List<Long> bnPtrs, List<Long> noisePtrs) {
		Class<?> cls = df.getClass();
		String name = cls.getName();
		
		if (name.contains("$Noise")) {
			NormalNoiseNative nativePtr = null;
			double xzScale = 0;
			try {
				Object holderOrNoise = cachedMethod(cls, "noise").invoke(df);
				Object actualNoise = null;
				Class<?> hc = holderOrNoise.getClass();
				for (String mn : new String[]{"noise", "value"}) {
					try {
						actualNoise = cachedMethod(hc, mn).invoke(holderOrNoise);
						break;
					} catch (NoSuchMethodException e) {
					}
				}
				if (actualNoise == null) actualNoise = holderOrNoise;
				try {
					nativePtr = (NormalNoiseNative) NORMAL_NOISE_NATIVE.get(actualNoise);
				} catch (IllegalAccessException e) {
				}
				xzScale = (double) cachedMethod(cls, "xzScale").invoke(df);
			} catch (Exception e) {
			}
			if (nativePtr != null) {
				int nId = noisePtrs.size();
				noisePtrs.add(segmentAddress(nativePtr));
				out.add((double) NOISE);
				out.add(-1.0);
				out.add(-1.0);
				out.add((double) nId);
				out.add(xzScale);
				return out.size() / 5 - 1;
			}
		}
		
		if (name.contains("$Mapped") || name.contains("$MulOrAdd") || name.contains("$Clamp")) {
			try {
				Field inputF = cachedField(cls, "input");
				int c0 = serializeNode(inputF.get(df), out, bnPtrs, noisePtrs);
				int op = TRANSFORM;
				try {
					Field st = cachedField(cls, "specificType");
					op = ((Enum<?>) st.get(df)).ordinal();
					if (op > 3) op = TRANSFORM;
				} catch (NoSuchFieldException e) {
					try {
						Field t = cachedField(cls, "type");
						op = ((Enum<?>) t.get(df)).ordinal();
						if (op > 3) op = TRANSFORM;
					} catch (NoSuchFieldException e2) {
					}
				}
				out.add((double) op);
				out.add((double) c0);
				out.add(-1.0);
				out.add(1.0);
				out.add(0.0);
				return out.size() / 5 - 1;
			} catch (Exception e) {
			}
		}
		
		if (name.contains("$Ap2")) {
			try {
				Field a1 = cachedField(cls, "argument1");
				Field a2 = cachedField(cls, "argument2");
				Field t = cachedField(cls, "type");
				int ordinal = ((Enum<?>) t.get(df)).ordinal();
				int nodeType = ordinal == 0 ? ADD : ordinal == 1 ? MUL : ordinal == 2 ? MIN : MAX;
				int c0 = serializeNode(a1.get(df), out, bnPtrs, noisePtrs);
				int c1 = serializeNode(a2.get(df), out, bnPtrs, noisePtrs);
				out.add((double) nodeType);
				out.add((double) c0);
				out.add((double) c1);
				out.add(0.0);
				out.add(0.0);
				return out.size() / 5 - 1;
			} catch (Exception e) {
			}
		}
		
		if (name.contains("$RangeChoice") || name.contains("$IntervalSelect")) {
			try {
				Field inputF = cachedField(cls, "input");
				Field minF = cachedField(cls, "minInclusive");
				Field maxF = cachedField(cls, "maxExclusive");
				Field choiceF = cachedField(cls, "whenInRange");
				Field defaultF = cachedField(cls, "whenOutOfRange");
				double min = (double) minF.get(df);
				double max = (double) maxF.get(df);
				int c0 = serializeNode(inputF.get(df), out, bnPtrs, noisePtrs);
				int c1 = serializeNode(choiceF.get(df), out, bnPtrs, noisePtrs);
				int c2 = serializeNode(defaultF.get(df), out, bnPtrs, noisePtrs);
				out.add((double) RANGE_CHOICE);
				out.add((double) c0);
				out.add((double) c1);
				out.add(min);
				out.add(max);
				out.add((double) c2);
				out.add(-1.0);
				out.add(-1.0);
				out.add(0.0);
				out.add(0.0);
				return out.size() / 5 - 2;
			} catch (Exception e) {
			}
		}
		
		if (name.contains("CacheOnce") || name.contains("CacheAllInCell") || name.contains("FlatCache") || name.contains("Cache2D")) {
			try {
				Field fillerF = null;
				for (String fn : new String[]{"filler", "noiseFiller", "function"}) {
					try {
						fillerF = cachedField(cls, fn);
						break;
					} catch (NoSuchFieldException e) {
					}
				}
				if (fillerF == null) fillerF = cls.getDeclaredFields()[0];
				fillerF.setAccessible(true);
				return serializeNode(fillerF.get(df), out, bnPtrs, noisePtrs);
			} catch (Exception e) {
			}
		}
		
		if (df instanceof BlendedNoise) {
			try {
				BlendedNoiseNative nativePtr = (BlendedNoiseNative) BLENDED_NOISE_NATIVE.get(df);
				if (nativePtr != null) {
					int bnId = bnPtrs.size();
					bnPtrs.add(segmentAddress(nativePtr));
					out.add((double) BLENDED_NOISE);
					out.add(-1.0);
					out.add(-1.0);
					out.add((double) bnId);
					out.add(0.0);
					return out.size() / 5 - 1;
				}
			} catch (Exception e) {
			}
		}
		
		if (name.contains("$Constant")) {
			try {
				double v = (double) cachedMethod(cls, "value").invoke(df);
				out.add((double) CONSTANT);
				out.add(-1.0);
				out.add(-1.0);
				out.add(v);
				out.add(0.0);
				return out.size() / 5 - 1;
			} catch (Exception e) {
			}
		}
		
		if (name.contains("YClampedGradient")) {
			try {
				double fY = ((Number) cachedMethod(cls, "fromY").invoke(df)).doubleValue();
				double tY = ((Number) cachedMethod(cls, "toY").invoke(df)).doubleValue();
				double fV = ((Number) cachedMethod(cls, "fromValue").invoke(df)).doubleValue();
				double tV = ((Number) cachedMethod(cls, "toValue").invoke(df)).doubleValue();
				out.add((double) Y_GRADIENT);
				out.add(fY);
				out.add(tY);
				out.add(fV);
				out.add(tV);
				return out.size() / 5 - 1;
			} catch (Exception e) {
			}
		}
		
		out.add((double) CONSTANT);
		out.add(-1.0);
		out.add(-1.0);
		out.add(0.0);
		out.add(0.0);
		return out.size() / 5 - 1;
	}
	
	private static final Field BLENDED_NOISE_NATIVE;
	private static final Field NORMAL_NOISE_NATIVE;
	
	static {
		try {
			BLENDED_NOISE_NATIVE = BlendedNoise.class.getDeclaredField("hwopt$nativePtr");
			BLENDED_NOISE_NATIVE.setAccessible(true);
			NORMAL_NOISE_NATIVE = NormalNoise.class.getDeclaredField("hwopt$nativePtr");
			NORMAL_NOISE_NATIVE.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
