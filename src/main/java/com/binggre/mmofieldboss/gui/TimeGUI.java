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
import java.time.LocalTime;
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
        Inventory inventory = Bukkit.createInventory(this, config.getSize() * 9, Component.text(config.getTitle()));
        return inventory;
    }

    private void refresh() {
        inventory.clear();
        init();

        List<Integer> fieldBossIds = config().getPageIds().get(page);
        if (fieldBossIds == null) {
            return;
        }

        Config playerDataConfig = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig();

        Map<Integer, List<FieldBossRedis>> groupedByBossId = redisRepository.values().stream()
                .filter(redis -> fieldBossIds.contains(redis.getFieldBossId()))
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

            List<String> lore = redisList.stream()
                    .map(redis -> {
                        String serverName = playerDataConfig.getServerName(redis.getPort()).replace("던전 ", "§f");
                        return serverName + " : " + getTimeString(redis);
                    })
                    .collect(Collectors.toList());

            lore.add("");
            lore.add("§7 - §f" + getMyTime(bossId));

            ItemStack itemStack = customItemStack.getItemStack();
            ItemManager.setLore(itemStack, lore);
            ItemManager.setCustomModelData(itemStack, customItemStack.getCustomModelData());
            inventory.setItem(customItemStack.getSlot(), itemStack);
        });
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
        PlayerFieldBoss playerFieldBoss = MMOFieldBoss.getPlugin().getPlayerRepository().get(this.player.getUniqueId());
        FieldBoss fieldBoss = MMOFieldBoss.getPlugin().getFieldBossRepository().get(id);
        PlayerJoinBoss join = playerFieldBoss.getJoin(id);
        LocalDateTime lastJoinTime = join.getLastJoinTime();
        int lastJoinTimeHour = lastJoinTime.getHour();
        return lastJoinTimeHour + fieldBoss.getInitRewardHour() + "시 이후부터 해당 필드보스를 처치할 수 있습니다.";
    }

    private String getTimeString(FieldBossRedis fieldBossRedis) {
        StringBuilder builder = new StringBuilder("§f");
        LocalDateTime now = LocalDateTime.now();
        List<Integer> spawnHours = fieldBossRedis.getSpawnHours();
        int remainingMinutes = Integer.MAX_VALUE;
        int remainingHours = 0;
        for (Integer spawnHour : spawnHours) {
            LocalDateTime spawnTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(spawnHour.intValue(), 0));
            if (spawnTime.isBefore(now))
                spawnTime = spawnTime.plusDays(1L);
            Duration duration = Duration.between(now, spawnTime);
            long minutes = duration.toMinutes();
            if (minutes < remainingMinutes) {
                remainingMinutes = (int)minutes;
                remainingHours = remainingMinutes / 60;
            }
        }
        int id = fieldBossRedis.getFieldBossId();
        FieldBoss fieldBoss = MMOFieldBoss.getPlugin().getFieldBossRepository().get(id);
        builder.append("§a").append(spawnHours.getFirst()).append("§f시 부터 §a").append(fieldBoss.getInitRewardHour()).append(" §f시간 마다 등장합니다.");
        return builder.toString();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
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