package com.binggre.mmofieldboss.listener.velocity;

import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.velocitysocketclient.listener.VelocitySocketListener;
import com.binggre.velocitysocketclient.socket.SocketResponse;
import org.jetbrains.annotations.NotNull;

public class BroadcastVelocityListener extends VelocitySocketListener {

    @Override
    public void onReceive(String[] messages) {
        FieldBoss.broadcastLastHit(messages[0], messages[1]);
    }

    @Override
    public @NotNull SocketResponse onRequest(String... strings) {
        return SocketResponse.empty();
    }

    @Override
    public void onResponse(SocketResponse socketResponse) {

    }
}
