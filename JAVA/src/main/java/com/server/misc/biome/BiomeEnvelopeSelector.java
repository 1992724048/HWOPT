package com.server.misc.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BiomeEnvelopeSelector {
	private final Map<Integer, BiomeEnvelope> envelopeMap = new HashMap<>();
	
	public BiomeEnvelopeSelector(Collection<Holder<Biome>> biomes, Climate.ParameterList<Holder<Biome>> parameters) {
		BiomeEnvelope combinedEnvelope = new BiomeEnvelope();
		combinedEnvelope.impossible = true;
		for (var pair : parameters.values()) {
			if (biomes.contains(pair.getSecond())) {
				combinedEnvelope.impossible = false;
				combinedEnvelope.add(pair.getFirst());
			}
		}
		this.envelopeMap.put(0, combinedEnvelope);
		for (Integer i : this.envelopeMap.keySet()) {
			BiomeEnvelope env = this.envelopeMap.get(i);
			if (!env.isValid()) this.envelopeMap.put(i, new BiomeEnvelope());
		}
	}
	
	public BiomeEnvelope getEnvelope(int qx, int qy, int qz) {
		return envelopeMap.get(0);
	}
}
