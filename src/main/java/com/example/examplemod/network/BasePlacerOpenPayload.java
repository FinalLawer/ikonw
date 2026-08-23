package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：打开基地铺设器 GUI，并附带当前是否正在铺设状态 */
public record BasePlacerOpenPayload(BlockPos pos, boolean running) implements CustomPacketPayload {

    public static final Type<BasePlacerOpenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "base_placer_open"));

    public static final StreamCodec<ByteBuf, BasePlacerOpenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BasePlacerOpenPayload::pos,
                    ByteBufCodecs.BOOL, BasePlacerOpenPayload::running,
                    BasePlacerOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
