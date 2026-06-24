package com.server.network.debugEntries;

import com.hwpp.mod.Config;
import com.server.network.aggregation.AggregationManager;
import com.server.network.indextype.NamespaceIndexManager;
import com.server.network.util.NetworkTrafficTracker;
import com.server.network.zstd.ZstdHelper;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class NetworkStatsEntry implements DebugScreenEntry {
	@Override
	public void display(DebugScreenDisplayer displayer, Level level, LevelChunk chunk, LevelChunk chunk2) {
		String nsStatus = NamespaceIndexManager.ready() ? "§a" + NamespaceIndexManager.getPayloadCount() + " payloads" : "§cnot initialized";
		displayer.addLine("§7Index:§r " + nsStatus + "  §7Zstd:§r " + (ZstdHelper.isAvailable() ? "§a✓" : "§c✗"));
		
		long totalSent = NetworkTrafficTracker.getTotalSent();
		long totalRecv = NetworkTrafficTracker.getTotalReceived();
		long rawSent = NetworkTrafficTracker.getTotalSentRaw();
		long rawRecv = NetworkTrafficTracker.getTotalReceivedRaw();
		long pktsSent = NetworkTrafficTracker.getPacketsSent();
		long pktsRecv = NetworkTrafficTracker.getPacketsReceived();
		double sendSpeed = NetworkTrafficTracker.getSendSpeed();
		double recvSpeed = NetworkTrafficTracker.getRecvSpeed();
		
		displayer.addLine("§7↑ Sent:§r " + NetworkTrafficTracker.formatBytes(totalSent) + "/" + NetworkTrafficTracker.formatBytes(rawSent) + " (" + pktsSent + " pkts)  §a" + NetworkTrafficTracker.formatBytes(sendSpeed) + "/s");
		displayer.addLine("§7↓ Recv:§r " + NetworkTrafficTracker.formatBytes(totalRecv) + "/" + NetworkTrafficTracker.formatBytes(rawRecv) + " (" + pktsRecv + " pkts)  §a" + NetworkTrafficTracker.formatBytes(recvSpeed) + "/s");
		
		double outRatio = NetworkTrafficTracker.getSentRatio();
		double inRatio = NetworkTrafficTracker.getReceivedRatio();
		displayer.addLine("§7Compression:§r ↑ " + (int) (outRatio * 100) + "%" + "  ↓ " + (int) (inRatio * 100) + "%");
		
		long aggr = AggregationManager.getAggregatedPacketCount();
		displayer.addLine("§7Aggregation:§r " + aggr + " pkts batched");
		
		if (Config.CONFIG.dccEnabled.get()) {
			displayer.addLine("§7DCC:§r dist " + Config.CONFIG.dccCacheDistance.get() + ", timeout " + Config.CONFIG.dccCacheTimeout.get() + "s" + ", buffer " + Config.CONFIG.dccCacheSizeLimit.get());
		}
	}
}
