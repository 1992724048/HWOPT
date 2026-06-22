package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface DensityFunctionTree extends AutoCloseable {
	class Holder {
		static final DensityFunctionTree INSTANCE = FFMFactory.load(DensityFunctionTree.class);
	}
	
	static DensityFunctionTree instance() {
		return DensityFunctionTree.Holder.INSTANCE;
	}
	
	@Static
	@Name("DensityFunctionTree::compute_densities_batch")
	void compute_densities_batch(double[] nodes, long[] bnPtrs, long[] noisePtrs, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, double[] output);
}
