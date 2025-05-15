package com.binggre.mmofieldboss;

import com.binggre.mmofieldboss.objects.FieldBossRedis;
import com.binggre.mmofieldboss.repository.FieldBossRedisRepository;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FieldBossPlaceHolder extends PlaceholderExpansion {

    private final MMOFieldBoss plugin;

    public FieldBossPlaceHolder(MMOFieldBoss plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "MMOFieldBoss";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "binggre";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    //보스 이름, 남은 시간, 등장 위치
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            // %FieldBoss_Name_{id}%
        if (identifier.startsWith("FieldBoss_Name_")) {
            int id = Integer.parseInt(identifier.split("_")[2]);
            return plugin.getFieldBossRepository().get(id).getMythicMob();

            // %FieldBoss_Time_{id}_{channel}%
            // %FieldBoss_Time_1_던전1%
        } else if (identifier.startsWith("FieldBoss_Time_")) {
            String[] split = identifier.split("_");
            int id = Integer.parseInt(split[2]);
            String channel = split[3];
            FieldBossRedisRepository redisRepository = plugin.getRedisRepository();
            for (FieldBossRedis value : redisRepository.values()) {
                if (!value.getChannel().equals(channel)) {
                    continue;
                }
                if (value.getFieldBossId() != id) {
                    continue;
                }
                return value.getTimeString();
            }
            return "";
        }
        return "";
    }
}