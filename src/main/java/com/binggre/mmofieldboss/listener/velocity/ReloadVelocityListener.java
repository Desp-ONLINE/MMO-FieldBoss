package com.binggre.mmofieldboss.listener.velocity;

import com.binggre.mmofieldboss.commands.arguments.ReloadArgument;
import com.binggre.velocitysocketclient.listener.VelocitySocketListener;
import com.binggre.velocitysocketclient.socket.SocketResponse;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ReloadVelocityListener extends VelocitySocketListener {

    @Override
    public void onReceive(String[] strings) {
        ReloadArgument.reload(Bukkit.getConsoleSender());
    }

    @Override
    public @NotNull SocketResponse onRequest(String... strings) {
        return SocketResponse.empty();
    }

    @Override
    public void onResponse(SocketResponse socketResponse) {

    }
}