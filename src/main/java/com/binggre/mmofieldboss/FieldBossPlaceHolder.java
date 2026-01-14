package com.binggre.mmofieldboss;

import com.binggre.binggreapi.utils.ColorManager;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.repository.FieldBossRepository; // 패키지 경로에 맞춰 수정하세요
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        // %FieldBoss_Name_{id}%
        if (identifier.startsWith("Name")) {
            return getBossId(identifier).map(id -> {
                FieldBoss fb = plugin.getFieldBossRepository().get(id);
                return fb != null ? fb.getMythicMob() : "";
            }).orElse("");
        }

        // %FieldBoss_Time_{id}%
        if (identifier.startsWith("Time")) {
            return getBossId(identifier).map(this::getTime).orElse("");
        }

        // %FieldBoss_Channel_{id}%
        if (identifier.startsWith("Channel")) {
            return getBossId(identifier).map(this::getChannel).orElse("");
        }

        return "";
    }

    private Optional<Integer> getBossId(String identifier) {
        try {
            String[] split = identifier.split("_");
            if (split.length < 2) return Optional.empty();
            return Optional.of(Integer.parseInt(split[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String getChannel(int id) {
        FieldBoss fieldBoss = plugin.getFieldBossRepository().get(id);
        if (fieldBoss == null) return "";

        // 가장 가까운 스폰 시간을 가진 포트(채널) 찾기
        return findNextSpawnData(fieldBoss)
                .map(targetData -> {
                    // targetData는 포트 번호가 필드에 없으므로, 역으로 찾아야 합니다.
                    // 만약 FieldBossData에 port 필드를 추가하셨다면 targetData.getPort()를 쓰면 됩니다.
                    int port = fieldBoss.getData().entrySet().stream()
                            .filter(e -> e.getValue().equals(targetData))
                            .map(java.util.Map.Entry::getKey)
                            .findFirst().orElse(0);
                    return MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(port);
                }).orElse("");
    }

    private String getTime(int id) {
        FieldBoss fieldBoss = plugin.getFieldBossRepository().get(id);
        if (fieldBoss == null) return "";

        return findNextSpawnData(fieldBoss)
                .map(this::getTimeString)
                .orElse("");
    }

    // 모든 채널(FieldBossData) 중 가장 빨리 열리는 데이터를 찾는 핵심 로직
    private Optional<FieldBossData> findNextSpawnData(FieldBoss fieldBoss) {
        LocalDateTime now = LocalDateTime.now();
        return fieldBoss.getData().values().stream()
                .filter(data -> data.getSpawnHours() != null && !data.getSpawnHours().isEmpty())
                .min(Comparator.comparingLong(data -> {
                    LocalDateTime next = getNextSpawnTime(data.getSpawnHours(), now);
                    return Duration.between(now, next).toMillis();
                }));
    }

    private LocalDateTime getNextSpawnTime(List<Integer> spawnHours, LocalDateTime now) {
        return spawnHours.stream()
                .map(hour -> calculateNextSpawn(hour, now))
                .min(Comparator.naturalOrder())
                .orElse(now);
    }

    private LocalDateTime calculateNextSpawn(int hour, LocalDateTime now) {
        LocalDateTime candidate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
    }

    public String getTimeString(FieldBossData data) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSpawn = getNextSpawnTime(data.getSpawnHours(), now);

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