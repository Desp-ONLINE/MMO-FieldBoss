package com.binggre.mmofieldboss.scheduler;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class SpawnScheduler extends BukkitRunnable {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public void run() {
//        LocalDateTime now = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.of(2023, 1, 15, 21, 0, 0);
        int hour = now.getHour();

        for (FieldBoss fieldBoss : repository.values()) {
            fieldBoss.getData().forEach((port, fieldBossData) -> {
                if (Bukkit.getPort() != port) {
                    return;
                }
                if (fieldBossData.getSpawnedBoss() != null) {
                    return;
                }
                if (!fieldBossData.getSpawnHours().contains(hour)) {
                    return;
                }
                if (now.getMinute() != 0 || now.getSecond() != 0) {
                    return;
                }
                try {
                    fieldBossData.spawn();
                } catch (InvalidMobTypeException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
