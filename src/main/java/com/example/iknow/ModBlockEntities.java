package com.example.iknow;

import com.example.iknow.block.InfinityOutputterBlockEntity;
import com.example.iknow.block.InfiniteItemSourceBlockEntity;
import com.example.iknow.block.InfiniteFluidSourceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.atomic.AtomicReference;

/** 方块实体类型注册表 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IknowMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfinityOutputterBlockEntity>> INFINITE_OUTPUTTER;

    // 3 个无限源方块实体
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteItemSourceBlockEntity>> INFINITE_ITEM_SOURCE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteFluidSourceBlockEntity>> INFINITE_FLUID_SOURCE;

    static {
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            INFINITE_OUTPUTTER = BLOCK_ENTITIES.register("infinite_item_outputter", () -> {
                AtomicReference<BlockEntityType<InfinityOutputterBlockEntity>> typeHolder = new AtomicReference<>();
                BlockEntityType.BlockEntitySupplier<InfinityOutputterBlockEntity> supplier = (pos, state) ->
                        new InfinityOutputterBlockEntity(typeHolder.get(), pos, state);

                var block = IknowMod.INFINITE_ITEM_OUTPUTTER.get();
                var type = BlockEntityType.Builder.of(supplier, block).build(null);
                typeHolder.setPlain(type);

                // 绑定方块 <-> 方块实体，使 newBlockEntity()/getTicker() 生效；
                // 用方块服务端 ticker 泵送（不依赖网格激活/供电）
                block.setBlockEntity(InfinityOutputterBlockEntity.class, type, null,
                        InfinityOutputterBlockEntity::serverTick);
                return type;
            });
        } else {
            INFINITE_OUTPUTTER = null;
        }

        INFINITE_ITEM_SOURCE = BLOCK_ENTITIES.register("infinite_stone",
                () -> BlockEntityType.Builder.of(InfiniteItemSourceBlockEntity::new, IknowMod.INFINITE_STONE.get()).build(null));
        INFINITE_FLUID_SOURCE = BLOCK_ENTITIES.register("infinite_fluid_source",
                () -> BlockEntityType.Builder.of(InfiniteFluidSourceBlockEntity::new, IknowMod.INFINITE_LAVA.get(),
                        IknowMod.INFINITE_WATER.get()).build(null));
    }

    private ModBlockEntities() {
    }
}
