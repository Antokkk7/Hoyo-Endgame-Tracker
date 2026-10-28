package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.AbyssClear;
import com.hoyoendgame.model.Team;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Add / edit an Abyss clear: patch, stars of floors 9 to 12 (total out of 36) and the two teams
 * used for floors 11 and 12 (pick a saved team, then tweak it freely).
 */
public final class AbyssAddDialog {
    private AbyssAddDialog() {
    }

    public static Optional<AbyssClear> show(Window owner, AbyssClear existing) {
        DataStore store = DataStore.get();

        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 6.1");
        patch.setPrefWidth(140);
        ComboBox<String> rotation = new ComboBox<>();
        rotation.getItems().addAll("Rotation 1", "Rotation 2");
        rotation.setValue(existing != null && rotation.getItems().contains(existing.rotation) ? existing.rotation : "Rotation 1");

        Spinner<Integer> f9 = Ui.spinner(0, 9, existing == null ? 9 : existing.floor9);
        Spinner<Integer> f10 = Ui.spinner(0, 9, existing == null ? 9 : existing.floor10);
        Spinner<Integer> f11 = Ui.spinner(0, 9, existing == null ? 9 : existing.floor11);
        Spinner<Integer> f12 = Ui.spinner(0, 9, existing == null ? 9 : existing.floor12);
        Label total = Ui.label("", "big-number", "gold");
        Runnable upd = () -> total.setText((nz(f9) + nz(f10) + nz(f11) + nz(f12)) + "/36");
        for (Spinner<Integer> s : List.of(f9, f10, f11, f12)) s.valueProperty().addListener((o, a, b) -> upd.run());
        upd.run();

        HBox starsRow = new HBox(18, field("Patch", patch), field("Rotation", rotation),
                field("Étage 9 (/9)", f9), field("Étage 10 (/9)", f10), field("Étage 11 (/9)", f11), field("Étage 12 (/9)", f12),
                Ui.grow(), new VBox(2, Ui.label("Total", "muted", "small"), total));
        starsRow.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = section("1. Patch et étoiles", starsRow,
                Ui.label("Étages 9 à 12 : 9 étoiles chacun. Exemple : 27/36 si l'étage 12 est à 0/9.", "muted", "small"));

        TeamPane pane1 = new TeamPane(1);
        TeamPane pane2 = new TeamPane(1);
        if (existing != null) {
            pane1.setMembers(existing.team1);
            pane2.setMembers(existing.team2);
        }
        VBox team1 = teamBox("Équipe 1 · première moitié", store.teams, pane1);
        VBox team2 = teamBox("Équipe 2 · seconde moitié", store.teams, pane2);
        HBox teams = new HBox(18, team1, team2);
        VBox section2 = section("2. Équipes utilisées (pour l'étage clear le plus haut)", teams,
                Ui.label("Choisir une équipe enregistrée remplit les 4 emplacements, elles restent toutefois modifiables", "muted", "small"));

        AbyssClear[] result = new AbyssClear[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            if (patch.getText() == null || patch.getText().isBlank()) {
                Ui.error("Indiquez le patch (ex. 6.1).");
                return;
            }
            AbyssClear c = new AbyssClear();
            if (existing != null) c.id = existing.id;
            c.patch = patch.getText().trim();
            c.rotation = rotation.getValue();
            c.floor9 = nz(f9);
            c.floor10 = nz(f10);
            c.floor11 = nz(f11);
            c.floor12 = nz(f12);
            c.team1 = pane1.members();
            c.team2 = pane2.members();
            result[0] = c;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, Ui.label(existing == null ? "Ajouter un clear" : "Modifier le clear", "h1"),
                Ui.label("Étoiles obtenues, puis les deux équipes utilisées.", "muted"), section1, section2);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        VBox root = new VBox(16, scroll, actions);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(24, 28, 24, 28));

        stage[0] = Ui.modal(owner, "Abysses spiralées - clear", root, 1180, 860);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static int nz(Spinner<Integer> s) {
        return s.getValue() == null ? 0 : s.getValue();
    }

    private static VBox field(String label, javafx.scene.Node control) {
        return new VBox(4, Ui.label(label, "muted", "small"), control);
    }

    private static VBox section(String title, javafx.scene.Node content, javafx.scene.Node footer) {
        VBox box = new VBox(14, Ui.label(title, "h3"), content, footer);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(22));
        return box;
    }

    private static VBox teamBox(String title, List<Team> saved, TeamPane pane) {
        ComboBox<Team> preset = new ComboBox<>();
        preset.getItems().addAll(saved);
        preset.setPromptText("Équipe enregistrée…");
        preset.setMaxWidth(Double.MAX_VALUE);
        preset.valueProperty().addListener((o, a, t) -> {
            if (t != null) pane.setMembers(t.copy().members);
        });
        VBox box = new VBox(10, Ui.label(title, "strong"), field("Équipe pré-enregistrée", preset), pane);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setPrefWidth(480);
        return box;
    }
}
