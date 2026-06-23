package com.server.network.indextype;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ConnectionIndexTable {
    private final Object2IntOpenHashMap<Identifier> typeToIndex = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<Identifier> namespaceToIndex = new Object2IntOpenHashMap<>();
    private final List<String> namespaces;
    private final List<Identifier> types;

    public ConnectionIndexTable(List<Identifier> types) {
        this.types = types;
        Object2IntOpenHashMap<String> nsMap = new Object2IntOpenHashMap<>();
        int nsIdx = 0, typeIdx = 0;
        for (Identifier id : types) {
            if (!nsMap.containsKey(id.getNamespace())) {
                nsMap.put(id.getNamespace(), nsIdx++);
            }
            namespaceToIndex.put(id, nsMap.getInt(id.getNamespace()));
            typeToIndex.put(id, typeIdx++);
        }
        this.namespaces = nsMap.keySet().stream().sorted().toList();
    }

    public boolean contains(Identifier type) {
        return typeToIndex.containsKey(type);
    }

    public int getNamespaceIndex(Identifier type) {
        return namespaceToIndex.getInt(type);
    }

    public int getTypeIndex(Identifier type) {
        return typeToIndex.getInt(type);
    }

    public Identifier getType(int namespaceIndex, int typeIndex) {
        if (typeIndex >= 0 && typeIndex < types.size()) {
            Identifier id = types.get(typeIndex);
            if (getNamespaceIndex(id) == namespaceIndex) return id;
        }
        return null;
    }

    public int size() {
        return types.size();
    }
}
