package com.example.iknow;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.parts.IPartHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.reporting.AbstractTerminalPart;
import com.example.iknow.item.IknowToolItem;
import com.example.iknow.network.AeBindPayload;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 多功能工具的掉落收容与 AE 绑定：
 * - 潜行 + 右键无线访问点(WAP) → 绑定 AE 网络（避开 AE 扳手拆除：先于方块交互拦截）
 * - 破坏物品直入物品栏 / AE：取消默认掉落，改为计算掉落并直接收进物品栏或 ME 存储，
 *   不产生掉落物实体（学习自 useless_mod 的 MiningUtils.processBlockBreak）。
 */
@EventBusSubscriber(modid = IknowMod.MODID)
public final class PickupEvents {

    private PickupEvents() {
    }

    // ============ AE 绑定：潜行 + 右键无线访问点 ============
        @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Ae2Integration.isAe2Loaded()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = event.getItemStack();
        if (!(tool.getItem() instanceof IknowToolItem)) {
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof IWirelessAccessPoint)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            // 客户端：取消事件，AE2 的 WrenchHook（NORMAL）看到已取消即跳过，不再预判拆解（消除闪烁）
        event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            // 服务端绑定交给专门的 payload（客户端取消后不再走方块交互包）
        PacketDistributor.sendToServer(new AeBindPayload(event.getPos()));
            return;
        }
        // 服务端：取消事件阻止 AE 扳手拆除；实际绑定由 AeBindPayload 处理器完成
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    // ============ AE 终端（terminal part）：右键打开界面、不旋转；shift+右键拆解 ============
    // 工具仍保留扳手能力（可旋转其它方块 / 其它 AE 方块），但 AE 各种终端右键时不再自旋，
    // 而是打开终端界面。蹲下+右键仍走 AE 扳手拆解（WrenchHook + c:tools/wrench 标签）。
        @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickTerminal(PlayerInteractEvent.RightClickBlock event) {
        if (!Ae2Integration.isAe2Loaded()) {
            return;
        }
        ItemStack tool = event.getItemStack();
        if (!(tool.getItem() instanceof IknowToolItem)) {
            return;
        }
        // 蹲下+右键：保留 AE 扳手拆解，不拦截
        if (event.getEntity() != null && event.getEntity().isShiftKeyDown()) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof IPartHost host)) {
            return;
        }
        // 遍历 6 个朝向，找到 AE 终端 part
        for (Direction side : Direction.values()) {
            if (host.getPart(side) instanceof AbstractTerminalPart terminal) {
                // 取消该次右键（阻止 onUseWithoutItem 里的扳手自旋）
                event.setCanceled(true);
                event.setCancellationResult(event.getLevel().isClientSide()
                        ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
                // 在服务端打开终端界面
                if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {
                    MenuOpener.open(terminal.getMenuType(serverPlayer), serverPlayer,
                            MenuLocators.forPart((AEBasePart) terminal));
                }
                return;
            }
        }
    }

    // ============ 破坏物品直入物品栏 / AE ============

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack tool = heldIknowTool(player);
        if (tool == null) {
            return;
        }
        PickupMode mode = IknowToolItem.breakMode(tool);
        if (mode != PickupMode.BREAK_INVENTORY && mode != PickupMode.BREAK_AE) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (state.isAir()) {
            return;
        }
        // 预计算掉落（尊重精准采集/时运），移除方块，路由到背包或 AE
        BlockEntity be = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, tool);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        routeDrops(player, tool, drops, mode);
        int exp = state.getBlock().getExpDrop(state, level, pos, be, player, tool);
        if (exp > 0) {
            player.giveExperiencePoints(exp);
        }
        event.setCanceled(true);
    }

    /** 把掉落物路由到背包（满则丢出）或 AE 网络（剩余仍进背包） */
    private static void routeDrops(Player player, ItemStack tool, List<ItemStack> drops, PickupMode mode) {
        boolean toAe = mode == PickupMode.BREAK_AE;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            if (toAe && player instanceof ServerPlayer sp) {
                long inserted = Ae2Integration.insertIntoGrid(tool, sp, drop);
                if (inserted > 0) {
                    drop.shrink((int) inserted);
                }
            }
            if (!drop.isEmpty() && !player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
    }

    /** 手持的多功能工具（主手优先，否则副手） */
    private static ItemStack heldIknowTool(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IknowToolItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof IknowToolItem ? off : null;
    }
}
