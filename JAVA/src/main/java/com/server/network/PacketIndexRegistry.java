package com.server.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PacketIndexRegistry {
	public static final PacketIndexRegistry INSTANCE = new PacketIndexRegistry();
	private static final Logger LOGGER = LoggerFactory.getLogger("hwopt");
	private static final int MAX_MODS = 32767;
	private static final int MAX_PACKETS_PER_MOD = 32767;
	
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final Object lock = new Object();
	
	// mod namespace -> mod index (read-only after init)
	private volatile Map<String, Integer> modToIndex = Collections.emptyMap();
	// per-mod: packet path -> packet index (read-only after init)
	private volatile List<Map<String, Integer>> modPackets = Collections.emptyList();
	// packed -> Identifier (read-only after init)
	private volatile Map<Integer, Identifier> packedToId = Collections.emptyMap();
	
	public int getIndex(Identifier id) {
		initOnce();
		int modIdx = modToIndex.getOrDefault(id.getNamespace(), -1);
		if (modIdx < 0) return -1;
		Map<String, Integer> pktMap = modPackets.get(modIdx);
		if (pktMap == null) return -1;
		int pktIdx = pktMap.getOrDefault(id.getPath(), -1);
		if (pktIdx < 0) return -1;
		return (modIdx << 16) | pktIdx;
	}
	
	public Identifier getIdentifier(int packed) {
		initOnce();
		return packedToId.get(packed);
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
	
	public void initOnce() {
		if (initialized.get()) return;
		synchronized (lock) {
			if (initialized.get()) return;
			initializeFromNetworkRegistry();
		}
	}
	
	/**
	 * Called from within $1's @Redirect handlers to also register vanilla
	 * payload types captured in the anonymous class's val$idToType field.
	 */
	public void captureVanillaPayloads(Object $this) {
		if (initialized.get()) return;
		synchronized (lock) {
			if (initialized.get()) return;
			try {
				Field f = $this.getClass().getDeclaredField("val$idToType");
				f.setAccessible(true);
				Map<?, ?> idToType = (Map<?, ?>) f.get($this);
				Set<Identifier> ids = new HashSet<>();
				for (Object key : idToType.keySet()) {
					if (key instanceof Identifier id) ids.add(id);
				}
				if (!ids.isEmpty()) {
					initializeFromNetworkRegistry();
					// If already initialized above (by another thread), skip.
					// Otherwise, merge vanilla + NeoForge payloads.
					if (initialized.get()) return;
					initializeFromBoth(ids);
				}
			} catch (Exception e) {
				LOGGER.error("Failed to capture vanilla payloads from $1", e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initializeFromNetworkRegistry() {
		try {
			Class<?> nr = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry");
			
			Field regField = nr.getDeclaredField("PAYLOAD_REGISTRATIONS");
			regField.setAccessible(true);
			Map<ConnectionProtocol, Map<Identifier, ?>> regs = (Map<ConnectionProtocol, Map<Identifier, ?>>) regField.get(null);
			
			Set<Identifier> allIds = new LinkedHashSet<>();
			for (Map<Identifier, ?> map : regs.values()) allIds.addAll(map.keySet());
			
			Field builtinField = nr.getDeclaredField("BUILTIN_PAYLOADS");
			builtinField.setAccessible(true);
			Map<Identifier, ?> builtins = (Map<Identifier, ?>) builtinField.get(null);
			allIds.addAll(builtins.keySet());
			
			buildIndex(allIds);
		} catch (Exception e) {
			LOGGER.error("Failed to init PacketIndexRegistry", e);
		}
	}
	
	private void initializeFromBoth(Set<Identifier> vanillaIds) {
		Set<Identifier> merged = new LinkedHashSet<>();
		merged.addAll(vanillaIds);
		// Also try to include NeoForge payloads
		try {
			Class<?> nr = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry");
			Field regField = nr.getDeclaredField("PAYLOAD_REGISTRATIONS");
			regField.setAccessible(true);
			Map<ConnectionProtocol, Map<Identifier, ?>> regs = (Map<ConnectionProtocol, Map<Identifier, ?>>) regField.get(null);
			for (Map<Identifier, ?> map : regs.values()) merged.addAll(map.keySet());
			
			Field builtinField = nr.getDeclaredField("BUILTIN_PAYLOADS");
			builtinField.setAccessible(true);
			Map<Identifier, ?> builtins = (Map<Identifier, ?>) builtinField.get(null);
			merged.addAll(builtins.keySet());
		} catch (Exception e) {
			LOGGER.warn("Could not add NeoForge payloads alongside vanilla", e);
		}
		buildIndex(merged);
	}
	
	private void buildIndex(Set<Identifier> allIds) {
		Map<String, Set<String>> byMod = new TreeMap<>();
		for (Identifier id : allIds) {
			byMod.computeIfAbsent(id.getNamespace(), k -> new TreeSet<>()).add(id.getPath());
		}
		
		Map<String, Integer> modIdxMap = new HashMap<>();
		List<Map<String, Integer>> pktMaps = new ArrayList<>();
		Map<Integer, Identifier> packedMap = new HashMap<>();
		
		int modIdx = 0;
		for (Map.Entry<String, Set<String>> entry : byMod.entrySet()) {
			if (modIdx >= MAX_MODS) break;
			String mod = entry.getKey();
			modIdxMap.put(mod, modIdx);
			
			Map<String, Integer> pktMap = new HashMap<>();
			int pktIdx = 0;
			for (String path : entry.getValue()) {
				if (pktIdx >= MAX_PACKETS_PER_MOD) break;
				pktMap.put(path, pktIdx);
				packedMap.put(pack(modIdx, pktIdx), Identifier.fromNamespaceAndPath(mod, path));
				pktIdx++;
			}
			pktMaps.add(pktMap);
			modIdx++;
		}
		
		this.modToIndex = Collections.unmodifiableMap(modIdxMap);
		this.modPackets = Collections.unmodifiableList(pktMaps);
		this.packedToId = Collections.unmodifiableMap(packedMap);
		initialized.set(true);
		
		LOGGER.info("PacketIndexRegistry: {} mods, {} payloads", modToIndex.size(), packedMap.size());
	}
}
