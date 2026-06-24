package com.server.network;

import com.server.network.aggregation.PacketAggregationPacket;
import com.server.network.indextype.NamespaceIndexManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

@EventBusSubscriber(modid = "hwopt")
public class PacketIndexRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("hwopt");
    private static volatile boolean initialized = false;

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("hwopt");
        net.neoforged.neoforge.network.handling.IPayloadHandler<PacketAggregationPacket> h = (p, c) -> p.handle(c);
        registrar.playBidirectional(PacketAggregationPacket.TYPE, PacketAggregationPacket.CODEC, h, h);

        if (initialized) return;
        initialized = true;
        NamespaceIndexManager.init(new ArrayList<>());
        LOGGER.info("PacketIndexRegistry: {} indexed (vanilla payloads), aggregation registered",
            NamespaceIndexManager.getPayloadCount());
    }
}
