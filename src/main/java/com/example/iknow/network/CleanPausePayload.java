package com.example.iknow.network;

import com.example.iknow.IknowMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：暂停/继续定时清理 */
public record CleanPausePayload(boolean paused) implements CustomPacketPayload {

    public static final Type<CleanPausePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "clean_pause"));

    public static final StreamCodec<ByteBuf, CleanPausePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, CleanPausePayload::paused, CleanPausePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
