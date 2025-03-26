package com.binggre.mmofieldboss.api;

import com.binggre.mmofieldboss.objects.FieldBoss;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FieldBossDespawnEvent extends FieldBossEvent {

    private static final HandlerList handlers = new HandlerList();

    public FieldBossDespawnEvent(FieldBoss fieldBoss) {
        this.fieldBoss = fieldBoss;
        this.bossEntity = fieldBoss.getSpawnedBoss();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
