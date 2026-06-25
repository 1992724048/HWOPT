package com.server.misc.biome.mixin;

import com.mojang.datafixers.util.Pair;
import com.server.misc.biome.BiomeEnvelopeSelector;
import com.server.misc.biome.structure.StructureChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	
	@Inject(method = "getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;IIIZJLnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;)Lcom/mojang/datafixers/util/Pair;", at = @At("HEAD"), cancellable = true)
	private static void biomespy$getNearestGeneratedStructure(Set<Holder<Structure>> structures, LevelReader level, StructureManager structureManager, int chunkOriginX, int chunkOriginZ, int radius, boolean createReference, long seed, RandomSpreadStructurePlacement config, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
		StructureCheck structureCheck = ((StructureManagerAccessor) structureManager).getStructureCheck();
		StructureCheckAccessor checkAccessor = (StructureCheckAccessor) structureCheck;
		BiomeSource biomeSource = checkAccessor.getBiomeSource();
		if (!(biomeSource instanceof MultiNoiseBiomeSource)) return;
		cir.cancel();
		
		int spacing = config.spacing();
		
		var parameters = ((MultiNoiseBiomeSourceAccessor) biomeSource).invokeParameters();
		Map<Holder<Structure>, BiomeEnvelopeSelector> structureBiome = new HashMap<>();
		for (Holder<Structure> structure : structures) {
			structureBiome.put(structure, new BiomeEnvelopeSelector(structure.value().biomes().stream().toList(), parameters));
		}
		
		for (int j = -radius; j <= radius; j++) {
			boolean flag = j == -radius || j == radius;
			for (int k = -radius; k <= radius; k++) {
				boolean flag1 = k == -radius || k == radius;
				if (!flag && !flag1) continue;
				
				int regionX = chunkOriginX + spacing * j;
				int regionZ = chunkOriginZ + spacing * k;
				ChunkPos chunkpos = config.getPotentialStructureChunk(seed, regionX, regionZ);
				
				Pair<BlockPos, Holder<Structure>> pair = StructureChecker.getStructureGeneratingAt(structureBiome, level, structureManager, createReference, config, chunkpos, parameters);
				if (pair == null) continue;
				
				cir.setReturnValue(pair);
				return;
			}
		}
		
		cir.setReturnValue(null);
	}
}
