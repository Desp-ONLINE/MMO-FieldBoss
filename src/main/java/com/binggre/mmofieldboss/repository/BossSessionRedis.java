package com.binggre.mmofieldboss.repository;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mongolibraryplugin.MongoLibraryPlugin;
import org.swlab.etcetera.EtCetera;
import redis.clients.jedis.RedisClient;

import java.util.logging.Logger;

public class BossSessionRedis {

    private static final String KEY_PREFIX = "mmofieldboss:open:";
    private static final long EXPIRE_SECONDS = 600;

    private BossSessionRedis() {
    }

    public static String currentChannelName() {
        String type = EtCetera.getChannelType();
        int num = EtCetera.getChannelNumber();
        if (type == null || type.isEmpty()) {
            return "";
        }
        return num == 1 ? type : (type + num);
    }

    private static RedisClient redis() {
        try {
            return MongoLibraryPlugin.getInst().getRedisClient();
        } catch (Throwable t) {
            MMOFieldBoss.getPlugin().getLogger().warning("[BossSessionRedis] redis() failed: " + t.getMessage());
            return null;
        }
    }

    public static void setOpen(int bossId, String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return;
        }
        Logger log = MMOFieldBoss.getPlugin().getLogger();
        RedisClient r = redis();
        if (r == null) {
            log.warning("[BossSessionRedis] setOpen skipped (no redis). bossId=" + bossId + " server=" + serverName);
            return;
        }
        try {
            r.setex(KEY_PREFIX + bossId, EXPIRE_SECONDS, serverName);
            log.info("[BossSessionRedis] setOpen bossId=" + bossId + " -> " + serverName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearOpen(int bossId) {
        RedisClient r = redis();
        if (r == null) {
            return;
        }
        try {
            r.del(KEY_PREFIX + bossId);
            MMOFieldBoss.getPlugin().getLogger().info("[BossSessionRedis] clearOpen bossId=" + bossId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getOpenServer(int bossId) {
        Logger log = MMOFieldBoss.getPlugin().getLogger();
        RedisClient r = redis();
        if (r == null) {
            log.warning("[BossSessionRedis] getOpenServer skipped (no redis). bossId=" + bossId);
            return null;
        }
        try {
            String value = r.get(KEY_PREFIX + bossId);
            log.info("[BossSessionRedis] getOpenServer bossId=" + bossId + " -> " + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
