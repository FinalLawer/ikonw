package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：更新多功能工具的完整配置。
 * flags 位掩码：bit0=无飞行惯性，bit1=夜视。
 * blockReach/attackReach 为浮点触及距离（最低=原版默认，最高=10）。
 * 字段较多，使用手动 ByteBuf codec（composite 仅支持最多 6 个字段）。
 */
public record ModeChangePayload(int toolMask, int enchantMode, int miningSpeed, int flightSpeed,
                                int pickupMode, int flags, float blockReach, float attackReach)
        implements CustomPacketPayload {

    public static final int FLAG_NO_INERTIA = 0b01;
    public static final int FLAG_NIGHT_VISION = 0b10;

    public static final Type<ModeChangePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "mode_change"));

    public static final StreamCodec<ByteBuf, ModeChangePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.toolMask());
                ByteBufCodecs.VAR_INT.encode(buf, p.enchantMode());
                ByteBufCodecs.VAR_INT.encode(buf, p.miningSpeed());
                ByteBufCodecs.VAR_INT.encode(buf, p.flightSpeed());
                ByteBufCodecs.VAR_INT.encode(buf, p.pickupMode());
                ByteBufCodecs.VAR_INT.encode(buf, p.flags());
                ByteBufCodecs.FLOAT.encode(buf, p.blockReach());
                ByteBufCodecs.FLOAT.encode(buf, p.attackReach());
            },
            buf -> new ModeChangePayload(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
