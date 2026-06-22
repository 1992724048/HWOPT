package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 0)
public interface CompressNative {
    CompressNative INSTANCE = FFMFactory.load(CompressNative.class);

    @Static
    @Name("Compress::compress")
    int compress(byte[] input, int inputLen, byte[] output, int outputLen);

    @Static
    @Name("Compress::decompress")
    int decompress(byte[] input, int inputLen, byte[] output, int outputLen);
}
