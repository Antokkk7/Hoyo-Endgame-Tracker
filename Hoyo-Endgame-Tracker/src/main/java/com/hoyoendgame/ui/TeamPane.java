package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.TeamMember;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the 4 slots of a team: character (from the roster), level, constellation ("doublons"),
 * weapon and weapon refinement. Picking a roster character pre-fills its saved values, which stay editable.
 */
public final class TeamPane extends GridPane {
    private static final GenshinCharacter NONE = new GenshinCharacter();

    static {
        NONE.id = "none";
        NONE.name = "— Aucun —";
        NONE.element = null;
    }

    private final List<Slot> slots = new ArrayList<>();

    /** @param columns 1 for a vertical list, 2 for a 2x2 grid */
    public TeamPane(int columns) {
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

    private final class Slot {
        final VBox node = new VBox(10);
        final ComboBox<GenshinCharacter> combo = new ComboBox<>();
        final StackPane tile = new StackPane();
        final javafx.scene.control.Label initials = Ui.label("?", "tile-text");
        final Spinner<Integer> level = Ui.spinner(1, 90, 90);
        final ToggleGroup cons = new ToggleGroup();
        final ToggleGroup refine = new ToggleGroup();
        final TextField weapon = new TextField();

        Slot(int n) {
            node.getStyleClass().add("inner-box");
            node.setStyle("-fx-padding:14;");
            combo.getItems().add(NONE);
            combo.getItems().addAll(DataStore.get().characters);
            combo.setValue(NONE);
            combo.setMaxWidth(Double.MAX_VALUE);

            tile.setPrefSize(44, 52);
            tile.setMinSize(44, 52);
            tile.getChildren().add(initials);
            Ui.tileStyle(tile, null);

            weapon.setPromptText("Arme");

            FlowPane consRow = new FlowPane(4, 4);
            for (int k = 0; k <= 6; k++) consRow.getChildren().add(Ui.pill("C" + k, cons, k));
            Ui.keepOneSelected(cons);
            Ui.select(cons, 0);

            FlowPane refRow = new FlowPane(4, 4);
            for (int k = 1; k <= 5; k++) refRow.getChildren().add(Ui.pill("R" + k, refine, k));
            Ui.keepOneSelected(refine);
            Ui.select(refine, 1);

            VBox comboBox = new VBox(4, Ui.label("Personnage " + n, "muted", "small"), combo);
            HBox.setHgrow(comboBox, Priority.ALWAYS);
            VBox levelBox = new VBox(4, Ui.label("Niveau", "muted", "small"), level);
            HBox top = new HBox(10, tile, comboBox, levelBox);
            top.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

            VBox consBox = new VBox(4, Ui.label("Doublons du personnage", "muted", "small"), consRow);
            HBox.setHgrow(weapon, Priority.ALWAYS);
            VBox weaponBox = new VBox(4, Ui.label("Arme", "muted", "small"), weapon);
            HBox.setHgrow(weaponBox, Priority.ALWAYS);
            VBox refBox = new VBox(4, Ui.label("Raffinement", "muted", "small"), refRow);
            HBox weaponRow = new HBox(10, weaponBox, refBox);
            weaponRow.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

            node.getChildren().addAll(top, consBox, weaponRow);

            combo.valueProperty().addListener((o, old, c) -> {
                updateTile(c);
                if (c != null && c != NONE) {
                    level.getValueFactory().setValue(Math.max(1, Math.min(90, c.level)));
                    Ui.select(cons, c.constellation);
                    weapon.setText(c.weapon == null ? "" : c.weapon);
                    Ui.select(refine, c.refinement);
                }
            });
        }

        void updateTile(GenshinCharacter c) {
            if (c == null || c == NONE) {
                initials.setText("?");
                Ui.tileStyle(tile, null);
            } else {
                initials.setText(Ui.initials(c.name));
                Ui.tileStyle(tile, c.element);
            }
        }

        TeamMember toMember() {
            GenshinCharacter c = combo.getValue();
            if (c == null || c == NONE) return null;
            TeamMember m = new TeamMember();
            m.characterId = c.id;
            m.name = c.name;
            m.element = c.element;
            m.level = level.getValue() == null ? 90 : level.getValue();
            m.constellation = Ui.selectedInt(cons, 0);
            m.weapon = weapon.getText() == null ? "" : weapon.getText().trim();
            m.refinement = Ui.selectedInt(refine, 1);
            return m;
        }

        void setMember(TeamMember m) {
            if (m == null) {
                combo.setValue(NONE);
                level.getValueFactory().setValue(90);
                Ui.select(cons, 0);
                Ui.select(refine, 1);
                weapon.setText("");
                return;
            }
            GenshinCharacter found = null;
            for (GenshinCharacter c : combo.getItems()) {
                if (c == NONE) continue;
                if ((m.characterId != null && m.characterId.equals(c.id)) || c.name.equalsIgnoreCase(m.name)) {
                    found = c;
                    break;
                }
            }
            if (found == null) {
                // The character is not in the roster (anymore): keep it selectable for this slot only.
                found = new GenshinCharacter();
                if (m.characterId != null) found.id = m.characterId;
                found.name = m.name;
                found.element = m.element;
                combo.getItems().add(found);
            }
            combo.setValue(found);
            level.getValueFactory().setValue(Math.max(1, Math.min(90, m.level)));
            Ui.select(cons, m.constellation);
            weapon.setText(m.weapon == null ? "" : m.weapon);
            Ui.select(refine, m.refinement);
        }
    }
}
