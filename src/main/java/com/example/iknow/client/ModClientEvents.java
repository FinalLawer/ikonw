package com.example.iknow.client;

import com.example.iknow.IknowMod;
import com.example.iknow.ModMenuTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.menu.AEBaseMenu;

/**
 * 客户端 MOD 事件总线：注册按键映射与屏幕。
 */
@EventBusSubscriber(modid = IknowMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEvents {

    private ModClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientEvents.OPEN_WHEEL);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        if (ModMenuTypes.INFINITY_OUTPUTTER != null) {
            // 复用 AE2 压印器屏幕样式（完整复刻 AE 界面），通过 StyleManager 加载
            register(event, ModMenuTypes.INFINITY_OUTPUTTER.get(), InfinityOutputterScreen::new,
                    "/screens/inscriber.json");
        }
    }

    /**
     * 与 AE2 InitScreens 相同的注册方式：先从 StyleManager 加载屏幕样式文档，
     * 再交给屏幕构造器（其额外接受一个 ScreenStyle 参数）。
     */
    private static <M extends AEBaseMenu, U extends AEBaseScreen<M>> void register(RegisterMenuScreensEvent event,
            MenuType<M> type, StyledScreenFactory<M, U> factory, String stylePath) {
        event.<M, U>register(type, (menu, playerInv, title) -> {
            var style = StyleManager.loadStyleDoc(stylePath);
            return factory.create(menu, playerInv, title, style);
        });
    }

    /** 适配 AE2 屏幕构造器签名：额外带一个 ScreenStyle 参数。 */
    @FunctionalInterface
    private interface StyledScreenFactory<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> {
        U create(T t, Inventory pi, Component title, ScreenStyle style);
    }
}
