package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.BossSession;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.BossSessionRedis;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.swlab.etcetera.Util.CommandUtil;

import javax.annotation.Nonnull;

public class EnterArgument implements CommandArgument {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public boolean execute(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String s, @Nonnull String[] args) {
        Player player = (Player) sender;
        int id = toInt(args[1]);
        if (id == NumberUtil.PARSE_ERROR) {
            player.sendMessage("§c숫자를 입력해 주세요.");
            return false;
        }
        FieldBoss fieldBoss = repository.get(id);
        if (fieldBoss == null) {
            player.sendMessage("§c존재하지 않는 필드보스입니다.");
            return false;
        }

        if (!player.isOp()) {
            int min = fieldBoss.getMinLevel();
            int max = fieldBoss.getMaxLevel();
            if (min > 0 || max > 0) {
                int playerMax = com.binggre.mmofieldboss.utils.LevelUtil.getMaxLevel(player);
                if (playerMax < 0) playerMax = 0;
                if (min > 0 && playerMax < min) {
                    player.sendMessage("§c레벨이 부족합니다. §7(§f" + rangeText(min, max) + "§7) §c보유 최대 §f" + playerMax);
                    return false;
                }
                if (max > 0 && playerMax > max) {
                    player.sendMessage("§c레벨이 너무 높습니다. §7(§f" + rangeText(min, max) + "§7) §c보유 최대 §f" + playerMax);
                    return false;
                }
            }
        }

        FieldBossData thisData = fieldBoss.getDataThisServer();
        if (thisData != null && thisData.getSession() != null) {
            BossSession session = thisData.getSession();
            if (session.isInBattle()) {
                player.sendMessage("§c필드보스가 이미 등장하여 입장할 수 없습니다.");
                return false;
            }
            if (session.isOpening()) {
                return enterLocal(player, fieldBoss, thisData);
            }
        }

        String openServer = BossSessionRedis.getOpenServer(id);
        if (openServer == null || openServer.isEmpty()) {
            player.sendMessage("§c아직 입장 시간이 아닙니다.");
            return false;
        }

        if (openServer.equals(BossSessionRedis.currentChannelName())) {
            player.sendMessage("§c아직 입장 시간이 아닙니다.");
            return false;
        }

        PlayerFieldBoss pf = MMOFieldBoss.getPlugin().getPlayerRepository().getOrCreate(player);
        PlayerJoinBoss join = pf.getJoin(fieldBoss.getId());
        if (join.isDailyLimitReached(fieldBoss)) {
            player.sendMessage("§c오늘은 이 필드보스를 모두 처치하셨습니다. §7(자정에 초기화)");
            return false;
        }

        CommandUtil.runCommandAsOP(player, "채널 워프 " + openServer + " fieldboss 입장 " + id);
        return true;
    }

    private static String rangeText(int min, int max) {
        if (min > 0 && max > 0) return min + " ~ " + max;
        if (min > 0) return min + " 이상";
        if (max > 0) return max + " 이하";
        return "제한 없음";
    }

    private boolean enterLocal(Player player, FieldBoss fieldBoss, FieldBossData data) {
        BossSession session = data.getSession();
        PlayerFieldBoss pf = MMOFieldBoss.getPlugin().getPlayerRepository().getOrCreate(player);
        PlayerJoinBoss join = pf.getJoin(fieldBoss.getId());
        if (join.isDailyLimitReached(fieldBoss)) {
            player.sendMessage("§c오늘은 이 필드보스를 모두 처치하셨습니다. §7(자정에 초기화)");
            return false;
        }
        if (session.enter(player)) {
            player.sendMessage("§a필드보스 토벌에 입장했습니다.");
            return true;
        }
        player.sendMessage("§c입장에 실패했습니다.");
        return false;
    }

    @Override
    public String getArg() {
        return "입장";
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public String getDescription() {
        return " [ID] - 필드보스에 입장합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.user.enter";
    }

    @Override
    public boolean onlyPlayer() {
        return true;
    }
}
