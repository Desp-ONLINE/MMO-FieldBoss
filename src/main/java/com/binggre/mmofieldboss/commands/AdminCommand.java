package com.binggre.mmofieldboss.commands;

import com.binggre.binggreapi.command.BetterCommand;
import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.commands.arguments.ForceSpawnArgument;
import com.binggre.mmofieldboss.commands.arguments.ReloadArgument;
import com.binggre.mmofieldboss.commands.arguments.RewardArgument;
import com.binggre.mmofieldboss.objects.RewardType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class AdminCommand extends BetterCommand {

    @Override
    public String getCommand() {
        return "필드보스";
    }

    @Override
    public boolean isSingleCommand() {
        return false;
    }

    @Override
    public List<CommandArgument> getArguments() {
        return List.of(
                new ForceSpawnArgument(),
                new RewardArgument(),
                new ReloadArgument()
        );
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> strings = super.onTabComplete(sender, command, s, args);

        switch (args[0]) {
            case "보상설정" -> {
                if (args.length == 2) {
                    return MMOFieldBoss.getPlugin().getFieldBossRepository()
                            .values()
                            .stream()
                            .map(fieldBoss -> fieldBoss.getId() + "").toList();
                } else if (args.length == 3) {
                    return Arrays.stream(RewardType.values()).map(Enum::toString).toList();
                }
            }
            case "강제스폰" -> {
                return MMOFieldBoss.getPlugin().getFieldBossRepository()
                        .values()
                        .stream()
                        .map(fieldBoss -> fieldBoss.getId() + "").toList();
            }
        }


        return strings;
    }
}