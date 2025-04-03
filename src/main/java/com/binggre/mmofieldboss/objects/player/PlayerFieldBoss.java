package com.binggre.mmofieldboss.objects.player;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mongolibraryplugin.base.MongoData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerFieldBoss implements MongoData<UUID> {

    private final UUID id;
    private String nickname;
    private final Map<Integer, PlayerJoinBoss> joinBoss;

    public PlayerFieldBoss(Player player) {
        this.id = player.getUniqueId();
        this.nickname = player.getName();
        this.joinBoss = new HashMap<>();
    }

    public Player toPlayer() {
        return Bukkit.getPlayer(id);
    }

    public void updateNickname(String name) {
        this.nickname = name;
        MMOFieldBoss.getPlugin().getPlayerRepository().update(this, "nickname", name);
    }

    public PlayerJoinBoss getJoin(int id) {
        return joinBoss.computeIfAbsent(id, integer -> new PlayerJoinBoss());
    }

    @Override
    public UUID getId() {
        return id;
    }
}