package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.ColorManager;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mmoplayerdata.config.Config;
import com.binggre.mongolibraryplugin.base.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
public class FieldBossRedis implements MongoData<String> {

    private String id;
    private String channel;
    private String bossName;
    private List<Integer> spawnHours;
    private CustomItemStack customItemStack;

    public int getFieldBossId() {
        return Integer.parseInt(id.split("-")[0]);
    }

    public int getPort() {
        return Integer.parseInt(id.split("-")[1]);
    }

    public String getTimeString() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSpawn = spawnHours.stream()
                .map(hour -> {
                    LocalDateTime candidate = now.withHour(hour)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    if (!candidate.isAfter(now)) {
                        candidate = candidate.plusDays(1);
                    }
                    return candidate;
                })
                .min(Comparator.comparingLong(candidate -> Duration.between(now, candidate).toMillis()))
                .orElse(now);

        Duration duration = Duration.between(now, nextSpawn);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours == 0) {
            if (minutes <= 3) {
                return ColorManager.format(String.format("<#FF8080>00:%s::%s", minutes, seconds));
            }
            return String.format("00:%s::%s", minutes, seconds);
        } else {
            return ColorManager.format(String.format("%s::%s::%s", hours, minutes, seconds));
        }
    }

    @Override
    public String getId() {
        return id;
    }
}