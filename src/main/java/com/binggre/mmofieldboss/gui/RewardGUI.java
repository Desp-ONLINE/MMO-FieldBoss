package com.binggre.mmofieldboss.gui;

import com.binggre.binggreapi.functions.HolderListener;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.RewardType;
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

public class RewardGUI implements InventoryHolder, HolderListener {

    public static void open(Player player, RewardType rewardType, int id) {
        RewardGUI rewardGUI = new RewardGUI(rewardType, id);
        player.openInventory(rewardGUI.inventory);
    }

    private final FieldBoss fieldBoss;
    private final RewardType rewardType;
    private final Inventory inventory;

    private RewardGUI(RewardType rewardType, int id) {
        this.rewardType = rewardType;
        fieldBoss = MMOFieldBoss.getPlugin().getFieldBossRepository().get(id);
        inventory = create(id);
    }

    private Inventory create(int id) {
        Inventory inventory = Bukkit.createInventory(this, 6 * 9, Component.text(id + " 보상 설정 (" + rewardType + ")"));

        int index = 0;
        for (ItemStack itemStack : fieldBoss.getRewards().get(rewardType).getItemStacks(false)) {
            inventory.setItem(index, itemStack);
            index++;
        }

        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof RewardGUI)) {
            return;
        }
        fieldBoss.getRewards().get(rewardType).setItems(inventory);
        MMOFieldBoss.getPlugin().getFieldBossRepository().save(fieldBoss);
    }

    @Override
    public void onDrag(InventoryDragEvent inventoryDragEvent) {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
