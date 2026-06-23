package com.hwpp.mod;

import com.mojang.logging.LogUtils;
import com.server.entity.util.EntityPushSystem;
import com.server.world.util.ChunkGenStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(HWOPT.MODID)
public class HWOPT {
	public static final String MODID = "hwopt";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	public HWOPT(final IEventBus modEventBus, final ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::addCreative);
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}
	
	private void commonSetup(final FMLCommonSetupEvent event) {
	}
	
	private void addCreative(final BuildCreativeModeTabContentsEvent event) {
	}
	
	@SubscribeEvent
	public void onWorldLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level) {
			if (level.dimension() == Level.OVERWORLD) {
				ChunkGenStats.reset();
			}
		}
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent.Post event) {
		for (ServerLevel level : event.getServer().getAllLevels()) {
			EntityPushSystem.tick(level);
		}
	}
	
}
