package com.example.iknow.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 无限源方块基类：物品源头 / 流体源头共用。主动向 6 面输出，且自身作为无限容器。
 */
public class InfiniteSourceBlock extends Block implements EntityBlock {

    private final Supplier<BlockEntityType<?>> beType;

    public InfiniteSourceBlock(Properties props, Supplier<BlockEntityType<?>> beType) {
        super(props);
        this.beType = beType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return beType.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != beType.get()) {
            return null;
        }
        return (lvl, p, st, be) -> ((IInfiniteSource) be).infiniteServerTick();
    }
}
