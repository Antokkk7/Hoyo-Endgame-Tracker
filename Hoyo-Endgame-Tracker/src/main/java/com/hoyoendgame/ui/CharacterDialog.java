package com.hoyoendgame.ui;

import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.GenshinCharacter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/** Add / edit a roster character. */
public final class CharacterDialog {
    private CharacterDialog() {
    }

    public static Optional<GenshinCharacter> show(Window owner, GenshinCharacter existing) {
        GenshinCharacter base = existing == null ? new GenshinCharacter() : existing.copy();

        TextField name = new TextField(base.name);
        name.setPromptText("Nom du personnage");
        ComboBox<Element> element = new ComboBox<>();
        element.getItems().addAll(Element.values());
        element.setValue(base.element);
        Spinner<Integer> level = Ui.spinner(1, 90, base.level);
        Spinner<Integer> cons = Ui.spinner(0, 6, base.constellation);
        TextField weapon = new TextField(base.weapon);
        weapon.setPromptText("Arme");
        Spinner<Integer> refine = Ui.spinner(1, 5, base.refinement);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.add(Ui.label("Nom", "muted", "small"), 0, 0);
        form.add(name, 0, 1, 2, 1);
        form.add(Ui.label("Élément", "muted", "small"), 0, 2);
        form.add(element, 0, 3);
        form.add(Ui.label("Niveau", "muted", "small"), 1, 2);
        form.add(level, 1, 3);
        form.add(Ui.label("Doublons (C0 à C6)", "muted", "small"), 0, 4);
        form.add(cons, 0, 5);
        form.add(Ui.label("Raffinement de l'arme", "muted", "small"), 1, 4);
        form.add(refine, 1, 5);
        form.add(Ui.label("Arme", "muted", "small"), 0, 6);
        form.add(weapon, 0, 7, 2, 1);

        GenshinCharacter[] result = new GenshinCharacter[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            if (name.getText() == null || name.getText().isBlank()) {
                Ui.error("Donnez un nom au personnage.");
                return;
            }
            base.name = name.getText().trim();
            base.element = element.getValue();
            base.level = level.getValue();
            base.constellation = cons.getValue();
            base.weapon = weapon.getText() == null ? "" : weapon.getText().trim();
            base.refinement = refine.getValue();
            result[0] = base;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, Ui.label(existing == null ? "Ajouter un personnage" : "Modifier le personnage", "h2"), form, actions);
        root.setPadding(new Insets(28));
        stage[0] = Ui.modal(owner, "Personnage", root, 520, 470);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }
}
