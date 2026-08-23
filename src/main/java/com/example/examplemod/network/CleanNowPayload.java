package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：立即清理掉落物 */
public record CleanNowPayload() implements CustomPacketPayload {

    public static final Type<CleanNowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "clean_now"));

    public static final StreamCodec<ByteBuf, CleanNowPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {}, buf -> new CleanNowPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
