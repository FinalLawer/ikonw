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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 爆破连锁：挖掘矿物方块（{@code neoforge:ores}）时，连锁挖掉以该方块为中心、
 * 半径 {@link #RADIUS} 立方体内的全部矿物方块。
 * <p>
 * 只扫描"已加载区块"内的方块（避免强制加载大片未加载区块造成卡死），
 * 掉落与附魔尊重多功能工具的既有逻辑，并默认收进背包（不在地面生成大量实体）。
 * </p>
 */
@EventBusSubscriber(modid = IknowMod.MODID)
public final class BlastChainHandler {

    /** 连锁半径（切比雪夫距离，即 2*RADIUS+1 的立方体） */
    public static final int RADIUS = 32;

    private BlastChainHandler() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
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
        // 收集半径内所有矿物（不含原点，原点已由本次破坏处理）
        List<BlockPos> targets = collectOrePositions(level, origin);
        IknowMod.LOGGER.info("[BlastChain] origin={} breakMode={} magnet={} collected={}",
                origin.toShortString(), IknowToolItem.breakMode(tool),
                IknowToolItem.magnetMode(tool), targets.size());
        if (targets.isEmpty()) {
            return;
        }
        // 依次破坏并路由掉落（单个失败不影响其余连锁）
        for (BlockPos pos : targets) {
            if (pos.equals(origin)) {
                continue;
            }
            try {
                mineOre(level, pos, player, tool);
            } catch (Exception e) {
                IknowMod.LOGGER.warn("[BlastChain] failed at {}: {}", pos.toShortString(), e.toString());
            }
        }
    }

    /** 收集以 origin 为中心、半径 RADIUS 立方体内的所有矿物坐标（不含原点）。 */
    private static List<BlockPos> collectOrePositions(ServerLevel level, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        // 用可变 BlockPos 逐坐标扫描；只对"已加载区块"内的方块做 getBlockState，
        // 避免强制加载大片未加载区块造成卡死（radius 32 → 65^3 ≈ 27 万格，需严格限制）。
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    pos.set(ox + dx, oy + dy, oz + dz);
                    // 仅当该坐标所在区块已加载时才读取方块状态（避免强制加载区块）
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    if (level.getBlockState(pos).is(Tags.Blocks.ORES)) {
                        result.add(pos.immutable());
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
        // 路由：优先按破坏模式（物品栏/AE），否则用磁吸；都没有则收进背包（避免原地大量实体）
        PickupMode mode = pickRoutingMode(tool);
        int exp = state.getBlock().getExpDrop(state, level, pos, be, player, tool);
        if (exp > 0) {
            player.giveExperiencePoints(exp);
        }
        routeDrops(level, pos, player, tool, drops, mode);
    }

    /** 决定连锁破坏的掉落去向：破坏进物品栏/AE > 磁吸 > 收进背包（满则掉落） */
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

    /** 把掉落物路由到背包（满则丢出）或 AE 网络；否则收进背包（满则掉落）。 */
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
            // 一律优先收进背包（避免在原位置生成大量掉落实体）；背包满则丢出
            if (!drop.isEmpty()) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
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
