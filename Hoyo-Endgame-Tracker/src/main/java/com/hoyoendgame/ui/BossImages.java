package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import javafx.scene.image.Image;

/**
 * Boss pictures from {@code data/genshin/bossIMG/} (e.g. bossIMG/Tulpa.png), all in one folder. The picture is found
 * by boss name, as written in genshinBoss.json; the same boss in several patches uses the same picture.
 */
public final class BossImages {
    private static ImageFolder folder;

    private BossImages() {
    }

    /** The picture of this boss, or null when there is none. */
    public static synchronized Image get(String boss) {
        if (folder == null) folder = new ImageFolder(DataStore.get().bossImagesDir());
        return folder.get(boss);
    }
}
