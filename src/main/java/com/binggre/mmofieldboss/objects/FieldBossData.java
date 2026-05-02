package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.api.FieldBossDeathEvent;
import com.binggre.mmofieldboss.api.FieldBossDespawnEvent;
import com.binggre.mmofieldboss.api.FieldBossSpawnEvent;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import com.binggre.mmomail.MMOMail;
import com.binggre.mmomail.api.MailAPI;
import com.binggre.mmomail.objects.Mail;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mongolibraryplugin.base.MongoUpdatable;
import com.binggre.velocitysocketclient.VelocityClient;
import com.binggre.velocitysocketclient.listener.BroadcastComponentVelocityListener;
import com.binggre.velocitysocketclient.listener.BroadcastStringVelocityListener;
import com.google.gson.annotations.SerializedName;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FieldBossData {

    private static final PlayerRepository playerRepository = MMOFieldBoss.getPlugin().getPlayerRepository();
    private static final FieldBossRepository fieldBossRepository = MMOFieldBoss.getPlugin().getFieldBossRepository();
    private static final BukkitAPIHelper mythicMobAPI = new BukkitAPIHelper();
    private static final MetadataManager metadataManager = MMOFieldBoss.getPlugin().getMetadataManager();

    private int port;
    @SerializedName("location")
    private String serializedLocation;
    @SerializedName("pos1")
    private String serializedPos1;
    @SerializedName("pos2")
    private String serializedPos2;
    @SerializedName("arenaSpawn")
    private String serializedArenaSpawn;
    @SerializedName("exit")
    private String serializedExitLocation;
    private String lastSpawnedBossUUID;
    private List<Integer> spawnHours;

    private transient Location spawnLocation;
    private transient Location pos1;
    private transient Location pos2;
    private transient Location arenaSpawn;
    private transient Location exitLocation;
    private transient Entity spawnedBoss;
    private transient int task = -1;
    private transient FieldBoss fieldBoss;
    private transient BossSession session;

    public void init(FieldBoss fieldBoss) {
        this.fieldBoss = fieldBoss;
        FieldBossRepository repo = MMOFieldBoss.getPlugin().getFieldBossRepository();
        spawnLocation = repo.deserializeLocation(fieldBoss, serializedLocation);
        pos1 = serializedPos1 != null ? repo.deserializeLocation(fieldBoss, serializedPos1) : null;
        pos2 = serializedPos2 != null ? repo.deserializeLocation(fieldBoss, serializedPos2) : null;
        arenaSpawn = serializedArenaSpawn != null ? repo.deserializeLocation(fieldBoss, serializedArenaSpawn) : null;
        exitLocation = serializedExitLocation != null ? repo.deserializeLocation(fieldBoss, serializedExitLocation) : null;
        session = new BossSession(this);
        onInit();
    }

    public boolean isInArena(Location location) {
        if (pos1 == null || pos2 == null || location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        double x = location.getX(), y = location.getY(), z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private static final String HEX_GOLD = hex("FFD700");
    private static final String HEX_RED = hex("FF6B6B");
    private static final String HEX_ORANGE = hex("FFB347");
    private static final String HEX_SKY = hex("87CEEB");
    private static final String HEX_GREEN = hex("4EA25A");
    private static final String HEX_WHITE = hex("FFFFFF");
    private static final String HEX_GRAY = hex("B0B0B0");

    private static String hex(String code) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : code.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    private static String colorize(String s) {
        if (s == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<#([A-Fa-f0-9]{6})>").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(hex(m.group(1))));
        }
        m.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void broadcast() {
        if (spawnedBoss == null) {
            return;
        }
        String serverName = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(Bukkit.getPort());
        String msg = "\n " + HEX_RED + "⚔ " + HEX_GOLD + serverName + HEX_WHITE + " 에서 "
                + getBossDisplayNameString() + HEX_WHITE + " 필드 보스가 등장했습니다.\n";
        Bukkit.broadcastMessage(msg);
        VelocityClient.getInstance().getConnectClient().send(BroadcastStringVelocityListener.class, msg);
    }

    private void broadcastOpen() {
        String serverName = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(Bukkit.getPort());
        int prepareMinute = MMOFieldBoss.getPlugin().getFieldBossConfig().getPrepareMinute();
        String msg = "\n " + HEX_GREEN + "✦ " + HEX_GOLD + serverName + HEX_WHITE + " 에서 "
                + getBossDisplayNameString() + HEX_WHITE + " 필드 보스 입장이 시작되었습니다. "
                + HEX_GRAY + "(" + prepareMinute + "분 후 등장)\n";
        Bukkit.broadcastMessage(msg);
        VelocityClient.getInstance().getConnectClient().send(BroadcastStringVelocityListener.class, msg);
    }

    private String getBossDisplayNameString() {
        MythicMob mythicMob = mythicMobAPI.getMythicMob(fieldBoss.getMythicMob());
        if (mythicMob != null && mythicMob.getDisplayName() != null) {
            String s = mythicMob.getDisplayName().get();
            if (s != null && !s.isEmpty()) {
                return colorize(s);
            }
        }
        if (fieldBoss.getItemStack() != null) {
            ItemStack i = fieldBoss.getItemStack().getItemStack();
            if (i != null && i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
                return LegacyComponentSerializer.legacySection().serialize(i.getItemMeta().displayName());
            }
        }
        return fieldBoss.getMythicMob();
    }

    public void openSession() {
        if (session == null || arenaSpawn == null) {
            return;
        }
        session.open();
        broadcastOpen();
    }

    public void warnSoon() {
        if (session == null || !session.isOpening() || session.isWarned()) {
            return;
        }
        session.markWarned();
        broadcastSoon();
    }

    private void broadcastSoon() {
        String serverName = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(Bukkit.getPort());
        String msg = "\n " + HEX_ORANGE + "⚠ " + HEX_GOLD + serverName + HEX_WHITE + " 에서 "
                + getBossDisplayNameString() + HEX_WHITE + " 필드 보스가 "
                + HEX_RED + "1분 후" + HEX_WHITE + " 등장합니다!\n";
        Bukkit.broadcastMessage(msg);
        VelocityClient.getInstance().getConnectClient().send(BroadcastStringVelocityListener.class, msg);
    }

    private void broadcastKillAnnouncement() {
        String bossName = getBossDisplayNameString();
        String particle = josa(bossName);
        String msg = "\n " + HEX_RED + "⚔ " + bossName + HEX_WHITE + particle + " 처치되었습니다!\n";
        Bukkit.broadcastMessage(msg);
        VelocityClient.getInstance().getConnectClient().send(BroadcastStringVelocityListener.class, msg);
    }

    private String buildKillDetailsMessage(PlayerFieldBoss lastHit, PlayerFieldBoss best,
                                           List<ItemStack> lastHitItems, List<ItemStack> bestDamageItems, List<ItemStack> normalItems) {
        String bar = HEX_GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━";
        StringBuilder sb = new StringBuilder("\n");
        sb.append(bar).append("\n");
        sb.append("              ").append(HEX_RED).append("⚔ ")
                .append(getBossDisplayNameString()).append(HEX_GOLD).append(" 처치 ")
                .append(HEX_RED).append("⚔").append("\n");
        sb.append(bar).append("\n");

        if (lastHit != null) {
            sb.append(HEX_GRAY).append(" ▸ 최고 타격자  ")
                    .append(HEX_RED).append(lastHit.getNickname()).append("\n");
        }
        if (best != null) {
            double dmg = best.getJoin(fieldBoss.getId()).getDamage();
            sb.append(HEX_GRAY).append(" ▸ 최고 기여자  ")
                    .append(HEX_ORANGE).append(best.getNickname())
                    .append(HEX_GRAY).append("  (")
                    .append(HEX_WHITE).append(NumberUtil.applyComma(dmg))
                    .append(HEX_GRAY).append(")").append("\n");
        }

        sb.append("\n").append(HEX_GREEN).append(" ✦ 드랍 보상").append("\n");
        appendItemLines(sb, "막타", HEX_RED, lastHitItems);
        appendItemLines(sb, "최고 기여", HEX_ORANGE, bestDamageItems);
        appendItemLines(sb, "참여", HEX_SKY, normalItems);

        sb.append(bar).append("\n");
        return sb.toString();
    }

    private static String josa(String coloredName) {
        if (coloredName == null) {
            return "이(가)";
        }
        String stripped = coloredName
                .replaceAll("§x(§[0-9A-Fa-f]){6}", "")
                .replaceAll("§.", "")
                .replaceAll("<#[0-9A-Fa-f]{6}>", "")
                .replaceAll("&[0-9A-Fa-fklmnor]", "");
        if (stripped.isEmpty()) {
            return "이(가)";
        }
        char last = stripped.charAt(stripped.length() - 1);
        if (last >= 0xAC00 && last <= 0xD7A3) {
            return ((last - 0xAC00) % 28) == 0 ? "가" : "이";
        }
        return "이(가)";
    }

    private void appendItemLines(StringBuilder sb, String tag, String tagColor, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            String name;
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());
            } else {
                name = item.getType().name().toLowerCase().replace('_', ' ');
            }
            sb.append("   ").append(HEX_GRAY).append("[")
                    .append(tagColor).append(tag)
                    .append(HEX_GRAY).append("] ")
                    .append(name)
                    .append(HEX_WHITE).append(" ×").append(item.getAmount())
                    .append("\n");
        }
    }

    public void spawn() throws InvalidMobTypeException {
        Entity entity = mythicMobAPI.spawnMythicMob(fieldBoss.getMythicMob(), spawnLocation);

        metadataManager.setEntity(entity, BossKey.ID, fieldBoss.getId());
        metadataManager.setEntity(entity, BossKey.TIME, LocalDateTime.now().toString());
        spawnedBoss = entity;
        lastSpawnedBossUUID = entity.getUniqueId().toString();

        ActiveMob mythicMobInstance = mythicMobAPI.getMythicMobInstance(entity);
        mythicMobInstance.setDespawnMode(DespawnMode.NEVER);
        fieldBossRepository.update(fieldBoss, "lastSpawnedBossUUID", lastSpawnedBossUUID);

        java.util.logging.Logger log = MMOFieldBoss.getPlugin().getLogger();
        log.info("[spawn] mythic=" + fieldBoss.getMythicMob() + " session=" + (session != null) + " participants=" + (session != null ? session.getParticipants().size() : -1));
        if (session != null) {
            applyScaling(mythicMobInstance, session.getParticipants().size());
            session.onSpawned();
        }

        startScheduler();
        broadcast();

        FieldBossSpawnEvent event = new FieldBossSpawnEvent(fieldBoss);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void applyScaling(ActiveMob activeMob, int participantCount) {
        java.util.logging.Logger log = MMOFieldBoss.getPlugin().getLogger();
        log.info("[applyScaling] called participantCount=" + participantCount);

        int n = Math.max(1, participantCount);
        if (n <= 1) {
            log.info("[applyScaling] n<=1, skipping. n=" + n);
            return;
        }
        if (!(spawnedBoss instanceof LivingEntity living)) {
            log.info("[applyScaling] spawnedBoss is not LivingEntity. type=" + (spawnedBoss == null ? "null" : spawnedBoss.getClass().getName()));
            return;
        }
        if (living.isDead()) {
            log.info("[applyScaling] living is already dead, skipping");
            return;
        }
        FieldBossConfig cfg = MMOFieldBoss.getPlugin().getFieldBossConfig();
        double hpMultiplier = 1.0 + cfg.getHpScaleBase() * (n - 1);
        double dmgMultiplier = 1.0 + cfg.getDamageScaleBase() * (n - 1);
        log.info("[applyScaling] hpMultiplier=" + hpMultiplier + " dmgMultiplier=" + dmgMultiplier);

        double baseHealth = living.getMaxHealth();
        double totalHealth = baseHealth * hpMultiplier;
        log.info("[applyScaling] BEFORE setMaxHealth: baseHealth=" + baseHealth + " target totalHealth=" + totalHealth);

        living.setMaxHealth(totalHealth);
        living.setHealth(totalHealth);

        log.info("[applyScaling] AFTER setMaxHealth: maxHealth=" + living.getMaxHealth() + " health=" + living.getHealth());

        AttributeInstance dmgAttr = living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) {
            double oldDmg = dmgAttr.getBaseValue();
            dmgAttr.setBaseValue(oldDmg * dmgMultiplier);
            log.info("[applyScaling] damage: " + oldDmg + " -> " + dmgAttr.getBaseValue());
        } else {
            log.info("[applyScaling] dmgAttr is null");
        }

        Bukkit.getScheduler().runTaskLater(MMOFieldBoss.getPlugin(), () -> {
            if (spawnedBoss instanceof LivingEntity l && !l.isDead()) {
                log.info("[applyScaling +1t] maxHealth=" + l.getMaxHealth() + " health=" + l.getHealth());
            } else {
                log.info("[applyScaling +1t] boss gone/dead");
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(MMOFieldBoss.getPlugin(), () -> {
            if (spawnedBoss instanceof LivingEntity l && !l.isDead()) {
                log.info("[applyScaling +20t] maxHealth=" + l.getMaxHealth() + " health=" + l.getHealth());
            } else {
                log.info("[applyScaling +20t] boss gone/dead");
            }
        }, 20L);
    }

    public void onInit() {
        if (lastSpawnedBossUUID == null) {
            return;
        }
        Entity entity = spawnLocation.getWorld().getEntity(UUID.fromString(lastSpawnedBossUUID));
        if (entity == null) {
            lastSpawnedBossUUID = null;
            return;
        }
        spawnedBoss = entity;
        startScheduler();
    }

    public void startScheduler() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (spawnedBoss == null) {
                    cancel();
                    return;
                }
                if (spawnedBoss.isDead()) {
                    spawnedBoss = null;
                    if (session != null) {
                        session.complete();
                    }
                    cancel();
                    return;
                }
                String spawnTimeString = (String) metadataManager.getEntity(spawnedBoss, BossKey.TIME);
                if (spawnTimeString == null) {
                    cancelTask();
                    return;
                }
                LocalDateTime spawnTime = LocalDateTime.parse(Objects.requireNonNull(spawnTimeString));
                LocalDateTime now = LocalDateTime.now();

                if (Duration.between(spawnTime, now).toMinutes() >= fieldBoss.getDespawnMinute()) {
                    despawn();
                    cancel();
                }

                // 플레이어

            }
        }.runTaskTimer(MMOFieldBoss.getPlugin(), 0, 20).getTaskId();
    }

    public void despawn() {
        FieldBossDespawnEvent event = new FieldBossDespawnEvent(fieldBoss);
        Bukkit.getPluginManager().callEvent(event);

        if (spawnedBoss != null) {
            spawnedBoss.remove();
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mm m k " + fieldBoss.getMythicMob());
        spawnedBoss = null;
        if (session != null) {
            session.fail();
        }
    }

    public void cancelTask() {
        if (task == -1) {
            return;
        }
        Bukkit.getScheduler().cancelTask(task);
    }

    public void onKill(Player killer) {
        MailAPI mailAPI = MMOMail.getInstance().getMailAPI();
        String mailSender = "시스템";

        // 참여한 플레이어 필터링
        List<PlayerFieldBoss> validPlayers = playerRepository.values().stream()
                .filter(player -> {
                    Integer id = fieldBoss.getId();
                    PlayerJoinBoss join = player.getJoin(id);
                    Integer nowJoinedId = join.getNowJoinedId();
                    return nowJoinedId != null && nowJoinedId.equals(id) && !join.isAFK();
                })
                .collect(Collectors.toList());

        if (validPlayers.isEmpty()) {
            spawnedBoss = null;
            if (session != null) {
                session.complete();
            }
            return;
        }

        // 보상 아이템 1회 롤(브로드캐스트와 메일에 동일 결과 사용)
        Map<RewardType, FieldBossReward> rewards = fieldBoss.getRewards();
        List<ItemStack> lastHitItems = rewards.get(RewardType.LAST_HIT).getItemStacks(true);
        List<ItemStack> bestDamageItems = rewards.get(RewardType.BEST_DAMAGE).getItemStacks(true);
        List<ItemStack> normalItems = rewards.get(RewardType.NORMAL).getItemStacks(true);

        Mail lastHitMail = mailAPI.createMail(mailSender, "필드 보스 막타 보상", 0, lastHitItems);
        Mail bestDamageMail = mailAPI.createMail(mailSender, "필드 보스 최고 데미지 보상", 0, bestDamageItems);
        Mail normalMail = mailAPI.createMail(mailSender, "필드 보스 참여 보상", 0, normalItems);

        // 데미지 기준 내림차순 정렬
        validPlayers.sort(Comparator.comparingDouble(player -> -player.getJoin(fieldBoss.getId()).getDamage()));

        // 최고 데미지 플레이어
        PlayerFieldBoss bestDamagePlayer = validPlayers.getFirst();

        // 막타 플레이어 찾기
        PlayerFieldBoss lastHitPlayer = null;
        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            for (PlayerFieldBoss player : validPlayers) {
                if (player.getId().equals(killerId)) {
                    lastHitPlayer = player;
                    break;
                }
            }
        }

        // 전체 서버에 처치 알림 (간결)
        broadcastKillAnnouncement();

        // 보상 상세 정보(드랍/막타/최고기여)는 보상 대상자 본인에게만 발송
        String details = buildKillDetailsMessage(lastHitPlayer, bestDamagePlayer, lastHitItems, bestDamageItems, normalItems);
        for (PlayerFieldBoss player : validPlayers) {
            Player online = player.toPlayer();
            if (online != null && online.isOnline()) {
                online.sendMessage(details);
            }
        }

        if (lastHitPlayer != null) {
            mailAPI.sendMail(lastHitPlayer.getNickname(), lastHitMail);
        }
        mailAPI.sendMail(bestDamagePlayer.getNickname(), bestDamageMail);

        for (PlayerFieldBoss player : validPlayers) {
            mailAPI.sendMail(player.getNickname(), normalMail);
            player.getJoin(fieldBoss.getId()).completeJoin();
            playerRepository.save(player);
        }
        FieldBossDeathEvent event = new FieldBossDeathEvent(fieldBoss, lastHitPlayer, bestDamagePlayer, validPlayers);
        Bukkit.getPluginManager().callEvent(event);
        spawnedBoss = null;
        if (session != null) {
            session.complete();
        }

        // 보상 받은 온라인 유저는 /spawn 명령어 자동 실행 (세션 IDLE 전환 후 → 명령어 차단 해제됨)
        for (PlayerFieldBoss player : validPlayers) {
            Player online = player.toPlayer();
            if (online != null && online.isOnline()) {
                online.performCommand("spawn");
            }
        }
    }
}
