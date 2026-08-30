package com.example.iknow;

import com.example.iknow.item.IknowToolItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 爆破连锁：挖掘矿物方块（{@code neoforge:ore}）时，连锁挖掉以该方块为中心、
 * 半径 {@link #RADIUS} 范围内的全部矿物方块。
 * <p>
 * 掉落与附魔走多功能工具的既有逻辑：尊重精准采集/时运，并按拾取模式路由到物品栏 / AE。
 * </p>
 */
@EventBusSubscriber(modid = IknowMod.MODID)
public final class BlastChainHandler {

    /** 连锁半径（方块数，切比雪夫距离，即 2*RADIUS+1 的立方体） */
    public static final int RADIUS = 100;

    private BlastChainHandler() {
    }

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
        // 仅在开启爆破连锁时生效
        if (!IknowToolItem.blastChainEnabled(tool)) {
            return;
        }
        BlockPos origin = event.getPos();
        BlockState originState = event.getState();
        // 只有矿物方块才触发连锁
        if (!originState.is(Tags.Blocks.ORES)) {
            return;
        }
        // 收集半径范围内所有矿物（不含原点，原点已由本次破坏处理）
        List<BlockPos> targets = collectOrePositions(level, origin);
        if (targets.isEmpty()) {
            return;
        }
        // 依次破坏并路由掉落
        for (BlockPos pos : targets) {
            if (pos.equals(origin)) {
                continue;
            }
            mineOre(level, pos, player, tool);
        }
    }

    /** 收集以 origin 为中心、半径 RADIUS 立方体内的所有矿物坐标（不含原点） */
    private static List<BlockPos> collectOrePositions(ServerLevel level, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    // 跳过原点（已破坏）
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    /** 破坏单个矿物，计算掉落（尊重采集/时运），并按工具拾取模式路由。 */
    private static void mineOre(ServerLevel level, BlockPos pos, Player player, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.is(Tags.Blocks.ORES)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, tool);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        // 路由：优先按破坏模式（物品栏/AE），否则用磁吸；都没有则正常掉落在地
        PickupMode mode = pickRoutingMode(tool);
        int exp = state.getBlock().getExpDrop(state, level, pos, be, player, tool);
        if (exp > 0) {
            player.giveExperiencePoints(exp);
        }
        routeDrops(level, pos, player, tool, drops, mode);
    }

    /** 决定连锁破坏的掉落去向：破坏进物品栏/AE > 磁吸 > 普通掉落 */
    private static PickupMode pickRoutingMode(ItemStack tool) {
        PickupMode brk = IknowToolItem.breakMode(tool);
        if (brk == PickupMode.BREAK_INVENTORY || brk == PickupMode.BREAK_AE) {
            return brk;
        }
        PickupMode magnet = IknowToolItem.magnetMode(tool);
        if (magnet == PickupMode.MAGNET || magnet == PickupMode.MAGNET_AE) {
            return magnet;
        }
        return PickupMode.NONE;
    }

    /** 把掉落物路由到背包（满则丢出）或 AE 网络；否则在地面生成掉落实体。 */
    private static void routeDrops(ServerLevel level, BlockPos pos, Player player, ItemStack tool,
                                   List<ItemStack> drops, PickupMode mode) {
        boolean toAe = mode == PickupMode.BREAK_AE || mode == PickupMode.MAGNET_AE;
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
            // 磁吸 / 破坏进背包：尝试收进背包
            if (mode != PickupMode.NONE && !drop.isEmpty()) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            } else if (!drop.isEmpty()) {
                // 普通掉落：在破坏位置生成实体
                Block.popResource(level, pos, drop);
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
