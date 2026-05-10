package com.binggre.mmofieldboss.objects.player;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Getter
public class PlayerJoinBoss {

    private double damage = 0;
    @Setter
    private Integer nowJoinedId;

    private LocalDateTime lastHitTime;
    private LocalDateTime lastJoinTime;
    private String killCountDate;
    private int todayKillCount;

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
        return getCooldownHour(fieldBoss, LocalDateTime.now());
    }

    public long getCooldownHour(FieldBoss fieldBoss, LocalDateTime referenceTime) {
        LocalDateTime ref = normalizeTime(referenceTime);
        LocalDateTime normalizedLastJoinTime = normalizeTime(lastJoinTime);

        int initRewardHour = fieldBoss.getInitRewardHour();
        long elapsedHours = Duration.between(normalizedLastJoinTime, ref).toHours();

        return Math.max(0, initRewardHour - elapsedHours);
    }

    public boolean isCooldown(FieldBoss fieldBoss) {
        return isCooldown(fieldBoss, LocalDateTime.now());
    }

    public boolean isCooldown(FieldBoss fieldBoss, LocalDateTime referenceTime) {
        LocalDateTime ref = normalizeTime(referenceTime);
        LocalDateTime normalizedLastJoinTime = normalizeTime(lastJoinTime);

        int initRewardHour = fieldBoss.getInitRewardHour();
        return Duration.between(normalizedLastJoinTime, ref).toHours() < initRewardHour;
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

    public int getTodayKillCount() {
        LocalDate stored = parseKillCountDate();
        if (stored == null || !stored.equals(LocalDate.now())) {
            return 0;
        }
        return todayKillCount;
    }

    private LocalDate parseKillCountDate() {
        if (killCountDate == null || killCountDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(killCountDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isDailyLimitReached(FieldBoss fieldBoss) {
        int limit = fieldBoss.getDailyLimit();
        if (limit <= 0) {
            return false;
        }
        return getTodayKillCount() >= limit;
    }

    public int getRemainingDaily(FieldBoss fieldBoss) {
        int limit = fieldBoss.getDailyLimit();
        if (limit <= 0) {
            return -1;
        }
        return Math.max(0, limit - getTodayKillCount());
    }

    public void cancelCompleteJoin() {
        reset();
        lastJoinTime = getResetTime();
    }

    public void completeJoin(LocalDateTime spawnTime) {
        reset();
        lastJoinTime = spawnTime != null ? spawnTime : LocalDateTime.now();

        LocalDate today = LocalDate.now();
        LocalDate stored = parseKillCountDate();
        if (stored == null || !stored.equals(today)) {
            killCountDate = today.toString();
            todayKillCount = 1;
        } else {
            todayKillCount++;
        }
    }

    public void resetDailyCount() {
        killCountDate = null;
        todayKillCount = 0;
    }

    public void reset() {
        damage = 0;
        nowJoinedId = null;
    }
}