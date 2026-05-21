package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.binggreapi.utils.NumberUtil;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.mmofieldboss.objects.FieldBossData;
import com.binggre.mmofieldboss.repository.FieldBossRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public class OpenSessionArgument implements CommandArgument {

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
        FieldBossData data = fieldBoss.getDataThisServer();
        if (data == null) {
            sender.sendMessage("이 서버에 해당 필드보스 데이터가 없습니다.");
            return false;
        }
        if (data.getArenaSpawn() == null) {
            sender.sendMessage("arenaSpawn 좌표가 설정되지 않았습니다. DB에서 먼저 설정해 주세요.");
            return false;
        }
        if (data.getSpawnedBoss() != null) {
            sender.sendMessage("이미 보스가 스폰되어 있습니다.");
            return false;
        }
        data.openSession();
        sender.sendMessage("§a입장 오픈 완료 (보스 ID: " + id + ")");
        return true;
    }

    @Override
    public String getArg() {
        return "입장오픈";
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public String getDescription() {
        return " [ID] - 필드보스 입장 세션을 오픈합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.admin.opensession";
    }

    @Override
    public boolean onlyPlayer() {
        return false;
    }
}
