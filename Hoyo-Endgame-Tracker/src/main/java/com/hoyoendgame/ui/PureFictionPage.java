package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.PureFictionClear;
import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
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

/** Pure Fiction: score of each of the 4 rooms, stellar mode of room 4 (3 teams, 120000 max), teams of the last room. */
public final class PureFictionPage {
    private PureFictionPage() {
    }

    public static HsrEndgamePage<PureFictionClear> create(Runnable onBack) {
        StarRailStore store = StarRailStore.get();
        return new HsrEndgamePage<>("Pure fiction", store.pf, store::savePf, PureFictionPage::edit,
                PureFictionPage::row, c -> HsrUi.num(c.totalScore()) + " points (salle 4)" + (c.stellar ? " · mode stellaire" : ""),
                PureFictionPage::detail, onBack);
    }

    static String summary(PureFictionClear c) {
        return HsrUi.num(c.totalScore()) + " pts";
    }

    private static Node row(PureFictionClear c) {
        HBox row = new HBox(10,
                new VBox(2, Ui.label("Patch " + c.patch, "strong"), Ui.label(c.stellar ? "mode stellaire" : "mode normal", "muted", "small")),
                Ui.grow(), HsrUi.prismStar(c.stellar, 16),
                Ui.label(HsrUi.num(c.totalScore()), c.totalScore() >= c.maxTotal() ? "neon-green" : "strong"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node detail(PureFictionClear c) {
        Label total = Ui.label(HsrUi.num(c.totalScore()), "big-number", "neon-green");
        HBox top = new HBox(18, new VBox(2, Ui.label("Score total", "muted", "small"), total),
                Ui.label("/ " + HsrUi.num(c.maxTotal()), "muted"), Ui.grow(),
                new VBox(2, Ui.label("Étoile prismatique", "muted", "small"), HsrUi.prismStar(c.stellar, 30)));
        top.setAlignment(Pos.CENTER_LEFT);

        VBox rooms = new VBox(8);
        for (int i = 0; i < PureFictionClear.ROOMS; i++) {
            String name = "Salle " + (i + 1) + (i == PureFictionClear.ROOMS - 1 && c.stellar ? " · stellaire" : "");
            HBox r = new HBox(10, Ui.label(name, "neon-cyan"), Ui.grow(),
                    Ui.label(c.autoOf(i) ? "auto clear" : HsrUi.num(c.scoreOf(i)), "strong"),
                    Ui.label("/ " + HsrUi.num(c.maxOf(i)), "muted", "small"));
            r.setAlignment(Pos.CENTER_LEFT);
            r.getStyleClass().add("inner-box");
            r.setPadding(new Insets(10, 16, 10, 16));
            rooms.getChildren().add(r);
        }
        return new VBox(16, top, rooms, Ui.label("Équipes de la dernière salle", "h3"), HsrUi.teamsRow("Équipe", c.teams));
    }

    private static Optional<PureFictionClear> edit(Window owner, PureFictionClear existing) {
        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 4.7");
        patch.setPrefWidth(140);
        CheckBox stellar = new CheckBox("Mode stellaire (salle 4) : 3 équipes, 120 000 max");
        stellar.setSelected(existing != null && existing.stellar);

        List<Spinner<Integer>> scores = new ArrayList<>();
        List<Label> maxLabels = new ArrayList<>();
        List<CheckBox> autos = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        for (int i = 0; i < PureFictionClear.ROOMS; i++) {
            int max = i == PureFictionClear.ROOMS - 1 ? PureFictionClear.ROOM_MAX_STELLAR : PureFictionClear.ROOM_MAX;
            int def = existing != null ? existing.scoreOf(i) : 0;
            Spinner<Integer> s = HsrDialogs.scoreSpinner(max, def);
            Label maxLabel = Ui.label("/ " + HsrUi.num(PureFictionClear.ROOM_MAX), "muted");
            scores.add(s);
            maxLabels.add(maxLabel);
            grid.add(Ui.label("Salle " + (i + 1), "strong"), 0, i);
            grid.add(s, 1, i);
            grid.add(maxLabel, 2, i);
            if (i < 2) {
                CheckBox auto = new CheckBox("Auto clear");
                auto.setSelected(existing != null && existing.autoOf(i));
                autos.add(auto);
                grid.add(auto, 3, i);
                Runnable apply = () -> {
                    s.setDisable(auto.isSelected());
                    if (auto.isSelected()) s.getValueFactory().setValue(PureFictionClear.ROOM_MAX);
                };
                auto.selectedProperty().addListener((o, a1, b1) -> apply.run());
                apply.run();
            }
        }
        Label total = Ui.label("", "big-number", "neon-green");
        Runnable upd = () -> {
            int room4Max = stellar.isSelected() ? PureFictionClear.ROOM_MAX_STELLAR : PureFictionClear.ROOM_MAX;
            maxLabels.get(PureFictionClear.ROOMS - 1).setText("/ " + HsrUi.num(room4Max));
            HsrDialogs.setMaxKeep(scores.get(PureFictionClear.ROOMS - 1), room4Max);
            total.setText(HsrUi.num(HsrDialogs.nz(scores.get(PureFictionClear.ROOMS - 1))) + " / " + HsrUi.num(room4Max));
        };
        for (Spinner<Integer> s : scores) s.valueProperty().addListener((o, a1, b1) -> upd.run());
        stellar.selectedProperty().addListener((o, a1, b1) -> upd.run());
        upd.run();

        HBox head = new HBox(24, HsrDialogs.field("Patch", patch), stellar, Ui.grow(),
                new VBox(2, Ui.label("Score total (salle 4)", "muted", "small"), total));
        head.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = HsrDialogs.section("1. Patch et scores des salles", head, grid);

        List<TeamPane> panes = new ArrayList<>();
        List<VBox> boxes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TeamPane pane = new TeamPane(2, GameProfile.starRail());
            if (existing != null && i < existing.teams.size()) pane.setMembers(existing.teams.get(i));
            panes.add(pane);
            boxes.add(HsrDialogs.teamBox("Équipe " + (i + 1), pane));
        }
        HsrDialogs.setShown(boxes.get(2), stellar.isSelected());
        stellar.selectedProperty().addListener((o, a, b) -> HsrDialogs.setShown(boxes.get(2), b));
        VBox section2 = HsrDialogs.section("2. Équipes de la dernière salle", HsrDialogs.teamsFlow(boxes),
                Ui.label("La 3e équipe n'existe qu'en mode stellaire.", "muted", "small"));

        return HsrDialogs.show(owner, "Pure fiction - clear", existing == null ? "Ajouter un clear" : "Modifier le clear",
                "Score de chaque salle, puis les équipes.", List.of(section1, section2), () -> {
                    if (patch.getText() == null || patch.getText().isBlank()) {
                        Ui.error("Indiquez le patch (ex. 4.7).");
                        return null;
                    }
                    PureFictionClear c = new PureFictionClear();
                    if (existing != null) c.id = existing.id;
                    c.patch = patch.getText().trim();
                    c.stellar = stellar.isSelected();
                    c.auto1 = autos.get(0).isSelected();
                    c.auto2 = autos.get(1).isSelected();
                    for (Spinner<Integer> s : scores) c.scores.add(HsrDialogs.nz(s));
                    for (int i = 0; i < panes.size(); i++) {
                        if (i == 2 && !c.stellar) continue;
                        List<TeamMember> m = panes.get(i).members();
                        if (!m.isEmpty()) c.teams.add(m);
                    }
                    c.normalize();
                    return c;
                });
    }
}
