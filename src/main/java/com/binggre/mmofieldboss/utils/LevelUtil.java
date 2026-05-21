package com.binggre.mmofieldboss.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class LevelUtil {

    private static volatile Class<?> playerDataClass;
    private static volatile Method getMethod;
    private static volatile Method getLevelMethod;
    private static volatile Method getSavedClassesMethod;
    private static volatile Method getClassInfoMethod;

    private LevelUtil() {
    }

    private static void resolve() throws Exception {
        if (playerDataClass != null) return;
        Class<?> cls = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
        getMethod = cls.getMethod("get", java.util.UUID.class);
        getLevelMethod = cls.getMethod("getLevel");
        getSavedClassesMethod = cls.getMethod("getSavedClasses");
        getClassInfoMethod = cls.getMethod("getClassInfo", String.class);
        playerDataClass = cls;
    }

    public static int getMaxLevel(Player player) {
        if (player == null) return -1;
        try {
            resolve();
            Object playerData = getMethod.invoke(null, player.getUniqueId());
            if (playerData == null) return -1;

            int max = (int) getLevelMethod.invoke(playerData);
            Object saved = getSavedClassesMethod.invoke(playerData);
            if (saved instanceof Iterable<?> iterable) {
                for (Object key : iterable) {
                    Object classInfo = getClassInfoMethod.invoke(playerData, key.toString());
                    if (classInfo == null) continue;
                    Method m = classInfo.getClass().getMethod("getLevel");
                    int lv = (int) m.invoke(classInfo);
                    if (lv > max) max = lv;
                }
            }
            return max;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }
}
