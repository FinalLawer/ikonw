package com.example.examplemod;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义数据组件注册表。
 * - {@link #TOOL_MODES}：int 位掩码，记录启用的工具模式（{@link ToolMode}）
 * - {@link #ENCHANT_MODE}：int，记录附魔模式（0=关闭，1=精准采集，2=时运），仅可单选
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ExampleMod.MODID);

    /** 工具模式位掩码组件 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_MODES =
            DATA_COMPONENTS.registerComponentType("tool_modes",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 附魔模式组件（0=关闭，1=精准采集，2=时运） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENCHANT_MODE =
            DATA_COMPONENTS.registerComponentType("enchant_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 挖掘速度滑块值（0-100，默认 50 = 当前速度 40） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MINING_SPEED =
            DATA_COMPONENTS.registerComponentType("mining_speed",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 飞行速度滑块值（0-100，默认 50 = 原版速度，全方向生效） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLIGHT_SPEED =
            DATA_COMPONENTS.registerComponentType("flight_speed",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 方块触及距离（默认=原版默认值，最高 10） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> BLOCK_REACH =
            DATA_COMPONENTS.registerComponentType("block_reach",
                    builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** 攻击距离（默认=原版默认值，最高 10） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ATTACK_REACH =
            DATA_COMPONENTS.registerComponentType("attack_reach",
                    builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** 拾取模式（{@link PickupMode} 的 ordinal，默认 0=NONE） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PICKUP_MODE =
            DATA_COMPONENTS.registerComponentType("pickup_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** AE 无线访问点绑定位置 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKED_POS =
            DATA_COMPONENTS.registerComponentType("linked_pos",
                    builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    private ModDataComponents() {
    }
}
