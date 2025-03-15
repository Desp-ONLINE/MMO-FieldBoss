package com.binggre.mmofieldboss.listener;

import com.binggre.binggreapi.functions.Callback;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.BossKey;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityListener implements Listener {

    private final PlayerRepository playerRepository = MMOFieldBoss.getPlugin().getPlayerRepository();
    private final FieldBossRepository fieldBossRepository = MMOFieldBoss.getPlugin().getFieldBossRepository();
    private final MetadataManager metadataManager = MMOFieldBoss.getPlugin().getMetadataManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity e1 = event.getDamager();
        if (!(e1 instanceof Player player)) {
            return;
        }
        Entity mythicMob = event.getEntity();

        logic(mythicMob, fieldBoss -> {
            int fieldBossId = fieldBoss.getId();

            PlayerFieldBoss playerFieldBoss = playerRepository.get(player.getUniqueId());
            PlayerJoinBoss playerJoinBoss = playerFieldBoss.getJoin(fieldBossId);

            if (playerJoinBoss.isCooldown(fieldBoss)) {
                event.setCancelled(true);
                return;
            }

            playerJoinBoss.setNowJoinedId(fieldBossId);
            playerJoinBoss.addDamage(event.getDamage());
        });
    }

    @EventHandler
    public void onDeath(MythicMobDeathEvent event) {
        LivingEntity killer = event.getKiller();
        if (!(killer instanceof Player player)) {
            return;
        }

        logic(event.getEntity(), fieldBoss -> {
            fieldBoss.onDeath(player);
        });
    }

    private void logic(Entity entity, Callback<FieldBoss> callback) {
        Object idObj = metadataManager.getEntity(entity, BossKey.ID);
        if (idObj == null) {
            return;
        }
        int id = (int) idObj;
        FieldBoss fieldBoss = fieldBossRepository.get(id);
        if (fieldBoss == null) {
            return;
        }
        callback.accept(fieldBoss);
    }
}