package com.example.iknow;

import com.example.iknow.item.IknowToolItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 多功能工具的玩家级效果：
 * - 拥有工具时可创造飞行
 * - 可关闭飞行惯性（飞行中持续阻尼水平速度，松开按键立即停下）
 * - 手持工具时取消水下 / 浮空挖掘惩罚
 */
@EventBusSubscriber(modid = IknowMod.MODID)
public final class FlightHandler {

    /** 关闭飞行惯性的玩家 UUID 集合 */
    private static final Set<UUID> NO_INERTIA = new HashSet<>();
    /** 关闭夜视的玩家 UUID 集合（默认开启夜视） */
    private static final Set<UUID> NIGHT_VISION_DISABLED = new HashSet<>();

    private FlightHandler() {
    }

    public static void setNoInertia(Player player, boolean on) {
        if (on) {
            NO_INERTIA.add(player.getUUID());
        } else {
            NO_INERTIA.remove(player.getUUID());
        }
    }

    public static boolean noInertia(Player player) {
        return NO_INERTIA.contains(player.getUUID());
    }

    public static void setNightVision(Player player, boolean on) {
        if (on) {
            NIGHT_VISION_DISABLED.remove(player.getUUID());
        } else {
            NIGHT_VISION_DISABLED.add(player.getUUID());
        }
    }

    public static boolean nightVisionEnabled(Player player) {
        return !NIGHT_VISION_DISABLED.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        Abilities abilities = player.getAbilities();
        ItemStack tool = findIknowTool(player);
        // 拥有工具 → 创造飞行（不改动创造模式玩家）
        if (!abilities.instabuild) {
            boolean hasTool = tool != null;
            if (hasTool != abilities.mayfly) {
                abilities.mayfly = hasTool;
                if (!hasTool && abilities.flying) {
                    abilities.flying = false;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            }
        }
        // 飞行速度：按背包/手持工具的滑块值设置（全方向生效），无工具恢复原版 0.05
        float target = tool != null ? 0.05F * IknowToolItem.flightSpeed(tool) / 50.0F : 0.05F;
        target = Math.max(0.0F, target);
        if (Math.abs(abilities.getFlyingSpeed() - target) > 0.0001F) {
            abilities.setFlyingSpeed(target);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        }
        // 拥有工具 → 不再饥饿（食物条始终回满、消耗清零）
        if (tool != null) {
            FoodData food = player.getFoodData();
            if (food.getFoodLevel() < 20) {
                food.setFoodLevel(20);
            }
            if (food.getSaturationLevel() < 20.0F) {
                food.setSaturation(20.0F);
            }
            if (food.getExhaustionLevel() > 0.0F) {
                food.setExhaustion(0.0F);
            }
        }
        // 拥有工具 → 永久夜视（无限时长，不循环、不衰减、不闪烁）；失去工具或关闭时移除
        if (tool != null && nightVisionEnabled(player)) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || !current.isInfiniteDuration()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            }
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        // 拥有工具 → 防火 + 防溺水 + 抗性提升 X（均为无限）
        if (tool != null) {
            ensureInfiniteEffect(player, MobEffects.FIRE_RESISTANCE, 0);
            ensureInfiniteEffect(player, MobEffects.WATER_BREATHING, 0);
            ensureInfiniteEffect(player, MobEffects.DAMAGE_RESISTANCE, 9); // 抗性提升 X
        } else {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
            player.removeEffect(MobEffects.WATER_BREATHING);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        // 触及距离 / 攻击距离：拥有工具时按滑块设置（最低=原版默认，最高=10）
        if (tool != null) {
            applyReach(player, tool);
        } else {
            resetReach(player);
        }
        // 物品磁吸模式（磁吸 / 磁吸进 AE）：只要背包有工具就生效；
        // 破坏类（破坏入包 / 破坏入 AE）由 PickupEvents.onBlockBreak 路由，不做磁吸，
        // 避免把玩家扔出的掉落物也被吸走。
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack magnetTool = findIknowTool(player);
            if (magnetTool != null) {
                PickupMode mmode = IknowToolItem.magnetMode(magnetTool);
                if (mmode == PickupMode.MAGNET || mmode == PickupMode.MAGNET_AE) {
                    handlePickup(serverPlayer, magnetTool, mmode);
                }
            }
        }
    }

    /** 确保玩家拥有某无限时长的效果（若不匹配则重新施加，避免闪烁） */
    private static void ensureInfiniteEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || !current.isInfiniteDuration() || current.getAmplifier() != amplifier) {
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, false));
        }
    }

    /** 拥有工具时按滑块值设置触及距离与攻击距离 */
    private static void applyReach(Player player, ItemStack tool) {
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance attack = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (block != null) {
            block.setBaseValue(IknowToolItem.blockReach(tool));
        }
        if (attack != null) {
            attack.setBaseValue(IknowToolItem.attackReach(tool));
        }
    }

    /** 失去工具时把触及距离恢复原版默认 */
    private static void resetReach(Player player) {
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance attack = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (block != null) {
            block.setBaseValue(Attributes.BLOCK_INTERACTION_RANGE.value().getDefaultValue());
        }
        if (attack != null) {
            attack.setBaseValue(Attributes.ENTITY_INTERACTION_RANGE.value().getDefaultValue());
        }
    }

    /** 磁吸 + 物品路由：把附近掉落吸到脚边停下（非乱飞），AE 模式靠近时插入 ME 存储 */
    private static void handlePickup(ServerPlayer player, ItemStack tool, PickupMode mode) {
        boolean toAe = mode == PickupMode.MAGNET_AE;
        int radius = 7; // 吸取范围 7 格
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(radius), e -> e.isAlive());
        if (items.isEmpty()) {
            return;
        }
        Vec3 target = player.position().add(0, 0.2, 0); // 脚边
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            double distSq = player.distanceToSqr(entity);
            if (toAe && distSq < 9) {
                long inserted = Ae2Integration.insertIntoGrid(tool, player, stack);
                if (inserted > 0) {
                    stack.shrink((int) inserted);
                    if (stack.isEmpty()) {
                        entity.discard();
                        continue;
                    }
                }
            }
            Vec3 to = target.subtract(entity.position());
            double d = to.length();
            if (d < 1.0) {
                // 已到脚边：停下，等玩家拾取或堆在脚下（满栏时不再乱飞）
        entity.setDeltaMovement(Vec3.ZERO);
            } else {
                // 朝脚边平滑加速；越近越慢，避免冲过头/抖动
        entity.setDeltaMovement(to.normalize().scale(Math.min(2.0, d * 0.4)));
            }
        }
    }

    /** 在玩家背包/装备栏/副手中查找多功能工具（优先主背包） */
    private static ItemStack findIknowTool(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isIknowTool(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (isIknowTool(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isIknowTool(stack)) {
                return stack;
            }
        }
        return null;
    }

    /**
     * 手持工具时：
     * - 统一所有方块的挖掘时间：挖掘进度 = 挖掘速度 ÷ 方块硬度 ÷ 30，
     *   这里把速度 × 硬度，使最终时间与方块硬度无关
     * - 取消水下 / 浮空挖掘惩罚（1.21.1 中这些惩罚在事件之前生效，反向补偿）
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isHoldingIknowTool(player)) {
            return;
        }
        float speed = event.getNewSpeed();
        // 1) 统一挖掘时间：× 方块硬度
        BlockPos pos = event.getPosition().orElse(player.blockPosition());
        float hardness = event.getState().getDestroySpeed(player.level(), pos);
        if (hardness > 0.0F) {
            speed *= hardness;
        }
        // 2) 取消水下挖掘惩罚（SUBMERGED_MINING_SPEED 属性，默认 0.2）
        if (player.isEyeInFluid(FluidTags.WATER)) {
            double submerged = player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged < 1.0) {
                speed = (float) (speed / submerged);
            }
        }
        // 3) 取消浮空挖掘惩罚（÷5）
        if (!player.onGround()) {
            speed *= 5.0F;
        }
        event.setNewSpeed(speed);
    }

    private static boolean isHoldingIknowTool(Player player) {
        return player.getMainHandItem().getItem() instanceof IknowToolItem
                || player.getOffhandItem().getItem() instanceof IknowToolItem;
    }

    private static boolean isIknowTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(IknowMod.IKNOW_TOOL);
    }
}
