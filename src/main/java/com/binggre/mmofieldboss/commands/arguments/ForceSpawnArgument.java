package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public class ForceSpawnArgument implements CommandArgument {

    private final FieldBossRepository repository = MMOFieldBoss.getPlugin().getFieldBossRepository();

    @Override
    public boolean execute(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String s, @Nonnull String[] args) {
        int id = toInt(args[1]);
        if (id == NumberUtil.PARSE_ERROR) {
            sender.sendMessage("숫자를 입력해 주세요.");
            return false;
        }
        FieldBoss fieldBoss = repository.get(id);
        if (fieldBoss == null) {
            sender.sendMessage("존재하지 않는 필드보스입니다.");
            return false;
        }
        try {
            fieldBoss.getDataThisServer().spawn();
            sender.sendMessage("스폰 완료");
            return true;
        } catch (InvalidMobTypeException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getArg() {
        return "강제스폰";
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public String getDescription() {
        return " [ID] - 강제로 보스를 스폰합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.admin.forcespawn";
    }

    @Override
    public boolean onlyPlayer() {
        return false;
    }
}
