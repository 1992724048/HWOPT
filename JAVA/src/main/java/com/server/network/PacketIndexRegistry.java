package com.server.network;

import com.server.network.indextype.NamespaceIndexManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class PacketIndexRegistry {
    public static final PacketIndexRegistry INSTANCE = new PacketIndexRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger("hwopt");
    private volatile boolean initialized;

    public int getIndex(Identifier id) {
        initOnce();
        if (!NamespaceIndexManager.contains(id)) return -1;
        int[] idx = NamespaceIndexManager.getCheckedIndex(id);
        return (idx[0] << 16) | idx[1];
    }

    public Identifier getIdentifier(int packed) {
        initOnce();
        return NamespaceIndexManager.getIdentifier(packed >>> 16, packed & 0xFFFF);
    }

    public static int getModIdx(int packed) { return packed >>> 16; }
    public static int getPktIdx(int packed) { return packed & 0xFFFF; }
    public static int pack(int modIdx, int pktIdx) { return (modIdx << 16) | pktIdx; }

    public synchronized void initOnce() {
        if (initialized) return;
        initialized = true;
        if (NamespaceIndexManager.ready()) return;
        try {
            List<Identifier> allIds = new ArrayList<>();
            Class<?> nr = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry");
            var regField = nr.getDeclaredField("PAYLOAD_REGISTRATIONS");
            regField.setAccessible(true);
            java.util.Map<?, java.util.Map<Identifier, ?>> regs =
                (java.util.Map<?, java.util.Map<Identifier, ?>>) regField.get(null);
            for (var map : regs.values()) allIds.addAll(map.keySet());
            NamespaceIndexManager.init(allIds);
            LOGGER.info("PacketIndexRegistry: {} payloads", NamespaceIndexManager.getPayloadCount());
        } catch (Exception e) {
            LOGGER.error("Failed to init PacketIndexRegistry", e);
        }
    }
}
