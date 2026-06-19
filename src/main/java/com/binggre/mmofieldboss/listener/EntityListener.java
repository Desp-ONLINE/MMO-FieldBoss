package com.binggre.mmofieldboss.listener;

import com.binggre.binggreapi.functions.Callback;
import com.binggre.binggreapi.utils.metadata.MetadataManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.BossKey;
import com.binggre.mmofieldboss.objects.BossSession;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
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
            FieldBossData data = fieldBoss.getDataThisServer();
            BossSession session = data != null ? data.getSession() : null;
            if (session != null && !session.isParticipant(player.getUniqueId())) {
                session.getParticipants().add(player.getUniqueId());
            }

            PlayerFieldBoss playerFieldBoss = playerRepository.getOrCreate(player);
            PlayerJoinBoss playerJoinBoss = playerFieldBoss.getJoin(fieldBoss.getId());
            playerJoinBoss.setNowJoinedId(fieldBoss.getId());
            playerJoinBoss.addDamage(event.getDamage());
        });
    }

    @EventHandler
    public void onDeath(MythicMobDeathEvent event) {
        // 막타가 도트/스킬/소환수 데미지면 getKiller()가 null/비플레이어가 될 수 있음.
        // 이 경우에도 처치는 처리되어야 하므로 player == null 로 onKill 진입 (막타는 최근 타격자로 대체).
        LivingEntity killer = event.getKiller();
        Player player = (killer instanceof Player p) ? p : null;

        logic(event.getEntity(), fieldBoss -> {
            fieldBoss.getDataThisServer().onKill(player);
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