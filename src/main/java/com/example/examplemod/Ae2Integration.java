package com.example.examplemod;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * AE2（Applied Energistics 2）集成：把多功能工具当作"无线终端"，可绑定无线访问点、
 * 跨维度取网格，并把物品插入 ME 存储。全部经 {@link #isAe2Loaded()} 守卫，
 * 未安装 AE2 时本类不会被引用，从而不影响模组在无 AE2 的环境下运行（软依赖）。
 */
public final class Ae2Integration {

    private Ae2Integration() {
    }

    public static boolean isAe2Loaded() {
        return ModList.get().isLoaded("ae2");
    }

    /** 按住 shift 右键无线访问点时绑定（成功返回 true） */
    public static boolean tryBind(ItemStack stack, Level level, BlockEntity blockEntity) {
        if (!isAe2Loaded()) {
            return false;
        }
        if (!(blockEntity instanceof IWirelessAccessPoint)) {
            return false;
        }
        stack.set(ModDataComponents.LINKED_POS.get(), GlobalPos.of(level.dimension(), blockEntity.getBlockPos()));
        return true;
    }

    /** 取绑定网格（跨维度），未绑定/不可达返回 null */
    public static IGrid getLinkedGrid(ItemStack stack, ServerLevel level) {
        if (!isAe2Loaded()) {
            return null;
        }
        GlobalPos gp = stack.get(ModDataComponents.LINKED_POS.get());
        if (gp == null) {
            return null;
        }
        ServerLevel levelAt = level.getServer().getLevel(gp.dimension());
        if (levelAt == null) {
            return null;
        }
        BlockEntity be = levelAt.getBlockEntity(gp.pos());
        if (be instanceof IWirelessAccessPoint wap) {
            return wap.getGrid();
        }
        return null;
    }

    /** 把物品插入绑定网络的 ME 存储，返回实际插入数量（0 = 未绑定/未插入） */
    public static long insertIntoGrid(ItemStack tool, ServerPlayer player, ItemStack toInsert) {
        if (!isAe2Loaded()) {
            return 0;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        IGrid grid = getLinkedGrid(tool, serverLevel);
        if (grid == null) {
            return 0;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        if (storage == null || toInsert.isEmpty()) {
            return 0;
        }
        return storage.insert(
                AEItemKey.of(toInsert),
                toInsert.getCount(),
                Actionable.MODULATE,
                IActionSource.ofPlayer(player));
    }

    /** 该物品是否已绑定网络（供提示/判断用） */
    public static boolean isLinked(ItemStack stack) {
        return stack != null && stack.has(ModDataComponents.LINKED_POS.get());
    }
}
