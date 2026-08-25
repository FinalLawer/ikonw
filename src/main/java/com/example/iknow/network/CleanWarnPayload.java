package com.example.iknow.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.example.iknow.IknowMod;

/**
 * 清理掉落物：定时清理前是否提示（1 分钟提醒开关）。
 */
public record CleanWarnPayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CleanWarnPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "clean_warn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CleanWarnPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, CleanWarnPayload::enabled,
                    CleanWarnPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
