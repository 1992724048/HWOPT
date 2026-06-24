package com.hwpp.mod;

import com.hwpp.util.BlockIdRegistry;
import com.server.network.aggregation.AggregationManager;
import com.server.network.debugEntries.NetworkStatsEntry;
import com.server.network.util.NetworkTrafficTracker;
import com.server.render.entityculling.EntityCullingMod;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HWOPT.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HWOPT.MODID, value = Dist.CLIENT)
public class HWOPTClient {
	public HWOPTClient(final ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent) -> ModConfigScreen.create(parent));
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
			EntityCullingMod.getInstance().clientTick();
			NetworkTrafficTracker.tick();
		});
	}
	
	@SubscribeEvent
	static void onClientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			EntityCullingMod.getInstance();
			EntityCullingMod.getInstance().loadConfig();
		});
		event.enqueueWork(AggregationManager::init);
		// PacketIndexRegistry initializes via @SubscribeEvent on RegisterPayloadHandlersEvent
		BlockIdRegistry.init();
	}
	
	@SubscribeEvent
	static void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
		event.register(Identifier.fromNamespaceAndPath("hwopt", "network_stats"), new NetworkStatsEntry());
	}
}
