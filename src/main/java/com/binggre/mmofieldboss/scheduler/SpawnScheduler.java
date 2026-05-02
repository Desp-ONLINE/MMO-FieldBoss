package com.binggre.mmofieldboss.scheduler;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.BossSession;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;

public class SpawnScheduler extends BukkitRunnable {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        int prepareMinute = config.getPrepareMinute();
        int openMinute = (60 - prepareMinute) % 60;
        int upcomingHour = (now.getHour() + 1) % 24;

        for (FieldBoss fieldBoss : repository.values()) {
            fieldBoss.getData().forEach((port, fieldBossData) -> {
                if (Bukkit.getPort() != port) {
                    return;
                }
                tryOpen(now, openMinute, upcomingHour, fieldBossData);
                tryWarn(now, prepareMinute, fieldBossData);
                trySpawn(now, fieldBossData);
            });
        }
    }

    private void tryWarn(LocalDateTime now, int prepareMinute, FieldBossData data) {
        BossSession session = data.getSession();
        if (session == null || !session.isOpening() || session.isWarned()) {
            return;
        }
        if (data.getSpawnedBoss() != null) {
            return;
        }
        if (session.getOpenedAt() == null) {
            return;
        }
        long warnAfterSeconds = Math.max(0, prepareMinute - 1) * 60L;
        long elapsed = Duration.between(session.getOpenedAt(), now).getSeconds();
        if (elapsed < warnAfterSeconds) {
            return;
        }
        data.warnSoon();
    }

    private void tryOpen(LocalDateTime now, int openMinute, int upcomingHour, FieldBossData data) {
        BossSession session = data.getSession();
        if (session == null) {
            return;
        }
        if (session.getState() != BossSession.State.IDLE) {
            return;
        }
        if (data.getSpawnedBoss() != null) {
            return;
        }
        if (data.getSpawnHours() == null || !data.getSpawnHours().contains(upcomingHour)) {
            return;
        }
        if (now.getMinute() != openMinute || now.getSecond() != 0) {
            return;
        }
        data.openSession();
    }

    private void trySpawn(LocalDateTime now, FieldBossData data) {
        if (data.getSpawnedBoss() != null) {
            return;
        }
        if (data.getSpawnHours() == null || !data.getSpawnHours().contains(now.getHour())) {
            return;
        }
        if (now.getMinute() != 0 || now.getSecond() != 0) {
            return;
        }
        try {
            data.spawn();
        } catch (InvalidMobTypeException e) {
            e.printStackTrace();
        }
    }
}
