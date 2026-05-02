package com.binggre.mmofieldboss.config;

import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mongolibraryplugin.base.MongoConfiguration;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FieldBossConfig extends MongoConfiguration {

    public FieldBossConfig(String database, String collection) {
        super(database, collection);
    }

    private int afkSeconds = 20;
    private int prepareMinute = 5;
    private double hpScaleBase = 0.6;
    private double damageScaleBase = 0.15;
    private List<String> commandWhitelist = new ArrayList<>(List.of("필드보스나가기"));

    @Override
    public void init() {
        Document configDocument = getConfigDocument();
        if (configDocument == null) {
            save();
            return;
        }

        FieldBossConfig newInstance = FileManager.toObject(configDocument.toJson(), FieldBossConfig.class);

        afkSeconds = newInstance.afkSeconds > 0 ? newInstance.afkSeconds : afkSeconds;
        prepareMinute = newInstance.prepareMinute > 0 ? newInstance.prepareMinute : prepareMinute;
        hpScaleBase = newInstance.hpScaleBase > 0 ? newInstance.hpScaleBase : hpScaleBase;
        damageScaleBase = newInstance.damageScaleBase > 0 ? newInstance.damageScaleBase : damageScaleBase;
        if (newInstance.commandWhitelist != null) {
            commandWhitelist = newInstance.commandWhitelist;
        }
    }
}
