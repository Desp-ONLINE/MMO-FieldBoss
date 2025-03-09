package com.binggre.mmofieldboss.config;

import com.binggre.binggreapi.utils.ColorManager;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mongolibraryplugin.base.MongoConfiguration;
import lombok.Getter;
import org.bson.Document;

@Getter
public class FieldBossConfig extends MongoConfiguration {

    public FieldBossConfig(String database, String collection) {
        super(database, collection);
    }

    private int afkSeconds = 20;
    private String broadcastLastHit = "\n§b<player>§f님께서 <boss> §f필드 보스를 마지막 타격\n";
    private String mailSender = "시스템";
    private String letterNormal = "필드 보스 참여 보상";
    private String letterBestDamage = "필드 보스 최고 데미지 보상";
    private String letterLastHit = "필드 보스 막타 보상";

    @Override
    public void init() {
        Document configDocument = getConfigDocument();
        if (configDocument == null) {
            save();
            return;
        }

        FieldBossConfig newInstance = FileManager.toObject(configDocument.toJson(), FieldBossConfig.class);
        afkSeconds = newInstance.afkSeconds;
        broadcastLastHit = ColorManager.format(newInstance.broadcastLastHit);
        mailSender = ColorManager.format(newInstance.mailSender);
        letterNormal = ColorManager.format(newInstance.letterNormal);
        letterBestDamage = ColorManager.format(newInstance.letterBestDamage);
        letterLastHit = ColorManager.format(newInstance.letterLastHit);
    }
}