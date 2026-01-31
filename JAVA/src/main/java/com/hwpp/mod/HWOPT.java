package com.hwpp.mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HWOPT.MODID)
public class HWOPT {
    public static final String MODID = "hwopt";
    public static final Logger LOGGER = LogUtils.getLogger();
    // 创建一个延迟注册器来存放所有区块，这些区块都将注册在“hwopt”命名空间下
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HWOPT.MODID);
    // 创建一个延迟注册来存放所有将注册在“hwopt”命名空间下的物品
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HWOPT.MODID);
    // 创建一个延迟注册器来存放所有 CreativeModeTabs，这些都将注册在“hwopt”命名空间下
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HWOPT.MODID);

    // 创建一个新块，ID“hwopt：example_block”，结合命名空间和路径
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = HWOPT.BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    // 创建一个新的 BlockItem ，ID 为“hwopt：example_block”，结合命名空间和路径
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = HWOPT.ITEMS.registerSimpleBlockItem("example_block", HWOPT.EXAMPLE_BLOCK);

    // 创建一个新食物，ID“hwopt：example_id”，营养1，饱和度2
    public static final DeferredItem<Item> EXAMPLE_ITEM = HWOPT.ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2.0f).build()));

    // 为示例物品创建一个带有“hwopt：example_tab”的创意标签页，放置在战斗标签页之后
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = HWOPT.CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hwopt")) // 你的CreativeModeTab标题的语言键
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> HWOPT.EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HWOPT.EXAMPLE_ITEM.get()); // 把示例项目添加到标签页中。对于你自己的账单，这种方式比活动更受欢迎
            }).build());
    
    public static long seed;

    // mod类的构造子是加载mod时运行的第一个代码。
    // FML 会识别一些参数类型，比如 IEventBus 或 ModContainer，并自动传递。
    public HWOPT(final IEventBus modEventBus, final ModContainer modContainer) {

        // 注册 commonSetup 方法进行 modloading
        modEventBus.addListener(this::commonSetup);

        // 将延迟寄存器注册到mod事件总线，这样块才能被注册
        BLOCKS.register(modEventBus);
        // 将延迟寄存器注册到mod事件总线，这样项目才能被注册
        ITEMS.register(modEventBus);
        // 将延迟注册器注册到模组事件总线，这样标签页才会被注册
        CREATIVE_MODE_TABS.register(modEventBus);

        // 报名参加我们感兴趣的服务器及其他游戏活动。
        // 注意，当且仅当我们希望*该*类（HWOPT）直接响应事件时，这是必要的。
        // 如果该类中没有@SubscribeEvent注释函数，如下面的 onServerStarting（） 那样，请不要添加这行。
        NeoForge.EVENT_BUS.register(this);

        // 将该物品注册到创意标签页
        modEventBus.addListener(this::addCreative);

        // 注册我们模组的ModConfigSpec，这样FML才能帮我们创建并加载配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event) {
        HWOPT.LOGGER.info("server starting");
    }

    @SubscribeEvent
    public void onServerStopping(final ServerStoppingEvent event) {
        HWOPT.LOGGER.info("server stopping");
    }

    @SubscribeEvent
    public void onServerStopped(final ServerStoppedEvent event) {
        HWOPT.LOGGER.info("server stopped");
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        HWOPT.LOGGER.info("server started");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player joined: {}", player.getName());
        }
    }
    
    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            seed = level.getSeed();
            System.out.println("Seed is: " + seed);
        }
    }

}
