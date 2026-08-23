package com.example.iknow;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * 龙之进化（龙之研究）集成：让多功能工具可以直接击杀"混沌龙"。
 * 采用反射以软依赖方式引用 DE 类，避免编译期强依赖与类层级解析问题；
 * 未安装 DE 时 {@link #isLoaded()} 提前返回。
 */
public final class DeIntegration {

    private static final String GUARDIAN_CLASS =
            "com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity";

    private static Class<?> cachedGuardianClass;

    private DeIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("draconicevolution");
    }

    /**
     * 若目标是混沌龙或其部件，反射调用其 kill() 使其死亡，返回 true。
     * 服务端执行（客户端触发时仅返回 true 表示已处理）。
     */
    public static boolean tryKillGuardian(Entity target) {
        if (!isLoaded()) {
            return false;
        }
        Entity guardian = unwrapGuardian(target);
        if (guardian == null) {
            return false;
        }
        if (!guardian.level().isClientSide()) {
            invokeKill(guardian);
        }
        return true;
    }

    private static Entity unwrapGuardian(Entity target) {
        if (isGuardian(target)) {
            return target;
        }
        if (target instanceof PartEntity<?> part && isGuardian(part.getParent())) {
            return part.getParent();
        }
        return null;
    }

    private static boolean isGuardian(Entity e) {
        Class<?> cls = guardianClass();
        return cls != null && e != null && cls.isInstance(e);
    }

    private static Class<?> guardianClass() {
        if (cachedGuardianClass == null) {
            try {
                cachedGuardianClass = Class.forName(GUARDIAN_CLASS);
            } catch (Throwable t) {
                cachedGuardianClass = null;
            }
        }
        return cachedGuardianClass;
    }

    private static void invokeKill(Entity guardian) {
        try {
            guardian.getClass().getMethod("kill").invoke(guardian);
        } catch (Throwable t) {
            // 忽略；不影响正常攻击
        }
    }
}
