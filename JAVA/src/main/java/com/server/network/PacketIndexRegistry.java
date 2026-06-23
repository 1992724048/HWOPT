package com.server.network;

import com.server.network.indextype.ConnectionIndexTable;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PacketIndexRegistry {
    public static final PacketIndexRegistry INSTANCE = new PacketIndexRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger("hwopt");
    private volatile ConnectionIndexTable table;
    private volatile boolean initialized;

    public int getIndex(Identifier id) {
        initOnce();
        if (table == null || !table.contains(id)) return -1;
        return (table.getNamespaceIndex(id) << 16) | table.getTypeIndex(id);
    }

    public Identifier getIdentifier(int packed) {
        initOnce();
        if (table == null) return null;
        return table.getType(packed >>> 16, packed & 0xFFFF);
    }

    public static int getModIdx(int packed) {
        return packed >>> 16;
    }

    public static int getPktIdx(int packed) {
        return packed & 0xFFFF;
    }

    public static int pack(int modIdx, int pktIdx) {
        return (modIdx << 16) | pktIdx;
    }

    public synchronized void initOnce() {
        if (initialized) return;
        initialized = true;
        try {
            List<Identifier> allIds = new ArrayList<>();
            Class<?> nr = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry");
            var regField = nr.getDeclaredField("PAYLOAD_REGISTRATIONS");
            regField.setAccessible(true);
            java.util.Map<?, java.util.Map<Identifier, ?>> regs =
                (java.util.Map<?, java.util.Map<Identifier, ?>>) regField.get(null);
            for (var map : regs.values()) allIds.addAll(map.keySet());

            var builtinField = nr.getDeclaredField("BUILTIN_PAYLOADS");
            builtinField.setAccessible(true);
            java.util.Map<Identifier, ?> builtins = (java.util.Map<Identifier, ?>) builtinField.get(null);
            allIds.addAll(builtins.keySet());

            table = new ConnectionIndexTable(allIds);
            LOGGER.info("PacketIndexRegistry: {} payloads, {} namespaces", allIds.size(), table.size());
        } catch (Exception e) {
            LOGGER.error("Failed to init PacketIndexRegistry", e);
        }
    }
}
