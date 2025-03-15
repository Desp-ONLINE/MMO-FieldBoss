package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerFieldBoss;
import com.binggre.mmofieldboss.objects.player.PlayerJoinBoss;
import com.binggre.mmofieldboss.repository.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CooldownResetArgument implements CommandArgument {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("오프라인 플레이어입니다.");
            return false;
        }
        int id = toInt(args[2]);
        if (id == NumberUtil.PARSE_ERROR) {
            sender.sendMessage("숫자를 입력해 주세요");
            return false;
        }
        PlayerRepository repository = MMOFieldBoss.getPlugin().getPlayerRepository();
        PlayerFieldBoss playerFieldBoss = repository.get(target.getUniqueId());
        PlayerJoinBoss join = playerFieldBoss.getJoin(id);
        join.cancelCompleteJoin();

        repository.save(playerFieldBoss);
        sender.sendMessage("초기화 완료");
        return false;
    }

    @Override
    public String getArg() {
        return "보상초기화";
    }

    @Override
    public int length() {
        return 3;
    }

    @Override
    public String getDescription() {
        return " [닉네임] [필보 ID] - 보상 초기화";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.admin.cooldownreset";
    }

    @Override
    public boolean onlyPlayer() {
        return false;
    }
}
