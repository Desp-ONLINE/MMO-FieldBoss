package com.binggre.mmofieldboss.objects;

import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.BossSessionRedis;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class BossSession {

    public enum State { IDLE, OPENING, IN_BATTLE, ENDED }

    private final FieldBossData data;
    private State state = State.IDLE;
    private final Set<UUID> participants = new HashSet<>();
    private LocalDateTime openedAt;
    private LocalDateTime spawnedAt;
    private boolean warned = false;

    public BossSession(FieldBossData data) {
        this.data = data;
    }

    public boolean isOpening() {
        return state == State.OPENING;
    }

    public boolean isInBattle() {
        return state == State.IN_BATTLE;
    }

    public boolean isParticipant(UUID id) {
        return participants.contains(id);
    }

    public void open() {
        if (state == State.OPENING || state == State.IN_BATTLE) {
            return;
        }
        state = State.OPENING;
        openedAt = LocalDateTime.now();
        participants.clear();
        warned = false;

        BossSessionRedis.setOpen(data.getFieldBoss().getId(), BossSessionRedis.currentChannelName());
    }

    public void markWarned() {
        warned = true;
    }

    public boolean enter(Player player) {
        if (state != State.OPENING) {
            return false;
        }
        Location dest = data.getArenaSpawn();
        if (dest == null) {
            return false;
        }
        participants.add(player.getUniqueId());

        PlayerRepository playerRepository = MMOFieldBoss.getPlugin().getPlayerRepository();
        PlayerFieldBoss playerFieldBoss = playerRepository.getOrCreate(player);
        PlayerJoinBoss joinBoss = playerFieldBoss.getJoin(data.getFieldBoss().getId());
        joinBoss.setNowJoinedId(data.getFieldBoss().getId());

        player.teleport(dest);
        return true;
    }

    public void onSpawned() {
        state = State.IN_BATTLE;
        spawnedAt = LocalDateTime.now();
        BossSessionRedis.clearOpen(data.getFieldBoss().getId());
    }

    public void fail() {
        if (state == State.IDLE) {
            return;
        }
        BossSessionRedis.clearOpen(data.getFieldBoss().getId());

        PlayerRepository playerRepository = MMOFieldBoss.getPlugin().getPlayerRepository();
        int bossId = data.getFieldBoss().getId();

        for (UUID id : participants) {
            PlayerFieldBoss playerFieldBoss = playerRepository.get(id);
            if (playerFieldBoss != null) {
                PlayerJoinBoss join = playerFieldBoss.getJoin(bossId);
                join.reset();
                playerRepository.save(playerFieldBoss);
            }

            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                continue;
            }
            p.sendMessage("§c필드보스 토벌에 실패하여 로비로 이동되었습니다.");
            p.performCommand("spawn");
        }
        reset();
    }

    public void complete() {
        BossSessionRedis.clearOpen(data.getFieldBoss().getId());
        reset();
    }

    private void reset() {
        participants.clear();
        state = State.IDLE;
        openedAt = null;
        spawnedAt = null;
        warned = false;
    }
}
