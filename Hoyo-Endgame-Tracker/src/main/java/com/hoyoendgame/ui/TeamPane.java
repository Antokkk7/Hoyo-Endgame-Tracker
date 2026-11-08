package com.hoyoendgame.ui;

import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.TeamMember;
import com.hoyoendgame.model.Weapon;
import javafx.geometry.Pos;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the 4 slots of a team. For each slot: character (picked from the roster), level,
 * constellation ("doublons"), weapon (picked from the weapon catalog) and weapon refinement.
 * These parameters belong to the team, so the same character can have different ones in each team.
 */
public final class TeamPane extends GridPane {
    private static final GenshinCharacter NONE = new GenshinCharacter();
    private static final Weapon NO_WEAPON = new Weapon("", 0);

    static {
        NONE.id = "none";
        NONE.name = "— Aucun —";
        NONE.element = null;
    }

    private final List<Slot> slots = new ArrayList<>();
    private final GameProfile profile;

    /** Genshin team of 4. */
    public TeamPane(int columns) {
        this(columns, GameProfile.genshin());
    }

    /** @param columns 1 for a vertical list, 2 for a 2x2 grid */
    public TeamPane(int columns, GameProfile profile) {
        this.profile = profile;
        setHgap(14);
        setVgap(14);
        for (int i = 0; i < 4; i++) {
            Slot s = new Slot(i + 1);
            slots.add(s);
            add(s.node, i % columns, i / columns);
        }
        for (int c = 0; c < columns; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            getColumnConstraints().add(cc);
        }
    }

    /** Members of the non-empty slots, in order. */
    public List<TeamMember> members() {
        List<TeamMember> out = new ArrayList<>();
        for (Slot s : slots) {
            TeamMember m = s.toMember();
            if (m != null) out.add(m);
        }
        return out;
    }

    public void setMembers(List<TeamMember> members) {
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).setMember(i < members.size() ? members.get(i) : null);
        }
    }

    static String characterLabel(GenshinCharacter c) {
        return c.element == null ? c.name : Ui.shortName(c.name) + " · " + c.element.label();
    }

    static String weaponLabel(Weapon w) {
        if (w.name().isEmpty()) return "— Aucune —";
        return w.rarity() > 0 ? w.name() + " (" + w.rarity() + "★)" : w.name();
    }

    private Weapon weaponByName(String name) {
        for (Weapon w : profile.weapons) {
            if (w.name().equalsIgnoreCase(name)) return w;
        }
        return new Weapon(name, 0);
    }

    private final class Slot {
        final VBox node = new VBox(10);
        final PickerButton<GenshinCharacter> charBtn;
        final PickerButton<Weapon> weaponBtn;
        final StackPane weaponTile = new StackPane();
        final StackPane tile = new StackPane();
        final Spinner<Integer> level = Ui.spinner(1, profile.maxLevel, profile.maxLevel);
        final ToggleGroup cons = new ToggleGroup();
        final ToggleGroup refine = new ToggleGroup();

        Slot(int n) {
            node.getStyleClass().add("inner-box");
            node.setPadding(new javafx.geometry.Insets(14));

            charBtn = new PickerButton<>("Choisir un personnage", "Choisir un personnage…", TeamPane::characterLabel, () -> {
                List<GenshinCharacter> l = new ArrayList<>();
                l.add(NONE);
                l.addAll(profile.characters);
                return l;
            });
            charBtn.setValue(NONE);
            charBtn.setSearchText(c -> c.name + " " + (c.element == null ? "" : c.element.label()));
            charBtn.setOnPick(this::updateTile);

            String chooseWeapon = "arme".equals(profile.weaponWord) ? "Choisir une arme" : "Choisir un " + profile.weaponWord;
            weaponBtn = new PickerButton<>(chooseWeapon, chooseWeapon + "…", TeamPane::weaponLabel, () -> {
                List<Weapon> l = new ArrayList<>();
                l.add(NO_WEAPON);
                l.addAll(profile.weapons);
                return l;
            });
            weaponBtn.setCustomFactory(text -> new Weapon(text, 0));
            weaponBtn.setOnPick(this::updateWeaponTile);
            updateWeaponTile(null);

            updateTile(NONE);

            FlowPane consRow = new FlowPane(4, 4);
            for (int k = 0; k <= 6; k++) consRow.getChildren().add(Ui.pill(profile.consPrefix + k, cons, k));
            Ui.keepOneSelected(cons);
            Ui.select(cons, 0);

            HBox refRow = new HBox(4); // no wrapping: a FlowPane would claim ~400px and squeeze the weapon name
            for (int k = 1; k <= 5; k++) refRow.getChildren().add(Ui.pill(profile.refinePrefix + k, refine, k));
            Ui.keepOneSelected(refine);
            Ui.select(refine, 1);

            VBox charBox = new VBox(4, Ui.label("Personnage " + n, "muted", "small"), charBtn);
            HBox.setHgrow(charBox, Priority.ALWAYS);
            VBox levelBox = new VBox(4, Ui.label("Niveau", "muted", "small"), level);
            HBox top = new HBox(10, tile, charBox, levelBox);
            top.setAlignment(Pos.BOTTOM_LEFT);

            VBox consBox = new VBox(4, Ui.label("Doublons du personnage", "muted", "small"), consRow);
            weaponBtn.setMinWidth(150);
            HBox weaponLine = new HBox(8, weaponTile, weaponBtn);
            weaponLine.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(weaponBtn, Priority.ALWAYS);
            VBox weaponBox = new VBox(4, Ui.label(profile.weaponTitle, "muted", "small"), weaponLine);
            HBox.setHgrow(weaponBox, Priority.ALWAYS);
            VBox refBox = new VBox(4, Ui.label(profile.refineTitle, "muted", "small"), refRow);
            refBox.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            HBox weaponRow = new HBox(10, weaponBox, refBox);
            weaponRow.setAlignment(Pos.BOTTOM_LEFT);

            node.getChildren().addAll(top, consBox, weaponRow);
        }

        void updateTile(GenshinCharacter c) {
            boolean none = c == null || c == NONE;
            tile.getChildren().setAll(Ui.portrait(profile.characterImages, none ? "?" : c.name, none ? null : c.element, 44, 52));
            applyElementStyle(none ? null : c.element);
        }

        /** Picture of the chosen weapon, in a square to the left of the weapon button. */
        void updateWeaponTile(Weapon w) {
            weaponTile.getChildren().setAll(Ui.weaponTile(profile.weaponImages, w == null ? "" : w.name(), 40));
        }

        /** Frame, glow and character button take the colour of the character's element. */
        void applyElementStyle(Element e) {
            if (e == null) {
                node.setStyle("");
                charBtn.setStyle("");
                return;
            }
            String col = Ui.elementColor(e);
            String light = Ui.hex(Color.web(col).interpolate(Color.WHITE, 0.30));
            node.setStyle("-fx-background-color:" + Ui.rgba(col, 0.09) + ";-fx-border-color:" + Ui.rgba(col, 0.85)
                    + ";-fx-effect:dropshadow(gaussian," + Ui.rgba(col, 0.30) + ",14,0,0,0);");
            charBtn.setStyle("-fx-border-color:" + col + ";-fx-text-fill:" + light + ";-fx-background-color:"
                    + Ui.rgba(col, 0.10) + ";");
        }

        TeamMember toMember() {
            GenshinCharacter c = charBtn.getValue();
            if (c == null || c == NONE) return null;
            TeamMember m = new TeamMember();
            m.characterId = c.id;
            m.name = c.name;
            m.element = c.element;
            m.level = level.getValue() == null ? profile.maxLevel : level.getValue();
            m.constellation = Ui.selectedInt(cons, 0);
            Weapon w = weaponBtn.getValue();
            m.weapon = w == null ? "" : w.name();
            m.refinement = Ui.selectedInt(refine, 1);
            return m;
        }

        void setMember(TeamMember m) {
            if (m == null) {
                charBtn.setValue(NONE);
                updateTile(NONE);
                level.getValueFactory().setValue(profile.maxLevel);
                Ui.select(cons, 0);
                Ui.select(refine, 1);
                weaponBtn.setValue(null);
                updateWeaponTile(null);
                return;
            }
            GenshinCharacter found = null;
            for (GenshinCharacter c : profile.characters) {
                if ((m.characterId != null && m.characterId.equals(c.id)) || c.name.equalsIgnoreCase(m.name)) {
                    found = c;
                    break;
                }
            }
            if (found == null) {
                // Not in the roster (anymore): keep the member as saved.
                found = new GenshinCharacter();
                if (m.characterId != null) found.id = m.characterId;
                found.name = m.name;
                found.element = m.element;
            }
            charBtn.setValue(found);
            updateTile(found);
            level.getValueFactory().setValue(Math.max(1, Math.min(profile.maxLevel, m.level)));
            Ui.select(cons, m.constellation);
            Weapon wp = m.weapon == null || m.weapon.isBlank() ? null : weaponByName(m.weapon);
            weaponBtn.setValue(wp);
            updateWeaponTile(wp);
            Ui.select(refine, m.refinement);
        }
    }
}
