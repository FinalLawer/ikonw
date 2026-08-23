package com.example.iknow;

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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.example.iknow.item.MultiToolItem;
import com.example.iknow.block.BasePlacerBlock;
import com.example.iknow.block.CleanWorldBlock;
import com.example.iknow.network.ModNetwork;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(IknowMod.MODID)
public class IknowMod {
    // Define mod id in a common place for everything to reference
        public static final String MODID = "iknow";
    // Directly reference a slf4j logger
        public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "iknow" namespace
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "iknow" namespace
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "iknow" namespace
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "iknow:example_block", combining the namespace and path
        public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "iknow:example_block", combining the namespace and path
        public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // 鍩哄湴閾鸿鍣細鍙抽敭鍚姩锛屾竻绌哄叾鍛ㄥ洿 17脳17 鍖哄潡骞堕摵璁鹃粍榛戝湴鏉?
        public static final DeferredBlock<BasePlacerBlock> BASE_PLACER = BLOCKS.register("base_placer",
            () -> new BasePlacerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F)));
    public static final DeferredItem<BlockItem> BASE_PLACER_ITEM = ITEMS.registerSimpleBlockItem("base_placer", BASE_PLACER);

    // 浣犳兂瑕佸共鍑€鐨勪笘鐣岋細鍙抽敭鎵撳紑 GUI锛屽彲绔嬪嵆/瀹氭椂娓呯悊鎺夎惤鐗?
        public static final DeferredBlock<CleanWorldBlock> CLEAN_WORLD = BLOCKS.register("clean_world",
            () -> new CleanWorldBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F)));
    public static final DeferredItem<BlockItem> CLEAN_WORLD_ITEM = ITEMS.registerSimpleBlockItem("clean_world", CLEAN_WORLD);

    // Creates a new food item with the id "iknow:example_id", nutrition 1 and saturation 2
        public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // 澶氬姛鑳藉伐鍏凤細闆嗘枾澶淬€侀晲瀛愩€侀攧澶淬€佸壀鍒€銆侀摬瀛愪簬涓€韬紝鎸変綇 R 鎵撳紑杞洏閫夋嫨鍔熻兘锛堝彲鍗曢€夋垨澶氶€夛級
        public static final DeferredItem<MultiToolItem> MULTI_TOOL = ITEMS.register("multi_tool",
            () -> new MultiToolItem(new Item.Properties()
                    .component(ModDataComponents.TOOL_MODES, ToolMode.DEFAULT_MASK)
                    .component(ModDataComponents.ENCHANT_MODE, MultiToolItem.ENCHANT_OFF)
                    .component(ModDataComponents.MINING_SPEED, 50)
                    .component(ModDataComponents.FLIGHT_SPEED, 50)
                    .component(ModDataComponents.BLOCK_REACH, MultiToolItem.DEFAULT_BLOCK_REACH)
                    .component(ModDataComponents.ATTACK_REACH, MultiToolItem.DEFAULT_ATTACK_REACH)));

    // Creates a creative tab with the id "iknow:example_tab" for the example item, that is placed after the combat tab
        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.iknow")) //The language key for the title of your CreativeModeTab
        .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
        output.accept(MULTI_TOOL.get());// Add the multi tool to the mod's own tab
                output.accept(BASE_PLACER_ITEM.get());// Add the base placer block to the mod's own tab
        output.accept(CLEAN_WORLD_ITEM.get());// Add the clean world block to the mod's own tab
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
        public IknowMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register data components and network payloads
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (IknowMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
        @EventBusSubscriber(modid = IknowMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
        LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}

