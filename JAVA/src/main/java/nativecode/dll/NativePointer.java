package nativecode.dll;

import java.lang.foreign.MemorySegment;

public interface NativePointer {
    MemorySegment $ptr();
}
