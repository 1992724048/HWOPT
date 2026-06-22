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
	
	// mod namespace -> mod index
	private final Map<String, Integer> modToIndex = new HashMap<>();
	// mod index -> mod namespace
	private final List<String> indexToMod = new ArrayList<>();
	// per-mod: packet path -> packet index
	private final List<Map<String, Integer>> modPackets = new ArrayList<>();
	// per-mod: packet index -> packet path
	private final List<List<String>> modPacketsReverse = new ArrayList<>();
	
	// packed identifier -> Identifier (reverse lookup)
	private volatile Map<Integer, Identifier> packedToId = Collections.emptyMap();
	
	public int getIndex(Identifier id) {
		ensureInitialized();
		String namespace = id.getNamespace();
		String path = id.getPath();
		Integer modIdx;
		Integer pktIdx;
		synchronized (lock) {
			modIdx = modToIndex.get(namespace);
			if (modIdx == null) return -1;
			Map<String, Integer> packets = modPackets.get(modIdx);
			pktIdx = packets.get(path);
			if (pktIdx == null) return -1;
		}
		return (modIdx << 16) | pktIdx;
	}
	
	public Identifier getIdentifier(int packed) {
		ensureInitialized();
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
	
	public void ensureInitialized() {
		if (!initialized.get()) {
			synchronized (lock) {
				if (!initialized.get()) {
					initializeFromNetworkRegistry();
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initializeFromNetworkRegistry() {
		if (initialized.get()) return;
		try {
			Class<?> networkRegistry = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry");
			Field regField = networkRegistry.getDeclaredField("PAYLOAD_REGISTRATIONS");
			regField.setAccessible(true);
			Map<ConnectionProtocol, Map<Identifier, ?>> regs = (Map<ConnectionProtocol, Map<Identifier, ?>>) regField.get(null);
			
			Set<Identifier> allIds = new LinkedHashSet<>();
			for (Map<Identifier, ?> map : regs.values()) {
				allIds.addAll(map.keySet());
			}
			
			// Also include builtin payloads
			Field builtinField = networkRegistry.getDeclaredField("BUILTIN_PAYLOADS");
			builtinField.setAccessible(true);
			Map<Identifier, ?> builtins = (Map<Identifier, ?>) builtinField.get(null);
			allIds.addAll(builtins.keySet());
			
			buildIndex(allIds);
		} catch (Exception e) {
			LOGGER.error("Failed to initialize PacketIndexRegistry from NetworkRegistry", e);
		}
	}
	
	private void buildIndex(Set<Identifier> allIds) {
		// Group by namespace (mod id)
		Map<String, Set<String>> byMod = new TreeMap<>();
		for (Identifier id : allIds) {
			byMod.computeIfAbsent(id.getNamespace(), k -> new TreeSet<>()).add(id.getPath());
		}
		
		Map<Integer, Identifier> packedMap = new HashMap<>();
		
		int modIdx = 0;
		for (Map.Entry<String, Set<String>> entry : byMod.entrySet()) {
			if (modIdx >= MAX_MODS) {
				LOGGER.warn("Too many mods (> {}) for packet index registry, truncating", MAX_MODS);
				break;
			}
			String mod = entry.getKey();
			modToIndex.put(mod, modIdx);
			indexToMod.add(mod);
			
			Map<String, Integer> pktMap = new HashMap<>();
			List<String> pktRev = new ArrayList<>();
			int pktIdx = 0;
			for (String path : entry.getValue()) {
				if (pktIdx >= MAX_PACKETS_PER_MOD) {
					LOGGER.warn("Too many packets for mod {} (> {}), truncating", mod, MAX_PACKETS_PER_MOD);
					break;
				}
				pktMap.put(path, pktIdx);
				pktRev.add(path);
				int packed = pack(modIdx, pktIdx);
				Identifier id = Identifier.fromNamespaceAndPath(mod, path);
				packedMap.put(packed, id);
				pktIdx++;
			}
			
			modPackets.add(pktMap);
			modPacketsReverse.add(pktRev);
			modIdx++;
		}
		
		this.packedToId = Collections.unmodifiableMap(packedMap);
		initialized.set(true);
		
		LOGGER.info("PacketIndexRegistry initialized with {} mods, {} total payloads ({} packed)", modToIndex.size(), allIds.size(), packedMap.size());
	}
}
