package com.binggre.mmofieldboss.repository;

import com.binggre.binggreapi.functions.Callback;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossRedis;
import com.mongodb.client.FindIterable;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldBossRepository {

    public static final String DIRECTORY = MMOFieldBoss.getPlugin().getDataFolder().getPath();

    private final Map<Integer, FieldBoss> cache = new HashMap<>();

    public void onEnable() {
        if (!cache.isEmpty()) {
            for (FieldBoss value : cache.values()) {
                value.cancelTask();
            }
        }
        cache.clear();
        readFiles(file -> {
            FieldBoss read = FileManager.read(FieldBoss.class, file);
            read.init();
            read.onInit();

            cache.put(read.getId(), read);
        });
    }

    public void save(FieldBoss fieldBoss) {
        readFiles(file -> {
            FieldBoss read = FileManager.read(FieldBoss.class, file);
            if (read.getId() == fieldBoss.getId()) {
                FileManager.write(file, fieldBoss);
            }
        });
    }

    public void requestPutAllInRedis() {
        FieldBossRedisRepository redisRepository = MMOFieldBoss.getPlugin().getRedisRepository();
        for (FieldBossRedis value : redisRepository.values()) {
            if (value.isJsonUseOnly()) {
                FieldBoss fieldBoss = value.toFieldBoss();
                fieldBoss.init();
                putIn(fieldBoss);
            }
        }
    }

    public void putIn(FieldBoss fieldBoss) {
        cache.put(fieldBoss.getId(), fieldBoss);
    }

    public FieldBoss get(int id) {
        return cache.get(id);
    }

    public List<FieldBoss> values() {
        return cache.values().stream().toList();
    }

    private void readFiles(Callback<File> callback) {
        for (File file : FileManager.readFiles(DIRECTORY)) {
            callback.accept(file);
        }
    }

    public Location deserializeLocation(FieldBoss fieldboss, String serializedLocation) {
        String[] split = serializedLocation
                .replace(" ", "")
                .split(",");

        String world = null;
        double x = -1, y = -1, z = -1;
        float yaw = 0, pitch = 0;

        for (String element : split) {
            if (element.startsWith("world:")) {
                world = element.replace("world:", "");
            } else if (element.startsWith("x:")) {
                x = Double.parseDouble(element.replace("x:", ""));
            } else if (element.startsWith("y:")) {
                y = Double.parseDouble(element.replace("y:", ""));
            } else if (element.startsWith("z:")) {
                z = Double.parseDouble(element.replace("z:", ""));
            } else if (element.startsWith("yaw:")) {
                yaw = Float.parseFloat(element.replace("yaw:", ""));
            } else if (element.startsWith("pitch:")) {
                pitch = Float.parseFloat(element.replace("pitch:", ""));
            }
        }
        if (world == null || x == -1 || y == -1 || z == -1) {
            throw new NullPointerException(fieldboss.getId() + "-ID 필드 보스의 좌표가 잘못되었습니다. " + serializedLocation);
        }
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}