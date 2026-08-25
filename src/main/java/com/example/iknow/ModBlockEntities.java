package com.example.iknow;

import com.example.iknow.block.InfiniteItemSourceBlockEntity;
import com.example.iknow.block.InfiniteFluidSourceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块实体类型注册表 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IknowMod.MODID);

    // 2 个无限源方块实体（物品源 + 流体源）
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteItemSourceBlockEntity>> INFINITE_ITEM_SOURCE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteFluidSourceBlockEntity>> INFINITE_FLUID_SOURCE;

    static {
        INFINITE_ITEM_SOURCE = BLOCK_ENTITIES.register("infinite_stone",
                () -> BlockEntityType.Builder.of(InfiniteItemSourceBlockEntity::new, IknowMod.INFINITE_STONE.get()).build(null));
        INFINITE_FLUID_SOURCE = BLOCK_ENTITIES.register("infinite_fluid_source",
                () -> BlockEntityType.Builder.of(InfiniteFluidSourceBlockEntity::new, IknowMod.INFINITE_LAVA.get(),
                        IknowMod.INFINITE_WATER.get()).build(null));
    }

    private ModBlockEntities() {
    }
}
