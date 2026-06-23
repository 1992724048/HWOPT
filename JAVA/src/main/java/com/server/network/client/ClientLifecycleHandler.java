package com.server.network.client;

import com.server.network.aggregation.AggregationManager;
import com.server.network.indextype.NamespaceIndexManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientLifecycleHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        AggregationManager.init();
    }
}
