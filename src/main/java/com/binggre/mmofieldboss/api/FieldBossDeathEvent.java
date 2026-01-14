package com.binggre.mmofieldboss.api;

import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class FieldBossDeathEvent extends FieldBossEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final PlayerFieldBoss killer;
    private final PlayerFieldBoss bestDamager;
    private final List<PlayerFieldBoss> damagers;

    public FieldBossDeathEvent(FieldBoss fieldBoss, PlayerFieldBoss killer, PlayerFieldBoss bestDamager, List<PlayerFieldBoss> damagers) {
        this.fieldBoss = fieldBoss;
        this.bossEntity = fieldBoss.getDataThisServer().getSpawnedBoss();
        this.killer = killer;
        this.bestDamager = bestDamager;
        this.damagers = damagers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}