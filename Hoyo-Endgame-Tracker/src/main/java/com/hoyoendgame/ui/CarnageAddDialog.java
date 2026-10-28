package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.BossRun;
import com.hoyoendgame.model.CarnageClear;
import com.hoyoendgame.model.Team;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Add / edit a Stygian Onslaught clear: patch, period, difficulty 1~6 and the 3 boss fights. */
public final class CarnageAddDialog {
    private static final int BOSSES = 3;

    private CarnageAddDialog() {
    }

    private record BossForm(TextField name, Spinner<Integer> seconds, TeamPane team, VBox node) {
    }

    public static Optional<CarnageClear> show(Window owner, CarnageClear existing) {
        DataStore store = DataStore.get();

        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 6.1");
        patch.setPrefWidth(120);
        TextField period = new TextField(existing == null ? "" : existing.period);
        period.setPromptText("ex. 2026.09.30 – 2026.11.03");
        period.setPrefWidth(280);

        ToggleGroup diff = new ToggleGroup();
        HBox diffRow = new HBox(10);
        for (int d = 1; d <= 6; d++) {
            ToggleButton tb = new ToggleButton(String.valueOf(d));
            tb.getStyleClass().add("diff-toggle");
            tb.setStyle("-fx-background-color:" + Ui.DIFF_COLORS[d - 1] + ";");
            tb.setToggleGroup(diff);
            tb.setUserData(d);
            diffRow.getChildren().add(tb);
        }
        Ui.keepOneSelected(diff);
        Ui.select(diff, existing == null ? 6 : existing.difficulty);

        List<BossForm> bosses = new ArrayList<>();
        Label total = Ui.label("", "h2");
        Runnable upd = () -> total.setText(bosses.stream().mapToInt(b -> b.seconds().getValue() == null ? 0 : b.seconds().getValue()).sum() + " s");

        VBox bossBoxes = new VBox(16);
        for (int i = 0; i < BOSSES; i++) {
            BossRun prior = existing != null && i < existing.bosses.size() ? existing.bosses.get(i) : null;
            TextField name = new TextField(prior == null ? "" : prior.name);
            name.setPromptText("Nom du boss " + (i + 1));
            Spinner<Integer> secs = Ui.spinner(0, 999, prior == null ? 0 : prior.seconds);
            secs.valueProperty().addListener((o, a, b) -> upd.run());
            TeamPane pane = new TeamPane(2);
            if (prior != null) pane.setMembers(prior.team);

            ComboBox<Team> preset = new ComboBox<>();
            preset.getItems().addAll(store.teams);
            preset.setPromptText("Équipe enregistrée…");
            preset.setPrefWidth(260);
            preset.valueProperty().addListener((o, a, t) -> {
                if (t != null) pane.setMembers(t.copy().members);
            });

            HBox.setHgrow(name, Priority.ALWAYS);
            HBox top = new HBox(14, field("Boss " + (i + 1), name), field("Durée (s)", secs), field("Équipe pré-enregistrée", preset));
            HBox.setHgrow(top.getChildren().get(0), Priority.ALWAYS);
            top.setAlignment(Pos.BOTTOM_LEFT);
            VBox node = new VBox(14, top, pane);
            node.getStyleClass().add("inner-box");
            node.setPadding(new Insets(16));
            bossBoxes.getChildren().add(node);
            bosses.add(new BossForm(name, secs, pane, node));
        }
        upd.run();

        VBox s1 = section("1. Patch, période et difficulté",
                new HBox(18, field("Patch", patch), field("Période", period)),
                new VBox(6, Ui.label("Difficulté (1 à 6)", "muted", "small"), diffRow));
        HBox totalRow = new HBox(10, Ui.label("Durée totale :", "muted"), total);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        VBox s2 = section("2. Les 3 combats", bossBoxes, totalRow);

        CarnageClear[] result = new CarnageClear[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer le clear", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            if (patch.getText() == null || patch.getText().isBlank()) {
                Ui.error("Indiquez le patch (ex. 6.1).");
                return;
            }
            CarnageClear c = new CarnageClear();
            if (existing != null) c.id = existing.id;
            c.patch = patch.getText().trim();
            c.period = period.getText() == null ? "" : period.getText().trim();
            c.difficulty = Ui.selectedInt(diff, 1);
            for (BossForm f : bosses) {
                BossRun b = new BossRun();
                b.name = f.name().getText() == null ? "" : f.name().getText().trim();
                b.seconds = f.seconds().getValue() == null ? 0 : f.seconds().getValue();
                b.team = f.team().members();
                if (!b.name.isBlank() || b.seconds > 0 || !b.team.isEmpty()) c.bosses.add(b);
            }
            result[0] = c;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, Ui.label(existing == null ? "Ajouter un clear" : "Modifier le clear", "h1"),
                Ui.label("Difficulté, durée de chaque combat et équipes utilisées.", "muted"), s1, s2);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        VBox root = new VBox(16, scroll, actions);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(24, 28, 24, 28));

        stage[0] = Ui.modal(owner, "Carnage chtonien - clear", root, 1100, 880);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static VBox field(String label, Node control) {
        return new VBox(4, Ui.label(label, "muted", "small"), control);
    }

    private static VBox section(String title, Node content, Node footer) {
        VBox box = new VBox(14, Ui.label(title, "h3"), content, footer);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(22));
        return box;
    }
}
