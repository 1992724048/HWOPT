package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import library.dll.BeardifierNative;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Beardifier.class)
public abstract class BeardifierMixin {

	@Shadow
	@Final
	protected List<Beardifier.Rigid> pieces;

	@Shadow
	@Final
	protected List<JigsawJunction> junctions;

	@Final
	@Shadow
	private BoundingBox affectedBox;

	@Unique
	private void hwopt$batchCompute(double[] output, DensityFunction.ContextProvider contextProvider) {
		if (affectedBox == null) {
			java.util.Arrays.fill(output, 0.0);
			return;
		}
		if (!(contextProvider instanceof NoiseChunk chunk)) {
			return;
		}
		NoiseChunkAccessor acc = (NoiseChunkAccessor) chunk;
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int cellSX = acc.cellStartBlockX();
		int cellSY = acc.cellStartBlockY();
		int cellSZ = acc.cellStartBlockZ();

		int bMinX = affectedBox.minX();
		int bMinY = affectedBox.minY();
		int bMinZ = affectedBox.minZ();
		int bMaxX = affectedBox.maxX();
		int bMaxY = affectedBox.maxY();
		int bMaxZ = affectedBox.maxZ();

		int numPieces = pieces.size();
		int[] piecesBox = new int[numPieces * 6];
		int[] piecesMeta = new int[numPieces * 2];
		for (int i = 0; i < numPieces; i++) {
			Beardifier.Rigid p = pieces.get(i);
			BoundingBox box = p.box();
			piecesBox[i * 6] = box.minX();
			piecesBox[i * 6 + 1] = box.minY();
			piecesBox[i * 6 + 2] = box.minZ();
			piecesBox[i * 6 + 3] = box.maxX();
			piecesBox[i * 6 + 4] = box.maxY();
			piecesBox[i * 6 + 5] = box.maxZ();
			piecesMeta[i * 2] = p.terrainAdjustment().ordinal();
			piecesMeta[i * 2 + 1] = p.groundLevelDelta();
		}

		int numJunctions = junctions.size();
		int[] junctionsData = new int[numJunctions * 4];
		for (int i = 0; i < numJunctions; i++) {
			JigsawJunction j = junctions.get(i);
			junctionsData[i * 4] = j.getSourceX();
			junctionsData[i * 4 + 1] = j.getSourceGroundY();
			junctionsData[i * 4 + 2] = j.getSourceZ();
			junctionsData[i * 4 + 3] = j.getSourceGroundY();
		}
		
		BeardifierNative.instance().batch_beardifier(
			cellSX, cellSY, cellSZ, cellW, cellH,
			piecesBox, piecesMeta, junctionsData,
			bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ,
			output
		);
	}

	@Inject(method = "fillArray", at = @At("HEAD"), cancellable = true)
	private void hwopt$fillArray(double[] output, DensityFunction.ContextProvider contextProvider, CallbackInfo ci) {
		if (contextProvider instanceof NoiseChunk) {
			hwopt$batchCompute(output, contextProvider);
			ci.cancel();
		}
	}
}
