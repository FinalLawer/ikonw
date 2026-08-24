package com.example.iknow.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.example.iknow.IknowMod;

/**
 * 鏃犻檺鍘熶欢杈撳嚭鍣細鍒囨崲杈撳嚭闈㈢殑鍔ㄤ綔鍖呫€?
 */
public record InfinityOutputterActionPayload(int action) implements CustomPacketPayload {

    /** 鍒囨崲杈撳嚭闈紙杞棆涓€娆★級 */
    public static final int CYCLE_FACING = 1;

    public static final CustomPacketPayload.Type<InfinityOutputterActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IknowMod.MODID, "infinity_outputter_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityOutputterActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InfinityOutputterActionPayload::action,
                    InfinityOutputterActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}