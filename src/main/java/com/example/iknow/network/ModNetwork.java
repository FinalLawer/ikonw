package com.example.iknow.network;

import com.example.iknow.Ae2Integration;
import com.example.iknow.BaseBuildHandler;
import com.example.iknow.CleanWorldHandler;
import com.example.iknow.IknowMod;
import com.example.iknow.FlightHandler;
import com.example.iknow.ModDataComponents;
import com.example.iknow.client.BasePlacerScreen;
import com.example.iknow.client.CleanWorldScreen;
import com.example.iknow.item.MultiToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 缃戠粶杞借嵎娉ㄥ唽銆? */
public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ModeChangePayload.TYPE, ModeChangePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null) {
                        return;
                    }
                    // 椋炶鎯€?/ 澶滆寮€鍏筹紙鐜╁绾ц缃紝flags 浣嶆帺鐮侊級
        FlightHandler.setNoInertia(player, (payload.flags() & ModeChangePayload.FLAG_NO_INERTIA) != 0);
                    FlightHandler.setNightVision(player, (payload.flags() & ModeChangePayload.FLAG_NIGHT_VISION) != 0);
                    // 鏇存柊涓绘墜锛堟垨鍓墜锛夌殑澶氬姛鑳藉伐鍏锋ā寮忋€侀檮榄斾笌鎸栨帢閫熷害
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
                        if (stack.getItem() instanceof MultiToolItem) {
                            stack.set(ModDataComponents.TOOL_MODES.get(), payload.toolMask());
                            stack.set(ModDataComponents.ENCHANT_MODE.get(), payload.enchantMode());
                            stack.set(ModDataComponents.MINING_SPEED.get(), payload.miningSpeed());
                            stack.set(ModDataComponents.FLIGHT_SPEED.get(), payload.flightSpeed());
                            stack.set(ModDataComponents.PICKUP_MODE.get(), payload.pickupMode());
                            stack.set(ModDataComponents.BLOCK_REACH.get(), payload.blockReach());
                            stack.set(ModDataComponents.ATTACK_REACH.get(), payload.attackReach());
                            MultiToolItem.applyEnchantments(stack, payload.enchantMode(), player.level().registryAccess());
                        }
                    }
                }));
        // AE 缁戝畾锛氬鎴风宸叉嫤鎴鍒ゆ媶瑙ｏ紝杩欓噷瀹屾垚瀹為檯缁戝畾
        registrar.playToServer(AeBindPayload.TYPE, AeBindPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null) {
                        return;
                    }
                    ItemStack tool = heldMultiTool(player);
                    if (tool == null || !Ae2Integration.isAe2Loaded()) {
                        return;
                    }
                    BlockEntity be = player.level().getBlockEntity(payload.pos());
                    if (Ae2Integration.tryBind(tool, player.level(), be)) {
                        if (player instanceof ServerPlayer sp) {
                            sp.displayClientMessage(Component.translatable("message.iknow.ae_bound"), true);
                        }
                    }
                }));
        // 鍩哄湴閾鸿鍣細鍙抽敭鎵撳紑 GUI
        registrar.playToClient(BasePlacerOpenPayload.TYPE, BasePlacerOpenPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new BasePlacerScreen(payload.pos(), payload.running()));
                }));
        // 鍩哄湴閾鸿鍣細鎸変笅"寮€濮嬮摵璁?鎸夐挳
        registrar.playToServer(BasePlacerStartPayload.TYPE, BasePlacerStartPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player != null && player.level() instanceof ServerLevel serverLevel) {
                        BaseBuildHandler.enqueue(serverLevel, payload.pos(), payload.borderColor(), payload.interiorColor(), payload.style(), payload.size(), payload.solid());
                    }
                }));
        // 浣犳兂瑕佸共鍑€鐨勪笘鐣岋細鎵撳紑 GUI
        registrar.playToClient(CleanWorldOpenPayload.TYPE, CleanWorldOpenPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                        new CleanWorldScreen(payload.remainingSeconds(), payload.durationSeconds(), payload.running(), payload.paused()))));
        // 立即清理掉落物
        registrar.playToServer(CleanNowPayload.TYPE, CleanNowPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CleanWorldHandler.clearNow(sp);
                    }
                }));
        // 鍚姩瀹氭椂娓呯悊
        registrar.playToServer(CleanStartPayload.TYPE, CleanStartPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CleanWorldHandler.start(sp, payload.seconds());
                    }
                }));
        // 鏆傚仠 / 缁х画瀹氭椂娓呯悊
        registrar.playToServer(CleanPausePayload.TYPE, CleanPausePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CleanWorldHandler.setPaused(sp, payload.paused());
                    }
                }));
    }

    private static ItemStack heldMultiTool(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof MultiToolItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof MultiToolItem ? off : null;
    }
}

