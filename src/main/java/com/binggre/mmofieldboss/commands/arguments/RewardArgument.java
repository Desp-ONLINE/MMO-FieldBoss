package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.gui.RewardGUI;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.RewardType;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RewardArgument implements CommandArgument {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player player = (Player) sender;

        int id = toInt(args[1]);
        if (id == NumberUtil.PARSE_ERROR) {
            sender.sendMessage("숫자를 입력해 주세요");
            return false;
        }
        RewardType rewardType = RewardType.valueOf(args[2]);

        FieldBoss fieldBoss = repository.get(id);
        if (fieldBoss == null) {
            sender.sendMessage("존재하지 않는 필드보스입니다.");
            return false;
        }
        RewardGUI.open(player, rewardType, id);
        return true;
    }

    @Override
    public String getArg() {
        return "보상설정";
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public String getDescription() {
        return " [ID] [타입] - 보상을 설정합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.admin.reward";
    }

    @Override
    public boolean onlyPlayer() {
        return true;
    }
}
