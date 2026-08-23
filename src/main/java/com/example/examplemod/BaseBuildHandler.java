package com.example.examplemod;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 基地铺设任务的批处理队列：17×17 区块清空 + 铺地板。
 * 为避免一次性操作卡死游戏，按列分批处理（每 tick 处理有限列），直至完成。
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class BaseBuildHandler {

    /** 每 tick 处理的列数（数值越大越快但越卡；适中以避免冻结） */
    private static final int COLUMNS_PER_TICK = 16;

    /** 16 种混凝土颜色的名称（与原版一致，索引即颜色编号） */
    public static final String[] CONCRETE_NAMES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    /** 16 种混凝土方块（索引对应名称） */
    public static final Block[] CONCRETE_BLOCKS = {
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE
    };

    /** 取指定索引的混凝土方块状态 */
    static BlockState concrete(int index) {
        return CONCRETE_BLOCKS[Math.max(0, Math.min(CONCRETE_BLOCKS.length - 1, index))].defaultBlockState();
    }

    // 区块边界样式：奇/偶
        public static final int STYLE_ODD = 0;  // 边界在区块边界（x%16==0 / z%16==0），角落在区块交界
    public static final int STYLE_EVEN = 1; // 每个区块内部一整圈边框（四边都在区内），角落在区块内部
        private static final Deque<BuildTask> QUEUE = new ArrayDeque<>();

    private BaseBuildHandler() {
    }

    /** 以 pos 所在区块为中心，登记一个 size×size 区块的基地铺设任务；已运行则忽略 */
    public static void enqueue(ServerLevel level, BlockPos pos, int borderIndex, int interiorIndex, int style, int size, boolean solid) {
        if (isRunning(pos)) {
            return;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int half = size / 2;
        int cx0 = cx - half;
        int cx1 = cx + (size - half - 1);
        int cz0 = cz - half;
        int cz1 = cz + (size - half - 1);
        QUEUE.add(new BuildTask(level, pos, cx0, cx1, cz0, cz1, borderIndex, interiorIndex, style, solid));
    }

    /** 该位置是否已在铺设中 */
    public static boolean isRunning(BlockPos pos) {
        for (BuildTask task : QUEUE) {
            if (task.at(pos)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        BuildTask task = QUEUE.peek();
        if (task.step(COLUMNS_PER_TICK)) {
            QUEUE.poll();
        }
    }

    private static final class BuildTask {

        private final ServerLevel level;
        private final int floorY;
        private final int placerX, placerZ;
        private final int x0, x1, z0, z1;
        private final BlockPos.MutableBlockPos pos;
        private final BlockState borderState;
        private final BlockState interiorState;
        private final int style;
        private final boolean solid;
        private int curX, curZ;

        BuildTask(ServerLevel level, BlockPos pos, int cx0, int cx1, int cz0, int cz1, int borderIndex, int interiorIndex, int style, boolean solid) {
            this.level = level;
            this.floorY = pos.getY();
            this.placerX = pos.getX();
            this.placerZ = pos.getZ();
            this.x0 = cx0 << 4;
            this.x1 = (cx1 << 4) + 15;
            this.z0 = cz0 << 4;
            this.z1 = (cz1 << 4) + 15;
            this.curX = x0;
            this.curZ = z0;
            this.pos = new BlockPos.MutableBlockPos();
            this.borderState = concrete(borderIndex);
            this.interiorState = concrete(interiorIndex);
            this.style = style;
            this.solid = solid;
        }

        /** 处理一批列；返回是否完成 */
        boolean step(int limit) {
            int processed = 0;
            while (processed < limit && curZ <= z1) {
                clearAndFloor(curX, curZ);
                processed++;
                curX++;
                if (curX > x1) {
                    curX = x0;
                    curZ++;
                }
            }
            return curZ > z1;
        }

        /** 是否对应某个铺设器方块位置 */
        boolean at(BlockPos p) {
            return placerX == p.getX() && placerZ == p.getZ();
        }

        private void clearAndFloor(int x, int z) {
            int minY = level.getMinBuildHeight(); // 最底层（基岩）保留
        boolean isPlacerCol = x == placerX && z == placerZ;
            if (solid) {
                // 实心：整个列从基岩上一层到地板层，逐层铺设分色地板（向下延伸到基岩）
        for (int y = minY + 1; y <= floorY; y++) {
                    if (isPlacerCol && y == floorY) {
                        continue; // 保留基地铺设器方块本体
                    }
                    BlockState s = isBoundary(x, z) ? borderState : interiorState;
                    level.setBlock(new BlockPos(x, y, z), s, 2);
                }
                return;
            }
            // 非实心：只清理地板层以下，并在地板层铺设分色
        for (int y = minY + 1; y < floorY; y++) {
                pos.set(x, y, z);
                if (level.getBlockState(pos).isAir()) {
                    continue; // 跳过已有空气，减少工作量
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            if (!isPlacerCol) {
                BlockState floor = isBoundary(x, z) ? borderState : interiorState;
                level.setBlock(new BlockPos(x, floorY, z), floor, 3);
            }
        }

        /** 区块边界：奇样式沿区块边界+整个地板四周外框；偶样式在每区块内部整圈边框（四边） */
        private boolean isBoundary(int x, int z) {
            if (style == STYLE_EVEN) {
                return (x & 15) == 0 || (x & 15) == 15 || (z & 15) == 0 || (z & 15) == 15;
            }
            // 奇数格：区块边界线 + 整个地板的外沿（x0/x1/z0/z1）补全边框
        return (x & 15) == 0 || (z & 15) == 0 || x == x0 || x == x1 || z == z0 || z == z1;
        }
    }
}
