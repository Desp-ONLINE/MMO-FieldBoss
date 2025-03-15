package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.ItemManager;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.listener.velocity.BroadcastVelocityListener;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import com.binggre.mmomail.MMOMail;
import com.binggre.mmomail.api.MailAPI;
import com.binggre.mmomail.objects.Mail;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mmoplayerdata.objects.MMOPlayerData;
import com.binggre.velocitysocketclient.VelocityClient;
import com.google.gson.annotations.SerializedName;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FieldBoss {

    private static final PlayerRepository playerRepository = MMOFieldBoss.getPlugin().getPlayerRepository();
    private static final FieldBossRepository fieldBossRepository = MMOFieldBoss.getPlugin().getFieldBossRepository();
    private static final BukkitAPIHelper mythicMobAPI = new BukkitAPIHelper();
    private static final MetadataManager metadataManager = MMOFieldBoss.getPlugin().getMetadataManager();

    private int id;
    private String mythicMob;
    @SerializedName("location")
    private String serializedLocation;

    private int despawnMinute;
    private List<Integer> spawnHours;
    private int initRewardHour;
    private CustomItemStack itemStack;

    private Map<RewardType, FieldBossReward> rewards;
    private String lastSpawnedBossUUID;

    private transient Location spawnLocation;
    private transient Entity spawnedBoss;
    private transient int task = -1;

    public void init() {
        rewards.values().forEach(FieldBossReward::init);
        spawnLocation = MMOFieldBoss.getPlugin().getFieldBossRepository().deserializeLocation(this, serializedLocation);
    }

    private void broadcast() {
        if (spawnedBoss == null) {
            return;
        }
        String serverName = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(Bukkit.getPort());
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        String broadcastSpawn = config.getBroadcastSpawn()
                .replace("<channel>", serverName)
                .replace("<boss>", spawnedBoss.getName());

        Bukkit.broadcast(Component.text(broadcastSpawn));
        VelocityClient.getInstance().getConnectClient()
                .send(BroadcastVelocityListener.class, serverName, spawnedBoss.getName());
    }

    public void spawn() throws InvalidMobTypeException {
        Entity entity = mythicMobAPI.spawnMythicMob(mythicMob, spawnLocation);

        metadataManager.setEntity(entity, BossKey.ID, id);
        metadataManager.setEntity(entity, BossKey.TIME, LocalDateTime.now().toString());
        spawnedBoss = entity;
        lastSpawnedBossUUID = entity.getUniqueId().toString();

        ActiveMob mythicMobInstance = mythicMobAPI.getMythicMobInstance(entity);
        mythicMobInstance.setDespawnMode(DespawnMode.NEVER);
        fieldBossRepository.save(this);
        startScheduler();
        broadcast();
    }

    public void onInit() {
        if (lastSpawnedBossUUID == null) {
            return;
        }
        Entity entity = spawnLocation.getWorld().getEntity(UUID.fromString(lastSpawnedBossUUID));
        if (entity == null) {
            return;
        }
        spawnedBoss = entity;
        startScheduler();
    }

    public void startScheduler() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (spawnedBoss == null || spawnedBoss.isDead()) {
                    cancel();
                    return;
                }
                String spawnTimeString = (String) metadataManager.getEntity(spawnedBoss, BossKey.TIME);

                LocalDateTime spawnTime = LocalDateTime.parse(Objects.requireNonNull(spawnTimeString));
                LocalDateTime now = LocalDateTime.now();

                if (Duration.between(spawnTime, now).toMinutes() >= despawnMinute) {
                    despawn();
                    cancel();
                }

                // 플레이어

            }
        }.runTaskTimer(MMOFieldBoss.getPlugin(), 0, 20).getTaskId();
    }

    public void despawn() {
        spawnedBoss.remove();
    }

    public void cancelTask() {
        if (task == -1) {
            return;
        }
        Bukkit.getScheduler().cancelTask(task);
    }

    public static void broadcastLastHit(String killer, String bossName) {
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        String broadcast = config.getBroadcastLastHit()
                .replace("<player>", killer)
                .replace("<boss>", bossName);
        Bukkit.broadcast(Component.text(broadcast));
    }

    public void onDeath(Player killer) {
        FieldBossConfig config = MMOFieldBoss.getPlugin().getFieldBossConfig();
        MailAPI mailAPI = MMOMail.getInstance().getMailAPI();

        // 메일 미리 생성
        Map<RewardType, Mail> mailMap = new EnumMap<>(RewardType.class);

        String mailSender = config.getMailSender();

        mailMap.put(RewardType.LAST_HIT, mailAPI.createMail(mailSender, config.getLetterLastHit(), 0,
                rewards.get(RewardType.LAST_HIT).getItemStacks(true)));

        mailMap.put(RewardType.BEST_DAMAGE, mailAPI.createMail(mailSender, config.getLetterBestDamage(), 0,
                rewards.get(RewardType.BEST_DAMAGE).getItemStacks(true)));

        mailMap.put(RewardType.NORMAL, mailAPI.createMail(mailSender, config.getLetterNormal(), 0,
                rewards.get(RewardType.NORMAL).getItemStacks(true)));

        // 참여한 플레이어 필터링
        List<PlayerFieldBoss> validPlayers = playerRepository.values().stream()
                .filter(player -> {
                    PlayerJoinBoss join = player.getJoin(id);
                    Integer nowJoinedId = join.getNowJoinedId();
                    return nowJoinedId != null && nowJoinedId == id && !join.isAFK();
                })
                .collect(Collectors.toList());

        // 막타 플레이어 처리 (killer)
        Optional<PlayerFieldBoss> lastHitPlayer = validPlayers.stream()
                .filter(player -> player.getId().equals(killer.getUniqueId()))
                .findFirst();

        lastHitPlayer.ifPresent(player -> {
            String nickname = player.getNickname();
            String bossName = spawnedBoss.getName();
            broadcastLastHit(nickname, bossName);
            VelocityClient.getInstance().getConnectClient().send(BroadcastVelocityListener.class, nickname, bossName);
            mailAPI.sendMail(player.getNickname(), mailMap.get(RewardType.LAST_HIT));
            validPlayers.remove(player);

            player.getJoin(id).completeJoin();
            playerRepository.save(player);
        });

        if (validPlayers.isEmpty()) {
            return;
        }

        // 데미지 기준 내림차순 정렬
        validPlayers.sort(Comparator.comparing(player -> -player.getJoin(id).getDamage()));

        // 최고 데미지 플레이어 처리
        PlayerFieldBoss bestDamagePlayer = validPlayers.get(0);
        mailAPI.sendMail(bestDamagePlayer.getNickname(), mailMap.get(RewardType.BEST_DAMAGE));
        bestDamagePlayer.getJoin(id).completeJoin();
        playerRepository.save(bestDamagePlayer);
        validPlayers.remove(0);

        // 나머지 일반 참여자 처리
        Mail normalMail = mailMap.get(RewardType.NORMAL);
        validPlayers.forEach(player -> {
            mailAPI.sendMail(player.getNickname(), normalMail);
            player.getJoin(id).completeJoin();
            playerRepository.save(player);
        });
    }
}