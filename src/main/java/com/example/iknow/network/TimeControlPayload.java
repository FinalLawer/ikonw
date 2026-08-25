package com.example.iknow.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.example.iknow.IknowMod;

/**
 * 时间控制包：设置游戏时间为清晨/正午/傍晚/黑夜，或锁定时间流逝（日夜循环开关）。
 */
public record TimeControlPayload(int action) implements CustomPacketPayload {

    public static final int SET_MORNING = 0;
    public static final int SET_NOON = 1;
    public static final int SET_DUSK = 2;
    public static final int SET_NIGHT = 3;
    public static final int TOGGLE_LOCK = 4;

    public static final CustomPacketPayload.Type<TimeControlPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "time_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimeControlPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TimeControlPayload::action,
                    TimeControlPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
