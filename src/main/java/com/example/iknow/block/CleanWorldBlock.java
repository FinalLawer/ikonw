package com.example.iknow.block;

import com.example.iknow.CleanWorldHandler;
import com.example.iknow.network.CleanWorldOpenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 你想要干净的世界：右键打开 GUI，可确定清理掉落物或定时清理掉落物。
 */
public class CleanWorldBlock extends Block {

    public CleanWorldBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openGui(level, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        openGui(level, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void openGui(Level level, Player player) {
        if (level.isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            CleanWorldHandler.State state = CleanWorldHandler.state(sp);
            int remaining = 0;
            int duration = 0;
            boolean running = false;
            boolean paused = false;
            if (state != null) {
                remaining = state.remainingSeconds();
                duration = state.durationSeconds();
                running = state.running;
                paused = state.paused;
            }
            PacketDistributor.sendToPlayer(sp, new CleanWorldOpenPayload(remaining, duration, running, paused));
        }
    }
}
