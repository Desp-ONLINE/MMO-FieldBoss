package com.binggre.mmofieldboss.gui;

import com.binggre.binggreapi.functions.HolderListener;
import com.binggre.binggreapi.functions.PageInventory;
import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.ItemManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.GUIConfig;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossRedis;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRedisRepository;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mmoplayerdata.config.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeGUI implements InventoryHolder, HolderListener, PageInventory {

    private final FieldBossRedisRepository redisRepository = MMOFieldBoss.getPlugin().getRedisRepository();

    public static void open(Player player) {
        TimeGUI timeGUI = new TimeGUI();
        timeGUI.player = player;
        player.openInventory(timeGUI.inventory);
        timeGUI.refresh();
    }

    private Player player;
    private int page = 1;
    private final Inventory inventory;

    private TimeGUI() {
        inventory = create();
        init();
    }

    private GUIConfig config() {
        return MMOFieldBoss.getPlugin().getGuiConfig();
    }

    private void init() {
        GUIConfig config = config();
        CustomItemStack previous = config.getPrevious();
        CustomItemStack next = config.getNext();
        inventory.setItem(previous.getSlot(), previous.getItemStack());
        inventory.setItem(next.getSlot(), next.getItemStack());
    }

    private Inventory create() {
        GUIConfig config = config();
        return Bukkit.createInventory(this, config.getSize() * 9, Component.text(config.getTitle()));
    }

    private void refresh() {
        inventory.clear();
        init();

        List<Integer> fieldBossIds = config().getPageIds().get(page);
        if (fieldBossIds == null) {
            return;
        }

        Map<Integer, List<FieldBossRedis>> groupedByBossId = redisRepository.values().stream()
                .filter(redis -> fieldBossIds.contains(redis.getFieldBossId()))
                .filter(redis -> redis.getPort() != 30066)
                .sorted(Comparator.comparing(FieldBossRedis::getPort))
                .collect(Collectors.groupingBy(FieldBossRedis::getFieldBossId));

        groupedByBossId.forEach((bossId, redisList) -> {
            CustomItemStack customItemStack = redisList.stream()
                    .map(FieldBossRedis::getCustomItemStack)
                    .findFirst()
                    .orElse(null);

            if (customItemStack == null) {
                return;
            }

            FieldBossRedis closestRedis = redisList.stream()
                    .min(Comparator.comparingLong(redis -> {
                        LocalDateTime now = LocalDateTime.now();
                        List<Integer> spawnHours = redis.getSpawnHours();
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
                        return Duration.between(now, nextSpawn).toMillis();
                    })).orElse(null);

            String timeString = getTimeString(closestRedis);

            List<String> lore = new ArrayList<>();
            lore.add(timeString);
            lore.add("");
            lore.add("§7 - §f" + getMyTime(bossId));

            ItemStack itemStack = customItemStack.getItemStack();
            ItemManager.setLore(itemStack, lore);
            ItemManager.setCustomModelData(itemStack, customItemStack.getCustomModelData());
            inventory.setItem(customItemStack.getSlot(), itemStack);
        });
    }

    private String getTimeString(FieldBossRedis fieldBossRedis) {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> spawnHours = fieldBossRedis.getSpawnHours();

        if (spawnHours == null || spawnHours.isEmpty()) {
            return "스폰 시간이 설정되어 있지 않습니다.";
        }

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

        Config playerDataConfig = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig();
        String serverName = playerDataConfig.getServerName(fieldBossRedis.getPort());

        if (hours == 0) {
            return String.format("§7다음 출현은 §f%d분 §7후에 §f%s §7에서 등장합니다.", minutes, serverName);
        } else {
            return String.format("§7다음 출현은 §f%d시간 %d분 §7후에 §f%s §7에서 등장합니다.", hours, minutes, serverName);
        }
    }


    @Override
    public void next() {
        Map<Integer, List<Integer>> pageIds = config().getPageIds();
        if (!pageIds.containsKey(page + 1)) {
            return;
        }
        page++;
        refresh();
    }


    @Override
    public void previous() {
        page = Math.max(1, page - 1);
        refresh();
    }

    @Override
    public int getNextSlot() {
        return config().getNext().getSlot();
    }

    @Override
    public int getPreviousSlot() {
        return config().getPrevious().getSlot();
    }

    @Override
    public int getPage() {
        return page;
    }

    private String getMyTime(int id) {
        PlayerFieldBoss playerFieldBoss = MMOFieldBoss.getPlugin()
                .getPlayerRepository().get(this.player.getUniqueId());
        FieldBoss fieldBoss = MMOFieldBoss.getPlugin()
                .getFieldBossRepository().get(id);

        PlayerJoinBoss join = playerFieldBoss.getJoin(id);
        long cooldownHour = join.getCooldownHour(fieldBoss);
        int lastJoinHour = join.getLastJoinTime().getHour();
        if (cooldownHour == 0) {
            return "§a처치에 관여할 수 있습니다.";
        }
        return String.format("§c%d시에 처치에 관여했기 때문에, %d시부터 처치할 수 있습니다.",
                lastJoinHour, lastJoinHour + fieldBoss.getInitRewardHour());
    }


    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if(event.getSlot() == 10){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add "+player.getName()+" fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove "+player.getName()+" fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove "+player.getName()+" fieldboss_hud_3");
        }
        if(event.getSlot() == 12){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove "+player.getName()+" fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add "+player.getName()+" fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " +player.getName()+" fieldboss_hud_3");
        }
        if(event.getSlot() == 14){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove "+player.getName()+" fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove "+player.getName()+" fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add "+player.getName()+" fieldboss_hud_3");
        }
    }

    @Override
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {

    }

    @Override
    public void onDrag(InventoryDragEvent inventoryDragEvent) {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}