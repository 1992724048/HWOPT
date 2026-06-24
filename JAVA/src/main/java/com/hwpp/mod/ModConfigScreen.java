package com.hwpp.mod;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModConfigScreen {
	private static final Config C = Config.CONFIG;
	
	public static Screen create(Screen parent) {
		return YetAnotherConfigLib.createBuilder()
				.title(Component.translatable("hwopt.config.title"))
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("hwopt.config.category.rendering"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.entityCulling"))
								.collapsed(false)
								.option(boolOpt("hwopt.config.tickCulling", "hwopt.config.tickCulling.desc",
										true, C.tickCulling, C.tickCulling::set))
								.option(boolOpt("hwopt.config.solidLeaves",
										"hwopt.config.solidLeaves.desc",
										false, C.solidLeaves, C.solidLeaves::set))
								.option(intOpt("hwopt.config.sleepDelay",
										"hwopt.config.sleepDelay.desc",
										10, C.sleepDelay, C.sleepDelay::set, 0, 1000, 10))
								.option(intOpt("hwopt.config.hitboxLimit",
										"hwopt.config.hitboxLimit.desc",
										50, C.hitboxLimit, C.hitboxLimit::set, 1, 500, 1))
								.option(intOpt("hwopt.config.captureRate",
										"hwopt.config.captureRate.desc",
										5, C.captureRate, C.captureRate::set, 1, 100, 1))
								.option(boolOpt("hwopt.config.skipEntityCulling",
										"hwopt.config.skipEntityCulling.desc",
										false, C.skipEntityCulling, C.skipEntityCulling::set))
								.option(boolOpt("hwopt.config.skipBlockEntityCulling",
										"hwopt.config.skipBlockEntityCulling.desc",
										false, C.skipBlockEntityCulling, C.skipBlockEntityCulling::set))
								.option(boolOpt("hwopt.config.forceDisplayCulling",
										"hwopt.config.forceDisplayCulling.desc",
										false, C.forceDisplayCulling, C.forceDisplayCulling::set))
								.option(boolOpt("hwopt.config.debugMode",
										"hwopt.config.debugMode.desc",
										false, C.debugMode, C.debugMode::set))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.particle"))
								.collapsed(false)
								.option(boolOpt("hwopt.config.asyncParticleTick",
										"hwopt.config.asyncParticleTick.desc",
										true, C.asyncParticleTick, C.asyncParticleTick::set))
								.option(boolOpt("hwopt.config.particleLightCache",
										"hwopt.config.particleLightCache.desc",
										true, C.particleLightCache, C.particleLightCache::set))
								.option(boolOpt("hwopt.config.removeIfMissedTick",
										"hwopt.config.removeIfMissedTick.desc",
										true, C.removeIfMissedTick, C.removeIfMissedTick::set))
								.option(intOpt("hwopt.config.particleLimit",
										"hwopt.config.particleLimit.desc",
										16384, C.particleLimit, C.particleLimit::set, 4096, 262144, 1024))
								.option(boolOpt("hwopt.config.parallelQueueRemoval",
										"hwopt.config.parallelQueueRemoval.desc",
										false, C.parallelQueueRemoval, C.parallelQueueRemoval::set))
								.option(boolOpt("hwopt.config.parallelQueueEviction",
										"hwopt.config.parallelQueueEviction.desc",
										false, C.parallelQueueEviction, C.parallelQueueEviction::set))
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("hwopt.config.category.network"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.aggregation"))
								.collapsed(false)
								.option(intOpt("hwopt.config.netFlushMs",
										"hwopt.config.netFlushMs.desc",
										20, C.netFlushMs, C.netFlushMs::set, 5, 100, 5))
								.option(intOpt("hwopt.config.netMaxBytes",
										"hwopt.config.netMaxBytes.desc",
										262144, C.netMaxBytes, C.netMaxBytes::set, 16384, 1048576, 16384))
								.option(intOpt("hwopt.config.netMaxCount",
										"hwopt.config.netMaxCount.desc",
										50, C.netMaxCount, C.netMaxCount::set, 5, 200, 5))
								.option(intOpt("hwopt.config.netMaxPacketSize",
										"hwopt.config.netMaxPacketSize.desc",
										4194304, C.netMaxPacketSize, C.netMaxPacketSize::set, 262144, 16777216, 262144))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.dcc"))
								.collapsed(false)
								.option(boolOpt("hwopt.config.dccEnabled",
										"hwopt.config.dccEnabled.desc",
										true, C.dccEnabled, C.dccEnabled::set))
								.option(intOpt("hwopt.config.dccCacheTimeout",
										"hwopt.config.dccCacheTimeout.desc",
										60, C.dccCacheTimeout, C.dccCacheTimeout::set, 10, 300, 10))
								.option(intOpt("hwopt.config.dccCacheDistance",
										"hwopt.config.dccCacheDistance.desc",
										8, C.dccCacheDistance, C.dccCacheDistance::set, 2, 32, 2))
								.option(intOpt("hwopt.config.dccCacheSizeLimit",
										"hwopt.config.dccCacheSizeLimit.desc",
										1200, C.dccCacheSizeLimit, C.dccCacheSizeLimit::set, 100, 5000, 100))
								.option(intOpt("hwopt.config.dccBufferDistance",
										"hwopt.config.dccBufferDistance.desc",
										8, C.dccBufferDistance, C.dccBufferDistance::set, 2, 32, 2))
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("hwopt.config.category.world"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.world"))
								.option(boolOpt("hwopt.configuration.world.spawnAtVillage",
										"hwopt.configuration.world.spawnAtVillage.tooltip",
										false, C.spawnAtVillage, C.spawnAtVillage::set))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.mobDespawn"))
								.collapsed(false)
								.option(boolOpt("hwopt.config.mobDespawnEnabled",
										"hwopt.config.mobDespawnEnabled.desc",
										true, C.mobDespawnEnabled, C.mobDespawnEnabled::set))
								.build())
						.build())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("hwopt.config.category.debug"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.debug"))
								.option(boolOpt("hwopt.configuration.debug.logChunkGen",
										"hwopt.configuration.debug.logChunkGen.tooltip",
										false, C.logChunkGen, C.logChunkGen::set))
								.option(Option.<Integer>createBuilder()
										.name(Component.translatable("hwopt.configuration.debug.logChunkGenInterval"))
										.description(OptionDescription.of(Component.translatable("hwopt.configuration.debug.logChunkGenInterval.tooltip")))
										.binding(0, C.logChunkGenInterval, C.logChunkGenInterval::set)
										.controller(IntegerFieldControllerBuilder::create)
										.build())
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("hwopt.config.group.logging"))
								.collapsed(false)
								.option(boolOpt("hwopt.config.asyncLoggerEnabled",
										"hwopt.config.asyncLoggerEnabled.desc",
										true, C.asyncLoggerEnabled, C.asyncLoggerEnabled::set))
								.option(intOpt("hwopt.config.asyncLoggerRingBufferSize",
										"hwopt.config.asyncLoggerRingBufferSize.desc",
										0, C.asyncLoggerRingBufferSize, C.asyncLoggerRingBufferSize::set, -1, 262144, 1024))
								.option(dropdownOpt("hwopt.config.asyncLoggerWaitStrategy",
										"hwopt.config.asyncLoggerWaitStrategy.desc",
										"", C.asyncLoggerWaitStrategy, C.asyncLoggerWaitStrategy::set,
										List.of("", "sleep", "yield", "busyspin", "block")))
								.option(dropdownOpt("hwopt.config.asyncLoggerSynchronizeEnqueueWhenQueueFull",
										"hwopt.config.asyncLoggerSynchronizeEnqueueWhenQueueFull.desc",
										"", C.asyncLoggerSynchronizeEnqueueWhenQueueFull, C.asyncLoggerSynchronizeEnqueueWhenQueueFull::set,
										List.of("", "true", "false")))
								.option(dropdownOpt("hwopt.config.asyncLoggerFormatMsgAsync",
										"hwopt.config.asyncLoggerFormatMsgAsync.desc",
										"", C.asyncLoggerFormatMsgAsync, C.asyncLoggerFormatMsgAsync::set,
										List.of("", "true", "false")))
								.option(dropdownOpt("hwopt.config.asyncLoggerAsyncQueueFullPolicy",
										"hwopt.config.asyncLoggerAsyncQueueFullPolicy.desc",
										"", C.asyncLoggerAsyncQueueFullPolicy, C.asyncLoggerAsyncQueueFullPolicy::set,
										List.of("", "Discard", "Block")))
								.option(dropdownOpt("hwopt.config.asyncLoggerDiscardThreshold",
										"hwopt.config.asyncLoggerDiscardThreshold.desc",
										"", C.asyncLoggerDiscardThreshold, C.asyncLoggerDiscardThreshold::set,
										List.of("", "0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0")))
								.option(boolOpt("hwopt.config.asyncLoggerWrapSysOutSysErr",
										"hwopt.config.asyncLoggerWrapSysOutSysErr.desc",
										false, C.asyncLoggerWrapSysOutSysErr, C.asyncLoggerWrapSysOutSysErr::set))
								.option(boolOpt("hwopt.config.asyncLoggerTestPerformance",
										"hwopt.config.asyncLoggerTestPerformance.desc",
										false, C.asyncLoggerTestPerformance, C.asyncLoggerTestPerformance::set))
								.option(boolOpt("hwopt.config.asyncLoggerUseColors",
										"hwopt.config.asyncLoggerUseColors.desc",
										true, C.asyncLoggerUseColors, C.asyncLoggerUseColors::set))
								.build())
						.build())
				.save(Config.SPEC::save)
				.build()
				.generateScreen(parent);
	}
	
	private static Option<Boolean> boolOpt(String key, String descKey, boolean def,
	                                       java.util.function.Supplier<Boolean> getter,
	                                       java.util.function.Consumer<Boolean> setter) {
		var b = Option.<Boolean>createBuilder()
				.name(Component.translatable(key))
				.binding(def, getter, setter)
				.controller(BooleanControllerBuilder::create);
		if (descKey != null) b.description(OptionDescription.of(Component.translatable(descKey)));
		return b.build();
	}
	
	private static Option<Integer> intOpt(String key, String descKey, int def,
	                                      java.util.function.Supplier<Integer> getter,
	                                      java.util.function.Consumer<Integer> setter,
	                                      int min, int max, int step) {
		var b = Option.<Integer>createBuilder()
				.name(Component.translatable(key))
				.binding(def, getter, setter)
				.controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(step));
		if (descKey != null) b.description(OptionDescription.of(Component.translatable(descKey)));
		return b.build();
	}
	
	private static Option<String> dropdownOpt(String key, String descKey, String def,
	                                          java.util.function.Supplier<String> getter,
	                                          java.util.function.Consumer<String> setter,
	                                          List<String> values) {
		var b = Option.<String>createBuilder()
				.name(Component.translatable(key))
				.binding(def, getter, setter)
				.controller(o -> DropdownStringControllerBuilder.create(o).values(values));
		if (descKey != null) b.description(OptionDescription.of(Component.translatable(descKey)));
		return b.build();
	}
}
