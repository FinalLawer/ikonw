package com.example.iknow;

import com.example.iknow.client.InfinityOutputterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单类型注册表 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, IknowMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<InfinityOutputterMenu>> INFINITY_OUTPUTTER;

    static {
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            // InfinityOutputterMenu.TYPE 由 AE2 的 MenuTypeBuilder.buildUnregistered 构建
            //（其内部已调用 MenuOpener.addOpener），这里仅将同一 MenuType 注册到本 mod 命名空间。
            INFINITY_OUTPUTTER = MENU_TYPES.register("infinite_item_outputter", () -> InfinityOutputterMenu.TYPE);
        } else {
            INFINITY_OUTPUTTER = null;
        }
    }

    private ModMenuTypes() {
    }
}
