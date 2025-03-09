package com.binggre.mmofieldboss.repository;

import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mongolibraryplugin.base.MongoCachedRepository;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

public class PlayerRepository extends MongoCachedRepository<UUID, PlayerFieldBoss> {

    public PlayerRepository(Plugin plugin, String database, String collection, Map<UUID, PlayerFieldBoss> cache) {
        super(plugin, database, collection, cache);
    }

    @Override
    public Document toDocument(PlayerFieldBoss playerFieldBoss) {
        return Document.parse(FileManager.toJson(playerFieldBoss));
    }

    @Override
    public PlayerFieldBoss toEntity(Document document) {
        return FileManager.toObject(document.toJson(), PlayerFieldBoss.class);
    }

    public PlayerFieldBoss init(Player player) {
        UUID id = player.getUniqueId();
        PlayerFieldBoss playerFieldBoss = findById(id);
        if (playerFieldBoss == null) {
            playerFieldBoss = new PlayerFieldBoss(player);
            save(playerFieldBoss);
        } else {
            playerFieldBoss.updateNickname(player.getName());
        }
        return playerFieldBoss;
    }

    public void onEnable() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerFieldBoss init = init(onlinePlayer);
            putIn(init);
        }
    }

    public void saveAll() {
        for (PlayerFieldBoss value : values()) {
            save(value);
        }
    }
}
