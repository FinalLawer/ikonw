package com.example.iknow;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * 鐗╁搧鎷惧彇/鏀跺妯″紡锛圓E 闆嗘垚杞洏鐨?4 绉嶆ā寮忥級銆? * 鍗曢€夐」锛岄粯璁ゅ叧闂紙NONE锛夈€? */
public enum PickupMode {
    NONE,
    /** 鏅€氱鍚革細闄勮繎鐗╁搧椋炲悜鐜╁锛岃繘鍏ョ墿鍝佹爮 */
    MAGNET,
    /** 纾佸惛杩?AE锛氶檮杩戠墿鍝侀鍚戠帺瀹跺苟瀛樺叆 AE 缃戠粶 */
    MAGNET_AE,
    /** 鐮村潖鐗╁搧鐩存帴杩涘叆鐗╁搧鏍忥細鎸栨帢鎺夎惤鐬棿鏀惰繘鐗╁搧鏍?*/
    BREAK_INVENTORY,
    /** 鐮村潖鐗╁搧鐩存帴杩涘叆 AE 绯荤粺锛氭寲鎺樻帀钀界灛闂村瓨鍏?AE 缃戠粶 */
    BREAK_AE;

    public static final int DEFAULT = NONE.ordinal();

    private static final PickupMode[] VALUES = values();

    public static PickupMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }

    public Component displayName() {
        return Component.translatable("pickup.iknow." + name().toLowerCase(Locale.ROOT));
    }
}

