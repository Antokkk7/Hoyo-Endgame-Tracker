package com.hoyoendgame.ui;

import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.GenshinCharacter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/** Add / edit a character of the roster (name and element only; the other parameters are set per team). */
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
        element.setMaxWidth(Double.MAX_VALUE);

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
            result[0] = base;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16,
                Ui.label(existing == null ? "Ajouter un personnage" : "Modifier le personnage", "h2"),
                Ui.label("Niveau, doublons et arme se règlent dans chaque équipe.", "muted", "small"),
                new VBox(4, Ui.label("Nom", "muted", "small"), name),
                new VBox(4, Ui.label("Élément", "muted", "small"), element),
                actions);
        root.setPadding(new Insets(28));
        stage[0] = Ui.modal(owner, "Personnage", root, 460, 360);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }
}
