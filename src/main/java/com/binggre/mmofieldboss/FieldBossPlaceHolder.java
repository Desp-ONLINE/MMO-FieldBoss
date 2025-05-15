package com.binggre.mmofieldboss;

import com.binggre.binggreapi.utils.ColorManager;
import com.binggre.mmofieldboss.objects.FieldBossRedis;
import com.binggre.mmofieldboss.repository.FieldBossRedisRepository;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FieldBossPlaceHolder extends PlaceholderExpansion {

    private final MMOFieldBoss plugin;

    public FieldBossPlaceHolder(MMOFieldBoss plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "FieldBoss";
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
        if (identifier.startsWith("Name")) {
            int id = Integer.parseInt(identifier.split("_")[1]);
            return plugin.getFieldBossRepository().get(id).getMythicMob();

            // %FieldBoss_Time_{id}%
            // %FieldBoss_Time_1%
        } else if (identifier.startsWith("Time")) {
            int id = Integer.parseInt(identifier.split("_")[1]);
            return getTime(id);


        } else if (identifier.startsWith("Channel")) {
            int id = Integer.parseInt(identifier.split("_")[1]);
            return getChannel(id);
        }
        return "";
    }

    private String getChannel(int id) {
        return getFieldBossData(id, this::extractChannel);
    }

    private String getTime(int id) {
        return getFieldBossData(id, this::extractTime);
    }

    private String getFieldBossData(int id, Function<FieldBossRedis, String> extractor) {
        FieldBossRedisRepository redisRepository = plugin.getRedisRepository();

        return redisRepository.values().stream()
                .filter(redis -> redis.getFieldBossId() == id)
                .filter(redis -> redis.getPort() != 30066)
                .collect(Collectors.groupingBy(FieldBossRedis::getFieldBossId))
                .values().stream()
                .flatMap(redisList -> redisList.stream()
                        .filter(redis -> redis.getCustomItemStack() != null)
                        .min(getRedisComparator())
                        .stream()
                        .map(extractor))
                .findFirst()
                .orElse("");
    }

    private Comparator<FieldBossRedis> getRedisComparator() {
        return Comparator.comparingLong(redis -> {
            LocalDateTime now = LocalDateTime.now();
            return redis.getSpawnHours().stream()
                    .map(hour -> calculateNextSpawn(hour, now))
                    .min(Comparator.comparingLong(candidate -> Duration.between(now, candidate).toMillis()))
                    .map(nextSpawn -> Duration.between(now, nextSpawn).toMillis())
                    .orElse(Long.MAX_VALUE);
        });
    }

    private LocalDateTime calculateNextSpawn(int hour, LocalDateTime now) {
        LocalDateTime candidate = now.withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
    }

    private String extractChannel(FieldBossRedis redis) {
        int port = redis.getPort();
        return MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(port);
    }

    private String extractTime(FieldBossRedis redis) {
        return getTimeString(redis);
    }

    public String getTimeString(FieldBossRedis fieldBossRedis) {
        List<Integer> spawnHours = fieldBossRedis.getSpawnHours();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSpawn = spawnHours.stream()
                .map(hour -> calculateNextSpawn(hour, now))
                .min(Comparator.comparingLong(candidate -> Duration.between(now, candidate).toMillis()))
                .orElse(now);

        Duration duration = Duration.between(now, nextSpawn);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        if (hours == 0 && minutes <= 3) {
            return ColorManager.format("<#FF8080>" + formattedTime);
        }
        return formattedTime;
    }
}