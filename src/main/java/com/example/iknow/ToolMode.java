package com.example.iknow;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * 澶氬姛鑳藉伐鍏风殑鍥涚鍔熻兘妯″紡锛屼互浣嶆帺鐮侊紙bitmask锛夋柟寮忓瓨鍌ㄤ簬鐗╁搧鐨勬暟鎹粍浠朵腑銆? */
public enum ToolMode {
    AXE(0),
    PICKAXE(1),
    HOE(2),
    SHEARS(3),
    SHOVEL(4);

    /** 榛樿锛氫簲绉嶅姛鑳藉叏閮ㄥ惎鐢?*/
    public static final int DEFAULT_MASK = 0b11111;
    /** 浜旂妯″紡鍏ㄩ儴鍚敤鏃剁殑鎺╃爜 */
    public static final int ALL_MASK = 0b11111;
    /** 绌烘帺鐮?*/
    public static final int NONE_MASK = 0;

    private final int bit;

    ToolMode(int bit) {
        this.bit = 1 << bit;
    }

    /** 璇ユā寮忓搴旂殑浣?*/
    public int bit() {
        return this.bit;
    }

    public static boolean isEnabled(int mask, ToolMode mode) {
        return (mask & mode.bit) != 0;
    }

    public static int enable(int mask, ToolMode mode) {
        return mask | mode.bit;
    }

    public static int disable(int mask, ToolMode mode) {
        return mask & ~mode.bit;
    }

    public static int toggle(int mask, ToolMode mode) {
        return mask ^ mode.bit;
    }

    /** 鍙繚鐣欐寚瀹氭ā寮忕殑鎺╃爜 */
    public static int only(ToolMode mode) {
        return mode.bit;
    }

    /** 鎺╃爜涓惎鐢ㄧ殑妯″紡鏁伴噺 */
    public static int count(int mask) {
        return Integer.bitCount(mask & ALL_MASK);
    }

    /** 璇ユā寮忕殑鏄剧ず鍚嶇О */
    public Component displayName() {
        return Component.translatable("toolmode.iknow." + name().toLowerCase(Locale.ROOT));
    }
}

