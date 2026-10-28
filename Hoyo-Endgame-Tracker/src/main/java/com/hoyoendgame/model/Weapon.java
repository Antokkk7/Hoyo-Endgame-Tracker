package com.hoyoendgame.model;

/** A weapon of the game catalog (genshinWeapons.json, read-only). Rarity 0 = unknown (not in the catalog). */
public record Weapon(String name, int rarity) {
    @Override
    public String toString() {
        return name;
    }
}
