package com.hwpp.mod;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HWOPT.MODID, dist = Dist.CLIENT)
// 你可以使用 EventBusSubscriber 自动注册所有标注为 @SubscribeEvent 的类中的所有静态方法。
@EventBusSubscriber(modid = HWOPT.MODID, value = Dist.CLIENT)
public class HWOPTClient {
    public HWOPTClient(final ModContainer container) {
        // 允许NeoForge为该模组的配置创建配置界面。
        // 配置界面可以通过进入模组界面>点击你的模组>再点击配置来访问。
        // 别忘了在en_us.json文件中添加配置文件的转换选项。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
    }
}
