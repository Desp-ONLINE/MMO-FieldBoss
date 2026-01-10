package com.binggre.mmofieldboss.scheduler;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class SpawnScheduler extends BukkitRunnable {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();
    private int lastHour = -1;
    private final Set<Integer> spawnedThisHour = new HashSet<>();

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        if (hour != lastHour) {
            spawnedThisHour.clear();
            lastHour = hour;
        }

        for (FieldBoss fieldBoss : repository.values()) {
            if (fieldBoss.getSpawnedBoss() != null) {
                spawnedThisHour.add(fieldBoss.getId());
                continue;
            }
            if (!fieldBoss.getSpawnHours().contains(hour)) {
                continue;
            }
            if (spawnedThisHour.contains(fieldBoss.getId())) {
                continue;
            }
            if (now.getMinute() != 0 || now.getSecond() != 0) {
                continue;
            }
            try {
                fieldBoss.spawn();
                spawnedThisHour.add(fieldBoss.getId());
            } catch (InvalidMobTypeException e) {
                e.printStackTrace();
            }
        }
    }
}
