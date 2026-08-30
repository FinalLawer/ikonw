package com.example.iknow;

import com.example.iknow.item.IknowToolItem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 爆破连锁：挖掘矿物方块（{@code neoforge:ores}）时，连锁挖掉以该方块为中心、
 * 半径 {@link #RADIUS}（欧氏距离）球体内的全部矿物方块。
 * <p>
 * 采用"球壳 + 分批"处理：触发时只把目标矿石坐标收集进队列（按到原点距离由内到外排列），
 * 之后每个 tick 处理一小批（{@link #ORES_PER_TICK}），避免一次性挖掉成千上万个矿石造成
 * 服务端卡死；只扫描已加载区块，掉落入背包 / AE。
 * </p>
 */
@EventBusSubscriber(modid = IknowMod.MODID)
public final class BlastChainHandler {

    /** 连锁半径（欧氏距离，单位：方块） */
    public static final int RADIUS = 64;

    /** 每个 tick 处理的矿石数量（越大越快但越卡；2 秒 ≈ 40 tick） */
    private static final int ORES_PER_TICK = 1200;

    /** 待挖矿石队列（按到原点距离由内到外排序） */
    private static final Deque<OreTask> QUEUE = new ArrayDeque<>();

    private BlastChainHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
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
        // 收集球体内所有矿物（不含原点），按到原点距离由内到外排序（球壳分批）
        List<OreTask> targets = collectOrePositions(level, origin, tool, player);
        IknowMod.LOGGER.info("[BlastChain] origin={} breakMode={} magnet={} collected={}",
                origin.toShortString(), IknowToolItem.breakMode(tool),
                IknowToolItem.magnetMode(tool), targets.size());
        if (targets.isEmpty()) {
            return;
        }
        // 已有任务在处理：追加进同一队列（由内到外插到末尾，最内层先）
        QUEUE.addAll(targets);
    }

    /** 收集以 origin 为中心、半径 RADIUS 欧氏球体内的所有矿物坐标（不含原点），并按距离排序。 */
    private static List<OreTask> collectOrePositions(ServerLevel level, BlockPos origin, ItemStack tool, Player player) {
        List<OreTask> result = new ArrayList<>();
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        long r2 = (long) RADIUS * RADIUS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    // 欧氏球体裁剪（dx²+dy²+dz² <= R²）
                    long d2 = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (d2 > r2) {
                        continue;
                    }
                    pos.set(ox + dx, oy + dy, oz + dz);
                    // 只扫描已加载区块，避免强制加载大片未加载区块
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    if (level.getBlockState(pos).is(Tags.Blocks.ORES)) {
                        result.add(new OreTask(pos.immutable(), level, tool, player));
                    }
                }
            }
        }
        // 按到原点的距离由内到外排序（球壳分批：先挖里层，再挖外层）
        result.sort(Comparator.comparingDouble(t -> t.distSq(origin)));
        return result;
    }

    /** 每 tick 从各任务队列挖一批，直到队列空或处理完该批次 */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) {
            return;
        }
        int processed = 0;
        while (processed < ORES_PER_TICK && !QUEUE.isEmpty()) {
            OreTask task = QUEUE.poll();
            if (task == null) {
                break;
            }
            // 用任务所记录的 level 与玩家处理，避免维度错配
            ServerLevel level = task.level();
            Player player = task.player();
            if (level == null || player == null || !(player instanceof ServerPlayer sp)) {
                continue;
            }
            try {
                mineOre(level, task.pos(), sp, task.tool());
            } catch (Exception e) {
                IknowMod.LOGGER.warn("[BlastChain] failed at {}: {}",
                        task.pos().toShortString(), e.toString());
            }
            processed++;
        }
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

    /** 待挖矿石任务：记录坐标、所属维度、来源工具与玩家 */
    private record OreTask(BlockPos pos, ServerLevel level, ItemStack tool, Player player) {
        double distSq(BlockPos origin) {
            return pos.distSqr(origin);
        }
    }
}
