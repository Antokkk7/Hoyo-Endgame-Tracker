package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.ApocClear;
import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Apocalyptic Shadow ("Fantômes de l'apocalypse"): difficulty, score and team of each boss. */
public final class ApocPage {
    private ApocPage() {
    }

    public static HsrEndgamePage<ApocClear> create(Runnable onBack) {
        StarRailStore store = StarRailStore.get();
        return new HsrEndgamePage<>("Fantômes de l'apocalypse", store.apoc, store::saveApoc, ApocPage::edit,
                ApocPage::row, c -> c.mode.label + " · " + HsrUi.num(c.totalScore()) + " / " + HsrUi.num(c.maxTotal()),
                ApocPage::detail, onBack);
    }

    static String summary(ApocClear c) {
        return HsrUi.num(c.totalScore());
    }

    private static Node row(ApocClear c) {
        HBox row = new HBox(10,
                new VBox(2, Ui.label("Patch " + c.patch, "strong"), Ui.label(c.mode.label, "muted", "small")),
                Ui.grow(), HsrUi.prismStar(c.stellar(), 16),
                Ui.label(HsrUi.num(c.totalScore()), c.totalScore() >= c.maxTotal() ? "neon-green" : "strong"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node detail(ApocClear c) {
        HBox top = new HBox(18,
                new VBox(2, Ui.label("Score total", "muted", "small"), Ui.label(HsrUi.num(c.totalScore()), "big-number", "neon-green")),
                Ui.label("/ " + HsrUi.num(c.maxTotal()), "muted"), Ui.grow(),
                new VBox(2, Ui.label("Étoile prismatique", "muted", "small"), HsrUi.prismStar(c.stellar(), 30)));
        top.setAlignment(Pos.CENTER_LEFT);
        VBox bosses = new VBox(8);
        for (int i = 0; i < c.mode.bosses; i++) {
            HBox r = new HBox(10, Ui.label("Boss " + (i + 1), "neon-cyan"), Ui.grow(), Ui.label(HsrUi.num(c.scoreOf(i)), "strong"),
                    Ui.label("/ " + HsrUi.num(ApocClear.BOSS_MAX), "muted", "small"));
            r.setAlignment(Pos.CENTER_LEFT);
            r.getStyleClass().add("inner-box");
            r.setPadding(new Insets(10, 16, 10, 16));
            bosses.getChildren().add(r);
        }
        return new VBox(16, Ui.label(c.mode.label, "h3"), top, bosses, Ui.label("Équipes", "h3"), HsrUi.teamsRow("Boss", c.teams));
    }

    private static Optional<ApocClear> edit(Window owner, ApocClear existing) {
        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 4.7");
        patch.setPrefWidth(140);
        ComboBox<ApocClear.Mode> mode = new ComboBox<>();
        mode.getItems().addAll(ApocClear.Mode.values());
        mode.setValue(existing == null ? ApocClear.Mode.D4 : existing.mode);

        List<Spinner<Integer>> scores = new ArrayList<>();
        List<Label> names = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        for (int i = 0; i < 3; i++) {
            Spinner<Integer> s = HsrDialogs.scoreSpinner(ApocClear.BOSS_MAX, existing == null ? 0 : existing.scoreOf(i));
            Label n = Ui.label("Boss " + (i + 1), "strong");
            scores.add(s);
            names.add(n);
            grid.add(n, 0, i);
            grid.add(s, 1, i);
            grid.add(Ui.label("/ " + HsrUi.num(ApocClear.BOSS_MAX), "muted"), 2, i);
        }
        Label total = Ui.label("", "big-number", "neon-green");

        List<TeamPane> panes = new ArrayList<>();
        List<VBox> boxes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TeamPane pane = new TeamPane(2, GameProfile.starRail());
            if (existing != null && i < existing.teams.size()) pane.setMembers(existing.teams.get(i));
            panes.add(pane);
            boxes.add(HsrDialogs.teamBox("Équipe du boss " + (i + 1), pane));
        }

        Runnable upd = () -> {
            int n = mode.getValue().bosses;
            int sum = 0;
            for (int i = 0; i < 3; i++) {
                boolean on = i < n;
                HsrDialogs.setShown(names.get(i), on);
                HsrDialogs.setShown(scores.get(i), on);
                HsrDialogs.setShown(boxes.get(i), on);
                if (on) sum += HsrDialogs.nz(scores.get(i));
            }
            // the "/ 4 000" labels are the 3rd grid column: hide those of unused rows too
            for (Node child : grid.getChildren()) {
                Integer row = GridPane.getRowIndex(child);
                if (row != null) HsrDialogs.setShown(child, row < n);
            }
            total.setText(HsrUi.num(sum) + " / " + HsrUi.num(ApocClear.BOSS_MAX * n));
        };
        mode.valueProperty().addListener((o, a, b) -> upd.run());
        for (Spinner<Integer> s : scores) s.valueProperty().addListener((o, a, b) -> upd.run());
        upd.run();

        HBox head = new HBox(24, HsrDialogs.field("Patch", patch), HsrDialogs.field("Difficulté", mode), Ui.grow(),
                new VBox(2, Ui.label("Score total", "muted", "small"), total));
        head.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = HsrDialogs.section("1. Patch, difficulté et scores", head, grid,
                Ui.label("Difficulté 3 et 4 : 2 boss. Difficulté 4 en mode stellaire : 3 boss.", "muted", "small"));
        VBox section2 = HsrDialogs.section("2. Équipe de chaque boss", HsrDialogs.teamsFlow(boxes));

        return HsrDialogs.show(owner, "Fantômes de l'apocalypse - clear", existing == null ? "Ajouter un clear" : "Modifier le clear",
                "Difficulté choisie, score de chaque boss, puis les équipes.", List.of(section1, section2), () -> {
                    if (patch.getText() == null || patch.getText().isBlank()) {
                        Ui.error("Indiquez le patch (ex. 4.7).");
                        return null;
                    }
                    ApocClear c = new ApocClear();
                    if (existing != null) c.id = existing.id;
                    c.patch = patch.getText().trim();
                    c.mode = mode.getValue();
                    for (int i = 0; i < c.mode.bosses; i++) {
                        c.scores.add(HsrDialogs.nz(scores.get(i)));
                        List<TeamMember> m = panes.get(i).members();
                        c.teams.add(m);
                    }
                    c.normalize();
                    return c;
                });
    }
}
