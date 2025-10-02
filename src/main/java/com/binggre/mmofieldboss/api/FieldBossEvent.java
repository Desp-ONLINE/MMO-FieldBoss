package com.binggre.mmofieldboss.api;

import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class FieldBossEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    protected FieldBoss fieldBoss;
    protected Entity bossEntity;

}