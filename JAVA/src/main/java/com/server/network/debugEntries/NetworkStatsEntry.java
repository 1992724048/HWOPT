package com.server.network.debugEntries;

import com.server.network.indextype.NamespaceIndexManager;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class NetworkStatsEntry implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, Level level, LevelChunk chunk, LevelChunk chunk2) {
        displayer.addPriorityLine("§6[NEBL]§r NamespaceIndex: "
            + NamespaceIndexManager.getPayloadCount() + " payloads, "
            + (NamespaceIndexManager.ready() ? "§aready" : "§cnot ready"));
    }
}
