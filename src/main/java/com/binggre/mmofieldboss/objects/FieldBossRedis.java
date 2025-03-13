package com.binggre.mmofieldboss.objects;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.mongolibraryplugin.base.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FieldBossRedis implements MongoData<String> {

    private String id;
    private String bossName;
    private List<Integer> spawnHours;
    private CustomItemStack customItemStack;

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
}