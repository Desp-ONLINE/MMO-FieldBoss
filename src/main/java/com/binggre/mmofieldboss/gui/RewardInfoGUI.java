package com.binggre.mmofieldboss.gui;

import com.binggre.binggreapi.functions.HolderListener;
import com.binggre.binggreapi.utils.ColorManager;
import com.binggre.mmofieldboss.objects.ChanceReward;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossReward;
import com.binggre.mmofieldboss.objects.RewardType;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class RewardInfoGUI implements InventoryHolder, HolderListener {

    private final Inventory inventory;

    public static void open(Player player, FieldBoss fieldBoss) {
        RewardInfoGUI gui = new RewardInfoGUI(fieldBoss);
        player.openInventory(gui.inventory);
    }

    private RewardInfoGUI(FieldBoss fieldBoss) {
        String bossName = ColorManager.format(fieldBoss.getItemStack().getDisplayName());
        this.inventory = Bukkit.createInventory(this, 54, Component.text(bossName + " §8- 보상 정보"));
        init(fieldBoss);
    }

    private void init(FieldBoss fieldBoss) {
        inventory.setItem(0, createHeader(Material.LIME_STAINED_GLASS_PANE,
                "#7CFC00일반 보상",
                "#AAAAAA보스 처치에 참여한 모든",
                "#AAAAAA플레이어가 받는 보상입니다."));
        placeRewards(fieldBoss, RewardType.NORMAL, 1, 17);

        inventory.setItem(18, createHeader(Material.ORANGE_STAINED_GLASS_PANE,
                "#FFA500막타 보상",
                "#AAAAAA보스에게 마지막 일격을",
                "#AAAAAA가한 플레이어가 받는 보상입니다."));
        placeRewards(fieldBoss, RewardType.LAST_HIT, 19, 35);

        inventory.setItem(36, createHeader(Material.RED_STAINED_GLASS_PANE,
                "#FF6060최다 피해 보상",
                "#AAAAAA보스에게 가장 많은 피해를",
                "#AAAAAA입힌 플레이어가 받는 보상입니다."));
        placeRewards(fieldBoss, RewardType.BEST_DAMAGE, 37, 48);

        inventory.setItem(49, createHeader(Material.ARROW, "#FFFFFF뒤로가기"));
    }

    private ItemStack createHeader(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorManager.format(name));
        if (loreLines.length > 0) {
            meta.setLore(List.of(loreLines).stream().map(ColorManager::format).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private void placeRewards(FieldBoss fieldBoss, RewardType rewardType, int startSlot, int maxSlot) {
        FieldBossReward reward = fieldBoss.getRewards().get(rewardType);
        if (reward == null) {
            return;
        }

        int slot = startSlot;
        List<ItemStack> items = reward.getItemStacks(false);
        for (ItemStack item : items) {
            if (slot > maxSlot) {
                break;
            }
            inventory.setItem(slot, item.clone());
            slot++;
        }

        Map<Double, ChanceReward> chanceItems = reward.getChanceItems();
        if (chanceItems != null) {
            for (Map.Entry<Double, ChanceReward> entry : chanceItems.entrySet()) {
                if (entry.getKey() < 100.0) continue;
                slot = placeChanceReward(entry.getKey(), entry.getValue(), slot, maxSlot);
            }
            for (Map.Entry<Double, ChanceReward> entry : chanceItems.entrySet()) {
                if (entry.getKey() >= 100.0) continue;
                slot = placeChanceReward(entry.getKey(), entry.getValue(), slot, maxSlot);
            }
        }
    }

    private int placeChanceReward(double chance, ChanceReward chanceReward, int slot, int maxSlot) {
        if (slot > maxSlot) {
            return slot;
        }
        Type type = Type.get(chanceReward.getType());
        if (type == null) {
            return slot;
        }
        ItemStack item = MMOItems.plugin.getItem(type, chanceReward.getId());
        if (item == null) {
            return slot;
        }

        ItemStack display = item.clone();
        display.setAmount(chanceReward.getAmount());
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String originalName = meta.getDisplayName();
            String suffix = chance >= 100.0
                    ? ColorManager.format(" #AAAAAA(확정 보상)")
                    : ColorManager.format(" #AAAAAA&o(확률 보상)");
            meta.setDisplayName(originalName + suffix);
            display.setItemMeta(meta);
        }
        inventory.setItem(slot, display);
        return slot + 1;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlot() == 49) {
            Player player = (Player) event.getWhoClicked();
            TimeGUI.open(player);
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
