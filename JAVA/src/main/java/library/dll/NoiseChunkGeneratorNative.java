package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;

@LibraryImport(dll = "hwopt.dll", structSize = 8)
public interface NoiseBasedChunkGeneratorNative {
    NoiseBasedChunkGeneratorNative NATIVE = FFMFactory.load(NoiseBasedChunkGeneratorNative.class);
    
    
}
