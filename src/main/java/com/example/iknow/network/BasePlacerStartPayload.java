package com.example.iknow.network;

import com.example.iknow.IknowMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：请求为指定基地铺设器开始铺设（颜色、样式、大小、是否实心） */
public record BasePlacerStartPayload(BlockPos pos, int borderColor, int interiorColor, int style, int size, boolean solid)
        implements CustomPacketPayload {

    public static final Type<BasePlacerStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "base_placer_start"));

    public static final StreamCodec<ByteBuf, BasePlacerStartPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BasePlacerStartPayload::pos,
                    ByteBufCodecs.VAR_INT, BasePlacerStartPayload::borderColor,
                    ByteBufCodecs.VAR_INT, BasePlacerStartPayload::interiorColor,
                    ByteBufCodecs.VAR_INT, BasePlacerStartPayload::style,
                    ByteBufCodecs.VAR_INT, BasePlacerStartPayload::size,
                    ByteBufCodecs.BOOL, BasePlacerStartPayload::solid,
                    BasePlacerStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
