package com.binggre.mmofieldboss;

import com.binggre.binggreapi.BinggrePlugin;
import com.binggre.binggreapi.utils.metadata.KeepMetadataManager;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.commands.AdminCommand;
import com.binggre.mmofieldboss.config.FieldBossConfig;
import com.binggre.mmofieldboss.listener.EntityListener;
import com.binggre.mmofieldboss.listener.PlayerListener;
import com.binggre.mmofieldboss.listener.velocity.BroadcastVelocityListener;
import com.binggre.mmofieldboss.listener.velocity.ReloadVelocityListener;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import com.binggre.mmofieldboss.scheduler.SpawnScheduler;
import com.binggre.velocitysocketclient.VelocityClient;
import com.binggre.velocitysocketclient.socket.SocketClient;
import lombok.Getter;

import java.util.HashMap;

@Getter
public final class MMOFieldBoss extends BinggrePlugin {

    @Getter
    private static MMOFieldBoss plugin;
    public static final String DATABASE_NAME = "MMO-FieldBoss";

    private final MetadataManager metadataManager = new KeepMetadataManager(this);
    private FieldBossConfig fieldBossConfig;
    private FieldBossRepository fieldBossRepository;
    private PlayerRepository playerRepository;

    @Override
    public void onEnable() {
        plugin = this;

        saveResource("example.json", true);

        fieldBossConfig = new FieldBossConfig(DATABASE_NAME, "Config");
        fieldBossConfig.init();

        playerRepository = new PlayerRepository(this, DATABASE_NAME, "Player", new HashMap<>());
        playerRepository.onEnable();

        fieldBossRepository = new FieldBossRepository();
        fieldBossRepository.init();

        executeCommand(this, new AdminCommand());
        registerEvents(this,
                new EntityListener(),
                new PlayerListener()
        );

        SocketClient socket = VelocityClient.getInstance().getConnectClient();
        socket.registerListener(ReloadVelocityListener.class);
        socket.registerListener(BroadcastVelocityListener.class);

        new SpawnScheduler().runTaskTimer(this, 0, 30L);

    }

    @Override
    public void onDisable() {
        fieldBossConfig.save();
        playerRepository.saveAll();
    }
}
