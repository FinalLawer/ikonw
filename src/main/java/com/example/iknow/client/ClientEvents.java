package com.example.iknow.client;

import com.example.iknow.IknowMod;
import com.example.iknow.FlightHandler;
import com.example.iknow.item.MultiToolItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 瀹㈡埛绔€氱敤浜嬩欢锛堟父鎴忎簨浠舵€荤嚎锛夛細
 * 鎸変綇 R 鎵撳紑澶氬姛鑳藉伐鍏疯疆鐩橈紝鏉惧紑鍏抽棴銆? */
@EventBusSubscriber(modid = IknowMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    /** 鎵撳紑鍔熻兘杞洏鐨勬寜閿紙R锛?*/
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.iknow.open_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.iknow");

    private static boolean wheelOpen = false;
    /** 杞洏琚潪 R 閲婃斁鏂瑰紡鍏抽棴鍚庯紝鎶戝埗鍦?R 浠嶆寜浣忔湡闂撮噸寮€锛堥伩鍏嶇偣鍑荤┖鐧藉叧闂張绔嬪嵆閲嶅紑鐨勯棯鐑侊級 */
    private static boolean suppressReopen = false;
    private static int lastHintTick = Integer.MIN_VALUE;

    private ClientEvents() {
    }

    /** 杞洏鍏抽棴鏃剁敱 ModeWheelScreen 璋冪敤 */
    public static void onWheelClosed() {
        wheelOpen = false;
        suppressReopen = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (OPEN_WHEEL.isDown()) {
            if (!wheelOpen && mc.screen == null && !suppressReopen) {
                if (isHoldingMultiTool(mc.player)) {
                    mc.setScreen(new ModeWheelScreen());
                    wheelOpen = true;
                } else {
                    // 娌℃嬁宸ュ叿鏃舵彁绀猴紙闄愬埗棰戠巼锛岄伩鍏嶅埛灞忥級
        int tick = mc.player.tickCount;
                    if (tick - lastHintTick > 20) {
                        mc.player.displayClientMessage(Component.translatable("message.iknow.need_tool"), true);
                        lastHintTick = tick;
                    }
                }
            }
        } else {
            wheelOpen = false;
            suppressReopen = false;
        }

        // 鍏抽棴椋炶鎯€э細鏈寜绉诲姩閿椂闃诲凹閫熷害锛堥琛岃繍鍔ㄧ敱瀹㈡埛绔娴嬮┍鍔紝蹇呴』鍦ㄥ鎴风澶勭悊锛?
        LocalPlayer player = mc.player;
        if (player.getAbilities().flying && FlightHandler.noInertia(player)) {
            boolean moving = mc.options.keyUp.isDown()
                    || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown()
                    || mc.options.keyRight.isDown()
                    || mc.options.keyJump.isDown()
                    || mc.options.keyShift.isDown();
            if (!moving) {
                Vec3 v = player.getDeltaMovement();
                if (v.horizontalDistanceSqr() > 0.0001 || Math.abs(v.y) > 0.0001) {
                    player.setDeltaMovement(v.x * 0.6, v.y * 0.6, v.z * 0.6);
                }
            }
        }
    }

    private static boolean isHoldingMultiTool(Player player) {
        return player.getMainHandItem().getItem() instanceof MultiToolItem
                || player.getOffhandItem().getItem() instanceof MultiToolItem;
    }
}

