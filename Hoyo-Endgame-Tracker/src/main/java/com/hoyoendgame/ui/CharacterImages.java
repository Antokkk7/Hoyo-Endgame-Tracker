package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import javafx.scene.image.Image;

/** Character pictures from {@code data/genshin/personnagesIMG}, found by character name (see {@link ImageFolder}). */
public final class CharacterImages {
    private static ImageFolder folder;

    private CharacterImages() {
    }

    static String key(String name) {
        return ImageFolder.key(name);
    }

    /** The picture of this character, or null when there is none. */
    public static synchronized Image get(String name) {
        if (folder == null) folder = new ImageFolder(DataStore.get().imagesDir());
        return folder.get(name);
    }
}
