package com.binggre.mmofieldboss.repository;

import com.binggre.binggreapi.functions.Callback;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mongolibraryplugin.base.MongoCachedRepository;
import com.binggre.mongolibraryplugin.base.MongoData;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldBossRepository extends MongoCachedRepository<Integer, FieldBoss> {

    public static final String DIRECTORY = MMOFieldBoss.getPlugin().getDataFolder().getPath();

    public FieldBossRepository(Plugin plugin, String database, String collection, Map<Integer, FieldBoss> cache) {
        super(plugin, database, collection, cache);
    }

    @Override
    public Document toDocument(FieldBoss fieldBoss) {
        return Document.parse(FileManager.toJson(fieldBoss));
    }

    @Override
    public FieldBoss toEntity(Document document) {
        return FileManager.toObject(document.toJson(), FieldBoss.class);
    }

    public void onEnable() {
        List<FieldBoss> fieldBossList = findAll();
        for (FieldBoss fieldBoss : fieldBossList) {
            fieldBoss.init();
            putIn(fieldBoss);
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