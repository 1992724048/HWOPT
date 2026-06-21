package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface BeardifierNative {
	class Holder {
		static final BeardifierNative INSTANCE = FFMFactory.load(BeardifierNative.class);
	}
	
	static BeardifierNative instance() {
		return BeardifierNative.Holder.INSTANCE;
	}
	
	@Static
	@Name("Beardifier::batch_compute")
	void batch_beardifier(int cellStartBlockX, int cellStartBlockY, int cellStartBlockZ, int cellWidth, int cellHeight, int[] piecesBox, int[] piecesMeta, int[] junctionsData, int affectedMinX, int affectedMinY, int affectedMinZ, int affectedMaxX, int affectedMaxY, int affectedMaxZ, double[] output);
}
