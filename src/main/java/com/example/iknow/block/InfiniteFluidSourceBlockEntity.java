package com.example.iknow.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.example.iknow.IknowMod;
import com.example.iknow.ModBlockEntities;

/**
 * 无限流体源（熔岩/水）：每 tick 向 6 面邻近的流体容器大量输出；本身也是容器，可无限抽取流体。
 */
public class InfiniteFluidSourceBlockEntity extends BlockEntity implements IInfiniteSource {

    public static final int OUTPUT_AMOUNT = Integer.MAX_VALUE;

    public InfiniteFluidSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_FLUID_SOURCE.get(), pos, state);
    }

    /** 根据方块决定输出哪种流体 */
    private Fluid getFluid() {
        if (getBlockState().getBlock() == IknowMod.INFINITE_LAVA.get()) {
            return Fluids.LAVA;
        }
        if (getBlockState().getBlock() == IknowMod.INFINITE_WATER.get()) {
            return Fluids.WATER;
        }
        return Fluids.WATER;
    }

    @Override
    public void infiniteServerTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        Fluid fluid = getFluid();
        boolean any = false;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            BlockEntity te = level.getBlockEntity(targetPos);
            if (te == null) {
                continue;
            }
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            int filled = target.fill(new FluidStack(fluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                any = true;
            }
        }
        if (any) {
            this.setChanged();
        }
    }

    public IFluidHandler getInfiniteHandler() {
        return infiniteHandler;
    }

    private final IFluidHandler infiniteHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return new FluidStack(getFluid(), getTankCapacity(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid() == getFluid();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // 无限源不接受外部流体
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != getFluid()) {
                return FluidStack.EMPTY;
            }
            return new FluidStack(getFluid(), Math.min(resource.getAmount(), Integer.MAX_VALUE));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(getFluid(), Math.min(maxDrain, Integer.MAX_VALUE));
        }
    };
}
