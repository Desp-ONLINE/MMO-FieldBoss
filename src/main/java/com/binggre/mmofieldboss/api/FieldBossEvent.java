package com.binggre.mmofieldboss.api;

import com.binggre.mmofieldboss.objects.FieldBoss;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

@Getter
public abstract class FieldBossEvent extends Event {

    protected FieldBoss fieldBoss;
    protected Entity bossEntity;

}