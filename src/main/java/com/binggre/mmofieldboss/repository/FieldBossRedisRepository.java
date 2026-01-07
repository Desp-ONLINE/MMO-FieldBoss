package com.binggre.mmofieldboss.repository;

import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossRedis;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mongolibraryplugin.base.MongoRedisRepository;
import com.binggre.velocitysocketclient.VelocityClient;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;

public class FieldBossRedisRepository extends MongoRedisRepository<String, FieldBossRedis> {

    public FieldBossRedisRepository(Plugin plugin, String database, String collection, String key, Class<FieldBossRedis> managementClass) {
        super(plugin, database, collection, key, managementClass);
    }

    @Override
    protected Jedis resource() {
        return VelocityClient.getInstance().getResource();
    }

    @Override
    public Document toDocument(FieldBossRedis fieldBossRedis) {
        return Document.parse(FileManager.toJson(fieldBossRedis));
    }

    @Override
    public FieldBossRedis toEntity(Document document) {
        return FileManager.toObject(document.toJson(), FieldBossRedis.class);
    }

    public void onEnable() {
        FieldBossRepository fieldBossRepository = MMOFieldBoss.getPlugin().getFieldBossRepository();
        for (FieldBoss fieldBoss : fieldBossRepository.values()) {
            String bossName = getBossName(fieldBoss.getMythicMob());

            FieldBossRedis redis = new FieldBossRedis(
                    toId(fieldBoss.getId()),
                    bossName,
                    fieldBoss.getSpawnHours(),
                    fieldBoss.getItemStack()
            );
            putIn(redis);
        }
    }

    public void onDisable() {
        FieldBossRepository fieldBossRepository = MMOFieldBoss.getPlugin().getFieldBossRepository();
        for (FieldBoss value : fieldBossRepository.values()) {
            remove(toId(value.getId()));
        }
    }

    public String toId(int id) {
        return id + "-" + Bukkit.getPort();
    }

    @Override
    public void drop() {
        clear();
        super.drop();
    }

    private String getBossName(String mythicMobId) {
        BukkitAPIHelper bukkitAPIHelper = new BukkitAPIHelper();
        MythicMob mythicMob = bukkitAPIHelper.getMythicMob(mythicMobId);
        return mythicMob.getDisplayName().get();
    }
}
