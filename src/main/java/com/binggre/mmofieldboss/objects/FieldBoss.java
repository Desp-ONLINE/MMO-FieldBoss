package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.api.FieldBossDeathEvent;
import com.binggre.mmofieldboss.api.FieldBossDespawnEvent;
import com.binggre.mmofieldboss.api.FieldBossSpawnEvent;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import com.binggre.mmomail.MMOMail;
import com.binggre.mmomail.api.MailAPI;
import com.binggre.mmomail.objects.Mail;
import com.binggre.mmoplayerdata.MMOPlayerDataPlugin;
import com.binggre.mongolibraryplugin.base.MongoData;
import com.binggre.velocitysocketclient.VelocityClient;
import com.binggre.velocitysocketclient.listener.BroadcastComponentVelocityListener;
import com.google.gson.annotations.SerializedName;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FieldBoss implements MongoData<Integer> {

    private int id;
    private String mythicMob;
    private int despawnMinute;
    private int initRewardHour;
    private CustomItemStack itemStack;
    private Map<RewardType, FieldBossReward> rewards;

    private Map<Integer, FieldBossData> data;

    @Override
    public Integer getId() {
        return id;
    }

    public FieldBossData getData(int port) {
        return data.get(port);
    }

    public FieldBossData getDataThisServer() {
        return data.get(Bukkit.getPort());
    }

    public void init() {
        rewards.values().forEach(FieldBossReward::init);
        data.forEach((integer, fieldBossData) -> {
            fieldBossData.init(this);
        });
    }
}