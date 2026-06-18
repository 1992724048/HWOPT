package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface AABBNative {
	class Holder {
		static final AABBNative INSTANCE = FFMFactory.load(AABBNative.class);
	}
	
	static AABBNative instance() {
		return AABBNative.Holder.INSTANCE;
	}
}
