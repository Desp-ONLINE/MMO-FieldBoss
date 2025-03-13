package com.binggre.mmofieldboss.objects.player;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;

@Getter
public class PlayerJoinBoss {

    private double damage = 0;
    @Setter
    private Integer nowJoinedId;

    private LocalDateTime lastHitTime;
    private LocalDateTime lastJoinTime;

    public PlayerJoinBoss() {
        LocalDateTime temp = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        lastHitTime = temp;
        lastJoinTime = temp;
    }

    public boolean isCooldown(FieldBoss fieldBoss) {
        LocalDateTime now = LocalDateTime.now();
        int initRewardHour = fieldBoss.getInitRewardHour();

        return Duration.between(lastJoinTime, now).toHours() < initRewardHour;
    }

    public boolean isAFK() {
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        int afkSeconds = config.getAfkSeconds();

        long secondsDifference = Duration.between(this.lastHitTime, LocalDateTime.now()).getSeconds();
        return secondsDifference > afkSeconds;
    }


    public void addDamage(Player player, double damage) {
        this.damage += damage;
        lastHitTime = LocalDateTime.now();
    }

    public void completeJoin() {
        reset();
        lastJoinTime = LocalDateTime.now();
    }

    public void reset() {
        damage = 0;
        nowJoinedId = null;
    }
}