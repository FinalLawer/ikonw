package com.example.iknow.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import com.example.iknow.ModBlockEntities;

/**
 * 无限物品源（原石）：每 tick 向 6 面邻近的容器大量输出原石；本身也是容器，可无限抽取原石。
 */
public class InfiniteItemSourceBlockEntity extends BlockEntity implements IInfiniteSource {

    public static final int OUTPUT_AMOUNT = Integer.MAX_VALUE;

    public InfiniteItemSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_ITEM_SOURCE.get(), pos, state);
    }

    @Override
    public void infiniteServerTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // 顶到代码上限：每次向邻近容器最多塞 Integer.MAX_VALUE（目标容器按其容量接收）
        ItemStack push = new ItemStack(Items.COBBLESTONE, Integer.MAX_VALUE);
        boolean any = false;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            BlockEntity te = level.getBlockEntity(targetPos);
            if (te == null) {
                continue;
            }
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            for (int slot = 0; slot < target.getSlots(); slot++) {
                ItemStack leftover = target.insertItem(slot, push.copy(), false);
                if (leftover.getCount() < push.getCount()) {
                    any = true;
                }
            }
        }
        if (any) {
            this.setChanged();
        }
    }

    /** 无限物品处理器：永远能抽出原石，永不耗尽（作为容器的能力） */
    public IItemHandler getInfiniteHandler() {
        return infiniteHandler;
    }

    private final IItemHandler infiniteHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return new ItemStack(Items.COBBLESTONE, Integer.MAX_VALUE);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 无限源不接受外部物品
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return new ItemStack(Items.COBBLESTONE, Math.min(amount, Integer.MAX_VALUE));
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
}
