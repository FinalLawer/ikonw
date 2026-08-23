package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 娓呯悊鎺夎惤鐗╁畾鏃跺櫒锛? * - 绔嬪嵆娓呯悊/瀹氭椂娓呯悊鎺夎惤鐗? * - 浣跨敤鐜板疄鏃堕棿锛堟绉掞級璁℃椂锛涘墿浣?30 绉掓椂閫氳繃鑱婂ぉ妗嗘彁绀虹帺瀹? * - 鍙殏鍋?缁х画
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class CleanWorldHandler {

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final long WARN_THRESHOLD_MS = 30_000L;

    private CleanWorldHandler() {
    }

    /** 寮€濮嬪畾鏃舵竻鐞嗭紙seconds 绉掑悗娓呯悊锛屽惊鐜繍琛岋級锛岃鐩栨棫浠诲姟 */
    public static void start(ServerPlayer player, int seconds) {
        if (seconds <= 0) {
            return;
        }
        STATES.put(player.getUUID(), new State(seconds * 1000L, true, false));
    }

    /** 鏆傚仠锛坧aused=true锛夋垨缁х画锛坧aused=false锛?*/
    public static void setPaused(ServerPlayer player, boolean paused) {
        State s = STATES.get(player.getUUID());
        if (s != null) {
            s.paused = paused;
            s.lastTickMs = System.currentTimeMillis();
        }
    }

    /** 绔嬪嵆娓呯悊褰撳墠缁村害鐨勬帀钀藉疄浣?*/
    public static void clearNow(ServerPlayer player) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof ItemEntity ie) {
                    ie.discard();
                }
            }
        }
    }

    /** 褰撳墠璁℃椂鐘舵€侊紙渚?GUI 鏄剧ず锛涙棤浠诲姟杩斿洖 null锛?*/
    public static State state(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State s = STATES.get(player.getUUID());
        if (s == null || !s.running || s.paused) {
            return;
        }
        long now = System.currentTimeMillis();
        long dt = now - s.lastTickMs;
        s.lastTickMs = now;
        s.remainingMs -= dt;
        if (!s.warned30 && s.remainingMs > 0 && s.remainingMs <= WARN_THRESHOLD_MS) {
            s.warned30 = true;
            player.sendSystemMessage(Component.translatable("message.iknow.clean_warn_30"));
        }
        if (s.remainingMs <= 0) {
            clearNow(player);
            // 寰幆杩愯锛氶噸鏂板紑濮嬩竴杞€掕鏃?
        s.remainingMs = s.durationMs;
            s.warned30 = false;
            s.lastTickMs = now;
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
        }
    }

    /** 娓呯悊瀹氭椂浠诲姟鐘舵€侊紙鍙彉锛屽惊鐜繍琛岋級 */
    public static final class State {
        public long remainingMs;
        public long durationMs;
        public boolean running;
        public boolean paused;
        boolean warned30;
        long lastTickMs;

        State(long remainingMs, boolean running, boolean paused) {
            this.remainingMs = remainingMs;
            this.durationMs = remainingMs;
            this.running = running;
            this.paused = paused;
            this.lastTickMs = System.currentTimeMillis();
        }

        public int remainingSeconds() {
            return (int) Math.max(0, (remainingMs + 999) / 1000);
        }

        public int durationSeconds() {
            return (int) Math.max(0, (durationMs + 999) / 1000);
        }
    }
}

