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
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Add / edit an Abyss clear: patch, cleared floor (9 to 12), stars earned before that floor
 * (adjustable, full by default), stars of each of the 3 rooms, and the team used for the
 * highest cleared floor.
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

        // ---- cleared floor and stars
        ComboBox<String> floorChoice = new ComboBox<>();
        floorChoice.getItems().addAll("Étage 9", "Étage 10", "Étage 11", "Étage 12");
        int floor = existing == null ? 12 : Math.max(9, Math.min(12, existing.floor));
        floorChoice.getSelectionModel().select(floor - 9);

        Spinner<Integer> base = Ui.spinner(0, 9 * (floor - 9), existing == null ? 9 * (floor - 9) : existing.baseStars);
        Spinner<Integer> r1 = Ui.spinner(0, 3, existing == null ? 3 : existing.room1);
        Spinner<Integer> r2 = Ui.spinner(0, 3, existing == null ? 3 : existing.room2);
        Spinner<Integer> r3 = Ui.spinner(0, 3, existing == null ? 3 : existing.room3);
        Label total = Ui.label("", "big-number", "gold");
        Label baseMax = Ui.label("/" + 9 * (floor - 9), "muted");

        Runnable upd = () -> {
            int f = floorChoice.getSelectionModel().getSelectedIndex() + 9;
            total.setText((nz(base) + nz(r1) + nz(r2) + nz(r3)) + "/" + 9 * (f - 8));
        };
        Runnable onFloor = () -> {
            int f = floorChoice.getSelectionModel().getSelectedIndex() + 9;
            SpinnerValueFactory.IntegerSpinnerValueFactory bf =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) base.getValueFactory();
            bf.setMax(9 * (f - 9));
            if (bf.getValue() == null || bf.getValue() > 9 * (f - 9)) bf.setValue(9 * (f - 9));
            baseMax.setText("/" + 9 * (f - 9));
            upd.run();
        };
        floorChoice.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onFloor.run());
        for (Spinner<Integer> s : List.of(base, r1, r2, r3)) s.valueProperty().addListener((o, a, b) -> upd.run());
        upd.run();

        HBox starsRow = new HBox(18, field("Patch", patch), field("Rotation", rotation), field("Étage du clear", floorChoice),
                new VBox(4, Ui.label("Étoiles avant l'étage", "muted", "small"), new HBox(8, base, baseMax)),
                field("Salle 1 (/3)", r1), field("Salle 2 (/3)", r2), field("Salle 3 (/3)", r3),
                Ui.grow(), new VBox(2, Ui.label("Total", "muted", "small"), total));
        starsRow.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = section("1. Patch et étoiles", starsRow,
                Ui.label("Étage 9 = 9 étoiles max · Étage 10 = 18 · Étage 11 = 27 · Étage 12 = 36. "
                        + "Le total = étoiles avant l'étage (modifiable) + les 3 salles de l'étage clear.", "muted", "small"));

        TeamPane pane = new TeamPane(1);
        if (existing != null) pane.setMembers(existing.team);
        VBox team1 = teamBox("Équipe (étage le plus haut)", store.teams, pane);
        HBox teams = new HBox(18, team1);
        VBox section2 = section("2. Équipe utilisée", teams,
                Ui.label("Choisir une équipe enregistrée remplit les 4 emplacements, elle reste toutefois modifiable", "muted", "small"));

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
            c.floor = floorChoice.getSelectionModel().getSelectedIndex() + 9;
            c.baseStars = nz(base);
            c.room1 = nz(r1);
            c.room2 = nz(r2);
            c.room3 = nz(r3);
            c.team = pane.members();
            c.normalize();
            result[0] = c;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, Ui.label(existing == null ? "Ajouter un clear" : "Modifier le clear", "h1"),
                Ui.label("Étoiles obtenues, puis l'équipe utilisée.", "muted"), section1, section2);
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