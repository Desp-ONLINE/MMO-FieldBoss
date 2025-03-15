package com.binggre.mmofieldboss.listener.velocity;

import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.velocitysocketclient.listener.VelocitySocketListener;
import com.binggre.velocitysocketclient.socket.SocketResponse;
import org.jetbrains.annotations.NotNull;

public class BroadcastVelocityListener extends VelocitySocketListener {

    @Override
    public void onReceive(String[] messages) {
//        lastNickname, bestNickname, bestDamage + "", bossName
        FieldBoss.broadcast(messages[0], messages[1], Double.parseDouble(messages[2]), messages[3]);
    }

    @Override
    public @NotNull SocketResponse onRequest(String... strings) {
        return SocketResponse.empty();
    }

    @Override
    public void onResponse(SocketResponse socketResponse) {

    }
}
