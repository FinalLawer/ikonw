package com.example.iknow.network;

import com.example.iknow.Ae2Integration;
import com.example.iknow.BaseBuildHandler;
import com.example.iknow.CleanWorldHandler;
import com.example.iknow.IknowMod;
import com.example.iknow.FlightHandler;
import com.example.iknow.ModDataComponents;
import com.example.iknow.client.BasePlacerScreen;
import com.example.iknow.client.CleanWorldScreen;
import com.example.iknow.item.IknowToolItem;
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
                        if (stack.getItem() instanceof IknowToolItem) {
                            stack.set(ModDataComponents.TOOL_MODES.get(), payload.toolMask());
                            stack.set(ModDataComponents.ENCHANT_MODE.get(), payload.enchantMode());
                            stack.set(ModDataComponents.MINING_SPEED.get(), payload.miningSpeed());
                            stack.set(ModDataComponents.FLIGHT_SPEED.get(), payload.flightSpeed());
                            stack.set(ModDataComponents.PICKUP_MODE.get(), payload.pickupMode());
                            stack.set(ModDataComponents.BLOCK_REACH.get(), payload.blockReach());
                            stack.set(ModDataComponents.ATTACK_REACH.get(), payload.attackReach());
                            IknowToolItem.setBlastChain(stack, (payload.flags() & ModeChangePayload.FLAG_BLAST_CHAIN) != 0);
                            IknowToolItem.applyEnchantments(stack, payload.enchantMode(), player.level().registryAccess());
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
                    ItemStack tool = heldIknowTool(player);
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
        // 定时清理前提示开关
        registrar.playToServer(CleanWarnPayload.TYPE, CleanWarnPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer sp) {
                        CleanWorldHandler.setWarnEnabled(sp, payload.enabled());
                    }
                }));
        // 时间控制：设置清晨/正午/傍晚/黑夜，或锁定时间流逝
        registrar.playToServer(TimeControlPayload.TYPE, TimeControlPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() == null || !(context.player().level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                        return;
                    }
                    switch (payload.action()) {
                        case TimeControlPayload.SET_MORNING -> serverLevel.setDayTime(0);
                        case TimeControlPayload.SET_NOON -> serverLevel.setDayTime(6000);
                        case TimeControlPayload.SET_DUSK -> serverLevel.setDayTime(12000);
                        case TimeControlPayload.SET_NIGHT -> serverLevel.setDayTime(18000);
                        case TimeControlPayload.TOGGLE_LOCK -> {
                            boolean locked = serverLevel.getGameRules()
                                    .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).get();
                            serverLevel.getGameRules()
                                    .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                                    .set(!locked, serverLevel.getServer());
                        }
                        default -> {
                        }
                    }
                }));
    }

    /** 客户端发送时间控制动作到服务端 */
    public static void sendTimeControl(int action) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TimeControlPayload(action));
    }

    private static ItemStack heldIknowTool(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IknowToolItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof IknowToolItem ? off : null;
    }
}

