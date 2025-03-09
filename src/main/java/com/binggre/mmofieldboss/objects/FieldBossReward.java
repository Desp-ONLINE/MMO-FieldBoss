package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.utils.NumberUtil;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class FieldBossReward {

    @SerializedName("items")
    private List<byte[]> serializedItems;
    private Map<Double, ChanceReward> chanceItems;

    @Getter(AccessLevel.PRIVATE)
    private transient List<ItemStack> itemStacks;

    public void init() {
        deserialize();
    }

    public List<ItemStack> getItemStacks(boolean chanceItem) {
        List<ItemStack> itemStacks = new ArrayList<>(this.itemStacks);
        if (chanceItem) {
            chanceItems.forEach((chance, mmoRewardItem) -> {
                Type type = Type.get(mmoRewardItem.getType());
                if (type == null) {
                    return;
                }
                ItemStack item = MMOItems.plugin.getItem(type, mmoRewardItem.getId());
                if (item != null && NumberUtil.isPercentage(chance)) {
                    ItemStack clone = item.clone();
                    clone.setAmount(mmoRewardItem.getAmount());
                    itemStacks.add(clone);
                }
            });
        }

        return itemStacks;
    }

    public void setItems(Inventory inventory) {
        serializedItems = new ArrayList<>();
        itemStacks = new ArrayList<>();

        for (ItemStack itemStack : inventory) {
            if (itemStack != null) {
                itemStacks.add(itemStack);
                serializedItems.add(itemStack.serializeAsBytes());
            }
        }
    }

    public void deserialize() {
        itemStacks = new ArrayList<>();

        if (serializedItems == null) {
            return;
        }
        serializedItems.forEach(bytes -> itemStacks.add(ItemStack.deserializeBytes(bytes)));
    }
}