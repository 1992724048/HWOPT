package com.hwpp.mod;

import com.hwpp.util.BlockIdRegistry;
import com.hwpp.mod.ModConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HWOPT.MODID, dist = Dist.CLIENT)
// 你可以使用 EventBusSubscriber 自动注册所有标注为 @SubscribeEvent 的类中的所有静态方法。
@EventBusSubscriber(modid = HWOPT.MODID, value = Dist.CLIENT)
public class HWOPTClient {
    public HWOPTClient(final ModContainer container) {
        Config.load();
        container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent) -> ModConfigScreen.create(parent));
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        BlockIdRegistry.init();
    }
}
