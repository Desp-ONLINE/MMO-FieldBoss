package com.binggre.mmofieldboss.scheduler;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;

public class SpawnScheduler extends BukkitRunnable {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        for (FieldBoss fieldBoss : repository.values()) {
            if (fieldBoss.getSpawnedBoss() != null) {
                continue;
            }
            if (!fieldBoss.getSpawnHours().contains(hour)) {
                continue;
            }
            if (now.getMinute() != 0 || now.getSecond() != 0) {
                continue;
            }
            try {
                fieldBoss.spawn();
            } catch (InvalidMobTypeException e) {
                e.printStackTrace();
            }
        }
    }
}
