package com.binggre.mmofieldboss.gui;

import com.binggre.binggreapi.functions.HolderListener;
import com.binggre.binggreapi.functions.PageInventory;
import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.ItemManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.config.GUIConfig;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
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
import java.util.*;

public class TimeGUI implements InventoryHolder, HolderListener, PageInventory {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

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

        // 1. 해당 페이지의 보스 리스트 가져오기
        List<FieldBoss> list = fieldBossIds.stream()
                .map(repository::get)
                .filter(Objects::nonNull)
                .toList();

        // 2. 보스 리스트를 순회하며 아이템 배치
        for (FieldBoss fieldBoss : list) {
            // getTimeString 메서드에서 모든 채널(port)의 spawnHours를 계산하여 반환함
            String timeString = getTimeString(fieldBoss);

            List<String> lore = new ArrayList<>();
            lore.add(timeString);
            lore.add("");
            lore.add("§7 - §f" + getMyTime(fieldBoss));
            lore.add("");
            lore.add("§e  &n&o클릭하여 남은 필드 보스 쿨타임을 화면 왼쪽에 띄울 수 있습니다!");

            CustomItemStack customItemStack = fieldBoss.getItemStack();
            ItemStack itemStack = customItemStack.getItemStack().clone(); // 원본 보호를 위해 복제

            // 기존에 사용하시던 ItemManager 방식을 그대로 유지
            ItemManager.setLore(itemStack, lore);
            ItemManager.setCustomModelData(itemStack, customItemStack.getCustomModelData());

            inventory.setItem(customItemStack.getSlot(), itemStack);
        }
    }

    private String getTimeString(FieldBoss fieldBoss) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모든 채널(FieldBossData) 중에서 가장 빠른 미래의 스폰 시간을 가진 데이터를 찾습니다.
        // (시간, 데이터객체)를 함께 관리하기 위해 Optional을 활용합니다.
        var nextSpawnEntry = fieldBoss.getData().values().stream()
                .filter(data -> data.getSpawnHours() != null && !data.getSpawnHours().isEmpty())
                .flatMap(data -> data.getSpawnHours().stream()
                        .map(hour -> {
                            LocalDateTime candidate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
                            if (!candidate.isAfter(now)) {
                                candidate = candidate.plusDays(1);
                            }
                            // 추후 비교를 위해 데이터와 계산된 시간을 Pair(추상적)로 묶어 처리
                            return new java.util.AbstractMap.SimpleEntry<>(candidate, data);
                        })
                )
                .min(Comparator.comparing(java.util.AbstractMap.SimpleEntry::getKey));

        if (nextSpawnEntry.isEmpty()) {
            return "§7다음 출현 정보가 없습니다.";
        }

        // 2. 가장 가까운 시간과 해당 데이터 객체 추출
        LocalDateTime nextSpawnTime = nextSpawnEntry.get().getKey();
        FieldBossData closestData = nextSpawnEntry.get().getValue();

        // 3. 시간 차이 계산
        Duration duration = Duration.between(now, nextSpawnTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        // 4. 해당 데이터의 포트를 사용하여 서버 이름 가져오기
        String serverName = MMOPlayerDataPlugin.getInstance().getPlayerDataConfig().getServerName(closestData.getPort());

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

    private String getMyTime(FieldBoss fieldBoss) {
        PlayerFieldBoss playerFieldBoss = MMOFieldBoss.getPlugin().getPlayerRepository().get(this.player.getUniqueId());
        PlayerJoinBoss join = playerFieldBoss.getJoin(fieldBoss.getId());
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
        if (event.getSlot() == 10) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add " + player.getName() + " fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_3");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_4");
            player.sendMessage("§a 성공적으로 §f§n애쉬우드§a 보스 쿨타임을 표시했습니다.");
            player.closeInventory();
        }
        if (event.getSlot() == 12) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add " + player.getName() + " fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_3");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_5");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_4");
            player.sendMessage("§a 성공적으로 §f§n화무사§a 보스 쿨타임을 표시했습니다.");
            player.closeInventory();
        }
        if (event.getSlot() == 14) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add " + player.getName() + " fieldboss_hud_3");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_4");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_5");
            player.sendMessage("§a 성공적으로 §f§n아이스 워든§a 보스 쿨타임을 표시했습니다.");
            player.closeInventory();
        }
        if (event.getSlot() == 16) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_3");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_5");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add " + player.getName() + " fieldboss_hud_4");
            player.sendMessage("§a 성공적으로 §f§n테네브리스§a 보스 쿨타임을 표시했습니다.");
            player.closeInventory();
        }
        if (event.getSlot() == 28) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_2");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_3");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud remove " + player.getName() + " fieldboss_hud_4");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hud hud add " + player.getName() + " fieldboss_hud_5");
            player.sendMessage("§a 성공적으로 §f§n트와일라잇§a 보스 쿨타임을 표시했습니다.");
            player.closeInventory();
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