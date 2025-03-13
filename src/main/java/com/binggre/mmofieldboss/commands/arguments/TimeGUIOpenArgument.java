package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.mmofieldboss.gui.TimeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimeGUIOpenArgument implements CommandArgument {

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        TimeGUI.open(player);
        return true;
    }

    @Override
    public String getArg() {
        return "시간";
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public String getDescription() {
        return " - 필드보스 시간을 확인합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.user.time";
    }

    @Override
    public boolean onlyPlayer() {
        return true;
    }
}
