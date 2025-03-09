package com.binggre.mmofieldboss.listener;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerRepository repository = MMOFieldBoss.getPlugin().getPlayerRepository();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerFieldBoss init = repository.init(player);
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
}