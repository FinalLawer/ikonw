package com.example.iknow.block;

import com.example.iknow.BaseBuildHandler;
import com.example.iknow.network.BasePlacerOpenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * 基地铺设器：放下后右键打开 GUI，通过"开始铺设"按钮启动。
 * 以其所在区块为中心，清空下方 17×17 区块（保留最底层基岩），并在其所在高度铺设
 * 黄黑地板（区块边界黄色混凝土、内部黑色混凝土）。
 */
public class BasePlacerBlock extends Block {

    public BasePlacerBlock(Properties properties) {
        super(properties);
    }

    // 空手右键
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openGui(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // 手持物品右键
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        openGui(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void openGui(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new BasePlacerOpenPayload(pos, BaseBuildHandler.isRunning(pos)));
        }
    }
}
