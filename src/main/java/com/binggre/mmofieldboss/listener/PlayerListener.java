package com.binggre.mmofieldboss.listener;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.BossSession;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final PlayerRepository repository = MMOFieldBoss.getPlugin().getPlayerRepository();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerFieldBoss init = repository.onEnable(player);
        repository.putIn(init);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerFieldBoss remove = repository.remove(player.getUniqueId());
        if (remove != null) {
            for (PlayerJoinBoss value : remove.getJoinBoss().values()) {
                value.reset();
            }
            repository.save(remove);
        }
    }

    private static final String ARENA_WORLD_NAME = "raid";

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location lastDeath = event.getPlayer().getLastDeathLocation();
        if (lastDeath == null || lastDeath.getWorld() == null) {
            return;
        }
        if (!ARENA_WORLD_NAME.equals(lastDeath.getWorld().getName())) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        FieldBossData participating = findParticipating(id);
        if (participating == null) {
            return;
        }
        Location arenaSpawn = participating.getArenaSpawn();
        if (arenaSpawn != null) {
            event.setRespawnLocation(arenaSpawn);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        FieldBossData participating = findParticipatingInBattle(id);
        if (participating == null) {
            return;
        }
        String message = event.getMessage();
        String cmd = message.length() > 1 ? message.substring(1).split(" ")[0] : "";
        List<String> whitelist = MMOFieldBoss.getPlugin().getFieldBossConfig().getCommandWhitelist();
        if (whitelist != null && whitelist.contains(cmd)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("§c필드보스 토벌 중에는 해당 명령어를 사용할 수 없습니다.");
    }

    private FieldBossData findParticipating(UUID id) {
        for (FieldBoss boss : MMOFieldBoss.getPlugin().getFieldBossRepository().values()) {
            FieldBossData data = boss.getDataThisServer();
            if (data == null) {
                continue;
            }
            BossSession session = data.getSession();
            if (session != null && session.isParticipant(id)) {
                return data;
            }
        }
        return null;
    }

    private FieldBossData findParticipatingInBattle(UUID id) {
        FieldBossData data = findParticipating(id);
        if (data == null || data.getSession() == null || !data.getSession().isInBattle()) {
            return null;
        }
        return data;
    }
}