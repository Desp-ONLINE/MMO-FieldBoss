package com.binggre.mmofieldboss.api;

import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class FieldBossDespawnEvent extends FieldBossEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public FieldBossDespawnEvent(FieldBoss fieldBoss) {
        this.fieldBoss = fieldBoss;
        this.bossEntity = fieldBoss.getDataThisServer().getSpawnedBoss();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
