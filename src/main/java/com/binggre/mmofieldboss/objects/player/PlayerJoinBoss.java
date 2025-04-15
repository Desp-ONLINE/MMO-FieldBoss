package com.binggre.mmofieldboss.objects.player;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class PlayerJoinBoss {

    private double damage = 0;
    @Setter
    private Integer nowJoinedId;

    private LocalDateTime lastHitTime;
    private LocalDateTime lastJoinTime;

    public PlayerJoinBoss() {
        LocalDateTime temp = getResetTime();
        lastHitTime = temp;
        lastJoinTime = temp;
    }

    private LocalDateTime getResetTime() {
        return LocalDateTime.of(2000, 1, 1, 1, 1, 1);
    }

    private LocalDateTime normalizeTime(LocalDateTime time) {
        return time.withMinute(0).withSecond(0).withNano(0);
    }

    public long getCooldownHour(FieldBoss fieldBoss) {
        LocalDateTime now = normalizeTime(LocalDateTime.now());
        LocalDateTime normalizedLastJoinTime = normalizeTime(lastJoinTime);

        int initRewardHour = fieldBoss.getInitRewardHour();
        long elapsedHours = Duration.between(normalizedLastJoinTime, now).toHours();

        return Math.max(0, initRewardHour - elapsedHours);
    }

    public boolean isCooldown(FieldBoss fieldBoss) {
        LocalDateTime now = normalizeTime(LocalDateTime.now());
        LocalDateTime normalizedLastJoinTime = normalizeTime(lastJoinTime);

        int initRewardHour = fieldBoss.getInitRewardHour();
        return Duration.between(normalizedLastJoinTime, now).toHours() < initRewardHour;
    }


    public boolean isAFK() {
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        int afkSeconds = config.getAfkSeconds();

        long secondsDifference = Duration.between(this.lastHitTime, LocalDateTime.now()).getSeconds();
        return secondsDifference > afkSeconds;
    }

    public void addDamage(double damage) {
        this.damage += damage;
        lastHitTime = LocalDateTime.now();
    }

    public void cancelCompleteJoin() {
        reset();
        lastJoinTime = getResetTime();
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