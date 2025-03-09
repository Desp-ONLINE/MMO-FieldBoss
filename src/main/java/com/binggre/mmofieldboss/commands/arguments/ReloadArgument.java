package com.binggre.mmofieldboss.commands.arguments;

import com.binggre.binggreapi.command.CommandArgument;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.listener.velocity.ReloadVelocityListener;
import com.binggre.velocitysocketclient.VelocityClient;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadArgument implements CommandArgument {

    public static void reload(CommandSender sender) {
        MMOFieldBoss plugin = MMOFieldBoss.getPlugin();
        plugin.getFieldBossRepository().init();
        plugin.getFieldBossConfig().init();
        sender.sendMessage("MMO-FieldBoss 리로드 완료");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        reload(sender);
        VelocityClient.getInstance().getConnectClient().send(ReloadVelocityListener.class, "");
        return true;
    }

    @Override
    public String getArg() {
        return "리로드";
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public String getDescription() {
        return " - config, 파일을 리로드합니다.";
    }

    @Override
    public String getPermission() {
        return "mmofieldboss.admin.relaod";
    }

    @Override
    public boolean onlyPlayer() {
        return false;
    }
}
