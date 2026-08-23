package com.example.iknow.network;

import com.example.iknow.IknowMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：把多功能工具绑定到所点击的 AE 无线访问点（携带其方块坐标）。
 * 服务于潜行+右键绑定，用于在客户端预判被拦截时仍能在服务端完成绑定。
 */
public record AeBindPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AeBindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "ae_bind"));

    public static final StreamCodec<ByteBuf, AeBindPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, AeBindPayload::pos, AeBindPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
