package com.binggre.mmofieldboss.listener.velocity;

import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mmofieldboss.MMOFieldBoss;
import com.binggre.mmofieldboss.objects.FieldBoss;
import com.binggre.velocitysocketclient.listener.VelocitySocketListener;
import com.binggre.velocitysocketclient.socket.SocketResponse;
import org.jetbrains.annotations.NotNull;

public class FieldBossVelocityListener extends VelocitySocketListener {

    @Override
    public void onReceive(String[] strings) {

    }

    @Override
    public @NotNull SocketResponse onRequest(String... strings) {
        int i = Integer.parseInt(strings[0]);
        FieldBoss fieldBoss = MMOFieldBoss.getPlugin().getFieldBossRepository().get(i);
        if (fieldBoss == null) {
            return SocketResponse.empty();
        }

        return SocketResponse.ok(FileManager.toJson(fieldBoss));
    }

    @Override
    public void onResponse(SocketResponse socketResponse) {

    }
}
