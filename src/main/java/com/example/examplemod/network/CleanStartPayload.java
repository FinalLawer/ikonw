package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：启动定时清理（seconds 秒后清理掉落物） */
public record CleanStartPayload(int seconds) implements CustomPacketPayload {

    public static final Type<CleanStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "clean_start"));

    public static final StreamCodec<ByteBuf, CleanStartPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, CleanStartPayload::seconds, CleanStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
