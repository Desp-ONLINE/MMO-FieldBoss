package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mongolibraryplugin.base.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FieldBossRedis implements MongoData<String> {

    private String id;
    private boolean jsonUseOnly;
    private String bossName;
    private List<Integer> spawnHours;
    private CustomItemStack customItemStack;

    private String filedBossJson;

    public int getFieldBossId() {
        return Integer.parseInt(id.split("-")[0]);
    }

    public int getPort() {
        return Integer.parseInt(id.split("-")[1]);
    }

    @Override
    public String getId() {
        return id;
    }

    public FieldBoss toFieldBoss() {
        return FileManager.toObject(filedBossJson, FieldBoss.class);
    }
}