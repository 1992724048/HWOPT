package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 8)
public interface BlockIdRegistryNative {
    BlockIdRegistryNative NATIVE = FFMFactory.load(BlockIdRegistryNative.class);
    
    @Static
    @Name("BlockIdRegistry::registry")
    void registry_block(String name, short id);
}
