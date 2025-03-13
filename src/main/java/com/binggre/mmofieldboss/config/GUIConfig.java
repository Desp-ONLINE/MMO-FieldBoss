package com.binggre.mmofieldboss.config;

import com.binggre.binggreapi.objects.items.CustomItemStack;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.mongolibraryplugin.base.MongoConfiguration;
import lombok.Getter;
import org.bson.Document;

import java.util.List;
import java.util.Map;

@Getter
public class GUIConfig extends MongoConfiguration {

    private String title = "필드 보스";
    private int size = 6;
    // page, view ids
    private Map<Integer, List<Integer>> pageIds = Map.of(1, List.of(1, 2));
    private CustomItemStack previous = CustomItemStack.create(45, "arrow", "§f이전", List.of(), 1, 0);
    private CustomItemStack next = CustomItemStack.create(53, "arrow", "§f다음", List.of(), 1, 0);

    public GUIConfig(String database, String collection) {
        super(database, collection);
    }

    @Override
    public void init() {
        Document configDocument = getConfigDocument();
        if (configDocument == null) {
            save();
            return;
        }
        GUIConfig newInstance = FileManager.toObject(configDocument.toJson(), GUIConfig.class);
        pageIds = newInstance.pageIds;
        title = newInstance.title;
        size = newInstance.size;
        previous = newInstance.previous;
        next = newInstance.next;
    }
}
