package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 8)
public interface NoiseChunkGeneratorNative {
    NoiseChunkGeneratorNative NATIVE = FFMFactory.load(NoiseChunkGeneratorNative.class);
    
    @Static
    @Name("NoiseChunkGenerator::get_interpolated_state")
    void getInterpolatedState(short[] id, int size, int x, int y, int z);
}
