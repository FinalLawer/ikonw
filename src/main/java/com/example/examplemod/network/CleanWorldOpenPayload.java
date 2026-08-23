package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 鏈嶅姟绔?鈫?瀹㈡埛绔細鎵撳紑"浣犳兂瑕佸共鍑€鐨勪笘鐣?GUI锛屽苟闄勫甫褰撳墠璁℃椂鐘舵€?*/
public record CleanWorldOpenPayload(int remainingSeconds, int durationSeconds, boolean running, boolean paused) implements CustomPacketPayload {

    public static final Type<CleanWorldOpenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "clean_world_open"));

    public static final StreamCodec<ByteBuf, CleanWorldOpenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CleanWorldOpenPayload::remainingSeconds,
                    ByteBufCodecs.VAR_INT, CleanWorldOpenPayload::durationSeconds,
                    ByteBufCodecs.BOOL, CleanWorldOpenPayload::running,
                    ByteBufCodecs.BOOL, CleanWorldOpenPayload::paused,
                    CleanWorldOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

