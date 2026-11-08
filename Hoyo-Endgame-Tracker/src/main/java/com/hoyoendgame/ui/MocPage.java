package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.MocClear;
import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Memory of Chaos ("Salle oubliée"): stars and cycles used per room, stellar mode of room 12, teams of the last room. */
public final class MocPage {
    private MocPage() {
    }

    public static HsrEndgamePage<MocClear> create(Runnable onBack) {
        StarRailStore store = StarRailStore.get();
        return new HsrEndgamePage<>("Salle oubliée", store.moc, store::saveMoc, MocPage::edit,
                MocPage::row, c -> c.totalStars() + "/36 étoiles · " + c.totalCycles() + " cycles", MocPage::detail, onBack);
    }

    static String summary(MocClear c) {
        return c.totalStars() + "/36" + (c.stellar ? " + ★" : "");
    }

    private static Node row(MocClear c) {
        HBox row = new HBox(10,
                new VBox(2, Ui.label("Patch " + c.patch, "strong"),
                        Ui.label(c.totalCycles() + " cycles" + (c.stellar ? " · mode stellaire" : ""), "muted", "small")),
                Ui.grow(), HsrUi.prismStar(c.stellar, 16),
                Ui.label(c.totalStars() + "/36", c.totalStars() == 36 ? "neon-green" : "strong"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node detail(MocClear c) {
        Label stars = Ui.label(c.totalStars() + "/36", "big-number", "neon-green");
        Label cycles = Ui.label(c.totalCycles() + " cycles utilisés au total", "muted");
        HBox top = new HBox(18, new VBox(2, Ui.label("Étoiles", "muted", "small"), stars),
                new VBox(2, Ui.label("Étoile prismatique", "muted", "small"), HsrUi.prismStar(c.stellar, 30)),
                Ui.grow(), cycles);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox rooms = new VBox(8);
        for (int i = 8; i < MocClear.ROOMS; i++) {
            Node starsNode = HsrUi.stars(c.starsOf(i), 3, 18);
            if (i == MocClear.ROOMS - 1) {
                // room 12: the purple (prismatic) star comes in addition to the three gold ones
                ((HBox) starsNode).getChildren().add(HsrUi.prismStar(c.stellar, 18));
            }
            String right = c.autoOf(i) ? "auto clear" : c.cyclesOf(i) + " cyc.";
            HBox r = new HBox(14, Ui.label("Salle " + (i + 1), "neon-cyan"), Ui.grow(), starsNode,
                    Ui.label(right, "muted", "small"));
            r.setAlignment(Pos.CENTER_LEFT);
            r.getStyleClass().add("inner-box");
            r.setPadding(new Insets(10, 16, 10, 16));
            rooms.getChildren().add(r);
        }
        return new VBox(16, top, rooms, Ui.label("Équipes de la dernière salle", "h3"), HsrUi.teamsRow("Équipe", c.teams));
    }

    private static Optional<MocClear> edit(Window owner, MocClear existing) {
        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 4.7");
        patch.setPrefWidth(140);

        // Rooms 1 to 8 are not tracked (3 stars). Rooms 9 and 10 can be "auto clear". Room 12 has the purple star.
        List<Spinner<Integer>> stars = new ArrayList<>();
        List<Spinner<Integer>> cycles = new ArrayList<>();
        List<CheckBox> autos = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        grid.add(Ui.label("Salle", "muted", "small"), 0, 0);
        grid.add(Ui.label("Étoiles (/3)", "muted", "small"), 1, 0);
        grid.add(Ui.label("Cycles utilisés", "muted", "small"), 2, 0);
        for (int k = 0; k < 4; k++) {
            int room = 8 + k;
            Spinner<Integer> s = Ui.spinner(0, 3, existing == null ? 3 : existing.starsOf(room));
            Spinner<Integer> cy = Ui.spinner(0, 99, existing == null ? 0 : existing.cyclesOf(room));
            stars.add(s);
            cycles.add(cy);
            grid.add(Ui.label("Salle " + (room + 1), "strong"), 0, k + 1);
            grid.add(s, 1, k + 1);
            grid.add(cy, 2, k + 1);
            if (k < 2) {
                CheckBox auto = new CheckBox("Auto clear");
                auto.setSelected(existing != null && existing.autoOf(room));
                autos.add(auto);
                grid.add(auto, 3, k + 1);
                Runnable apply = () -> {
                    boolean on = auto.isSelected();
                    s.setDisable(on);
                    cy.setDisable(on);
                    if (on) {
                        s.getValueFactory().setValue(3);
                        cy.getValueFactory().setValue(0);
                    }
                };
                auto.selectedProperty().addListener((o, a1, b1) -> apply.run());
                apply.run();
            }
        }
        CheckBox stellar = new CheckBox("Étoile prismatique (mode stellaire)");
        stellar.setSelected(existing != null && existing.stellar);
        grid.add(stellar, 3, 4);

        Label total = Ui.label("", "big-number", "neon-green");
        Label totalCycles = Ui.label("", "muted");
        Runnable upd = () -> {
            int st = 24, cy = 0;
            for (int k = 0; k < 4; k++) {
                st += HsrDialogs.nz(stars.get(k));
                cy += HsrDialogs.nz(cycles.get(k));
            }
            total.setText(st + "/36");
            totalCycles.setText(cy + " cycles au total");
        };
        for (Spinner<Integer> s : stars) s.valueProperty().addListener((o, a1, b1) -> upd.run());
        for (Spinner<Integer> s : cycles) s.valueProperty().addListener((o, a1, b1) -> upd.run());
        upd.run();

        HBox head = new HBox(24, HsrDialogs.field("Patch", patch), Ui.grow(),
                new VBox(2, Ui.label("Total", "muted", "small"), total), totalCycles);
        head.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = HsrDialogs.section("1. Patch, étoiles et cycles (salles 9 à 12)", head, grid,
                Ui.label("Les salles 1 à 8 ne sont pas suivies (comptées 3 étoiles).", "muted", "small"));

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

        return HsrDialogs.show(owner, "Salle oubliée - clear", existing == null ? "Ajouter un clear" : "Modifier le clear",
                "Étoiles et cycles de chaque salle, puis les équipes.", List.of(section1, section2), () -> {
                    if (patch.getText() == null || patch.getText().isBlank()) {
                        Ui.error("Indiquez le patch (ex. 4.7).");
                        return null;
                    }
                    MocClear c = new MocClear();
                    if (existing != null) c.id = existing.id;
                    c.patch = patch.getText().trim();
                    c.stellar = stellar.isSelected();
                    c.auto9 = autos.get(0).isSelected();
                    c.auto10 = autos.get(1).isSelected();
                    for (int i = 0; i < MocClear.ROOMS; i++) {
                        boolean tracked = i >= 8;
                        c.stars.add(tracked ? HsrDialogs.nz(stars.get(i - 8)) : 3);
                        c.cycles.add(tracked ? HsrDialogs.nz(cycles.get(i - 8)) : 0);
                    }
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
