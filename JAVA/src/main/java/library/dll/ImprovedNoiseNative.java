package library.dll;

import nativecode.dll.*;

@LibraryImport(dll = "hwopt.dll", structSize = 272)
public interface ImprovedNoiseNative {
	ImprovedNoiseNative NATIVE = FFMFactory.load(ImprovedNoiseNative.class);
	
	@Field(offset = 8)
	double xo();
	@Field(offset = 8)
	void xo(double v);
	@Field(offset = 16)
	double yo();
	@Field(offset = 16)
	void yo(double v);
	@Field(offset = 24)
	double zo();
	@Field(offset = 24)
	void zo(double v);
	@Field(offset = 32)
	byte[] p();
	@Field(offset = 32)
	void p(byte[] v);
	
	@Static
	@Name("ImprovedNoise::_create")
	void getInterpolatedState(short[] array, int arraySize, int sizeX, int sizeY, int sizeZ);
}
