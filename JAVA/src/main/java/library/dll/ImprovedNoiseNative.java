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
	
	@Static
	@Name("ImprovedNoise::_create")
	ImprovedNoiseNative create(double xo, double yo, double zo, byte[] array, int len);
	
	@Name("ImprovedNoise::_destroy")
	void destroy();
	
	@Name("ImprovedNoise::noise")
	double noise(double x, double y, double z, double y_scale, double y_fudge);
	
	@Static
	@Name("ImprovedNoise::grad_dot")
	double grad_dot(int hash, double x, double y, double z);
	
	@Name("ImprovedNoise::sample_and_lerperm")
	double sample_and_lerperm(int x, int y, int z, double xr, double yr, double zr, double yr_original);
	
	@Name("ImprovedNoise::sample_with_derivative")
	double sample_with_derivative(int x, int y, int z, double xr, double yr, double zr, double[] derivative_out);
	
	@Name("ImprovedNoise::noise_with_derivative")
	double noise_with_derivative(double x, double y, double z, double[] derivative_out);
	
	@Name("ImprovedNoise::perm")
	int perm(int x);
}
