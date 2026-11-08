package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.ArbitrationClear;
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

/** Arbiter of the Unstable ("Arbitre de l'instable"): 3 Horsemen (cycles, K.O.), then the King (cycles, stars, frame, team). */
public final class ArbitrationPage {
    private ArbitrationPage() {
    }

    public static HsrEndgamePage<ArbitrationClear> create(Runnable onBack) {
        StarRailStore store = StarRailStore.get();
        return new HsrEndgamePage<>("Arbitre de l'instable", store.arbitration, store::saveArbitration, ArbitrationPage::edit,
                ArbitrationPage::row, c -> c.totalStars() + "/12 étoiles" + frameSuffix(c), ArbitrationPage::detail, onBack);
    }

    static String summary(ArbitrationClear c) {
        return c.totalStars() + "/12";
    }

    static String frameSuffix(ArbitrationClear c) {
        return c.frame().isEmpty() ? "" : " · " + c.frame();
    }

    /** Colour of a frame name. */
    static String frameColor(String frame) {
        return switch (frame) {
            case "Cadre Bronze" -> "#CD7F32";
            case "Cadre Argent" -> "#C8D0DC";
            case "Cadre Dorée" -> Ui.STAR_GOLD;
            case "Cadre Prismatique" -> Ui.STAR_PRISM;
            default -> "#5F656E";
        };
    }

    static Node frameText(String frame) {
        return frameLabel(frame);
    }

    private static Node frameLabel(String frame) {
        Label l = Ui.label(frame.isEmpty() ? "Aucun cadre" : frame, "strong");
        l.setStyle("-fx-text-fill:" + frameColor(frame) + ";");
        return l;
    }

    private static Node row(ArbitrationClear c) {
        HBox row = new HBox(10,
                new VBox(2, Ui.label("Patch " + c.patch, "strong"), Ui.label(c.frame().isEmpty() ? "pas de cadre" : c.frame(), "muted", "small")),
                Ui.grow(), Ui.label(c.totalStars() + "/12", c.totalStars() == 12 ? "neon-green" : "strong"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node detail(ArbitrationClear c) {
        HBox top = new HBox(18,
                new VBox(2, Ui.label("Étoiles", "muted", "small"), Ui.label(c.totalStars() + "/12", "big-number", "neon-green")),
                Ui.grow(), new VBox(2, Ui.label("Cadre du Roi", "muted", "small"), frameLabel(c.frame())));
        top.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(10);
        for (int i = 0; i < ArbitrationClear.HORSEMEN; i++) {
            list.getChildren().add(fightBlock(Ui.label("Cavalier " + (i + 1), "neon-cyan"),
                    c.cyclesOf(i) + " cycles" + (c.downOf(i) ? " · K.O." : ""), c.starsOf(i),
                    i < c.horsemenTeams.size() ? c.horsemenTeams.get(i) : List.of()));
        }
        list.getChildren().add(fightBlock(Ui.label("Le Roi · King", "neon-magenta"),
                c.kingCycles + " cycles" + (c.kingDown ? " · K.O." : ""), c.kingStars(), c.team));
        return new VBox(16, top, list);
    }

    /** One fight: the framed result line, then its team (no frame) under it. */
    private static Node fightBlock(Label name, String cycles, int stars, List<com.hoyoendgame.model.TeamMember> team) {
        HBox r = new HBox(10, name, Ui.grow(), Ui.label(cycles, "muted", "small"), HsrUi.stars(stars, 3, 16));
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("inner-box");
        r.setPadding(new Insets(10, 16, 10, 16));
        VBox box = new VBox(6, r);
        if (!team.isEmpty()) box.getChildren().add(HsrUi.teamBox(null, team));
        return box;
    }

    private static Optional<ArbitrationClear> edit(Window owner, ArbitrationClear existing) {
        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 4.7");
        patch.setPrefWidth(140);

        List<Spinner<Integer>> cycles = new ArrayList<>();
        List<CheckBox> downs = new ArrayList<>();
        List<Label> starLabels = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        grid.add(Ui.label("Cavalier", "muted", "small"), 0, 0);
        grid.add(Ui.label("Cycles utilisés", "muted", "small"), 1, 0);
        grid.add(Ui.label("Personnage K.O.", "muted", "small"), 2, 0);
        grid.add(Ui.label("Étoiles", "muted", "small"), 3, 0);
        for (int i = 0; i < ArbitrationClear.HORSEMEN; i++) {
            Spinner<Integer> cy = Ui.spinner(0, 99, existing == null ? 0 : existing.cyclesOf(i));
            CheckBox down = new CheckBox("K.O. (-1 étoile)");
            down.setSelected(existing != null && existing.downOf(i));
            Label st = Ui.label("", "strong");
            cycles.add(cy);
            downs.add(down);
            starLabels.add(st);
            grid.add(Ui.label("Cavalier " + (i + 1), "strong"), 0, i + 1);
            grid.add(cy, 1, i + 1);
            grid.add(down, 2, i + 1);
            grid.add(st, 3, i + 1);
        }

        Spinner<Integer> kingCycles = Ui.spinner(0, 99, existing == null ? 0 : existing.kingCycles);
        CheckBox kingDown = new CheckBox("K.O. (-1 étoile)");
        kingDown.setSelected(existing != null && existing.kingDown);
        Label kingStarsLabel = Ui.label("", "strong");
        Label frame = Ui.label("", "strong");
        Label total = Ui.label("", "big-number", "neon-green");
        Runnable upd = () -> {
            int sum = 0;
            for (int i = 0; i < ArbitrationClear.HORSEMEN; i++) {
                int s = Math.max(0, ArbitrationClear.starsForCycles(HsrDialogs.nz(cycles.get(i))) - (downs.get(i).isSelected() ? 1 : 0));
                starLabels.get(i).setText(s + " ★");
                sum += s;
            }
            ArbitrationClear tmp = new ArbitrationClear();
            tmp.kingCycles = HsrDialogs.nz(kingCycles);
            tmp.kingDown = kingDown.isSelected();
            sum += tmp.kingStars();
            kingStarsLabel.setText(tmp.kingStars() + " ★");
            total.setText(sum + "/12");
            String f = tmp.frame();
            frame.setText(f.isEmpty() ? "Aucun cadre" : f);
            frame.setStyle("-fx-text-fill:" + frameColor(f) + ";");
        };
        for (Spinner<Integer> s : cycles) s.valueProperty().addListener((o, a, b) -> upd.run());
        for (CheckBox d : downs) d.selectedProperty().addListener((o, a, b) -> upd.run());
        kingCycles.valueProperty().addListener((o, a, b) -> upd.run());
        kingDown.selectedProperty().addListener((o, a, b) -> upd.run());
        upd.run();

        HBox head = new HBox(24, HsrDialogs.field("Patch", patch), Ui.grow(),
                new VBox(2, Ui.label("Total", "muted", "small"), total));
        head.setAlignment(Pos.BOTTOM_LEFT);
        VBox section1 = HsrDialogs.section("1. Patch, cycles et K.O. des Cavaliers", head, grid,
                Ui.label("0-2 cycles : 3 étoiles · 3-4 : 2 · 5-6 : 1 · plus de 6 : 0. Un personnage K.O. retire 1 étoile (minimum 0).",
                        "muted", "small"));

        HBox kingRow = new HBox(24, HsrDialogs.field("Cycles utilisés", kingCycles), HsrDialogs.field("Personnage K.O.", kingDown),
                HsrDialogs.field("Étoiles", kingStarsLabel), HsrDialogs.field("Cadre obtenu", frame));
        kingRow.setAlignment(Pos.BOTTOM_LEFT);

        // 2. to 4.: one team per Horseman, 5.: the King
        List<TeamPane> horsemenPanes = new ArrayList<>();
        List<Node> sections = new ArrayList<>();
        sections.add(section1);
        for (int i = 0; i < ArbitrationClear.HORSEMEN; i++) {
            TeamPane hp = new TeamPane(2, GameProfile.starRail());
            if (existing != null && i < existing.horsemenTeams.size()) hp.setMembers(existing.horsemenTeams.get(i));
            horsemenPanes.add(hp);
            sections.add(HsrDialogs.section((i + 2) + ". Cavalier " + (i + 1), HsrDialogs.teamBox(null, hp)));
        }
        TeamPane pane = new TeamPane(2, GameProfile.starRail());
        if (existing != null) pane.setMembers(existing.team);
        sections.add(HsrDialogs.section("5. King", kingRow, HsrDialogs.teamBox(null, pane),
                Ui.label("Mêmes règles que les Cavaliers. Cadre : 1 étoile Bronze, 2 Argent, 3 Dorée ; 3 étoiles en 0 cycle : Prismatique.", "muted", "small")));

        return HsrDialogs.show(owner, "Arbitre de l'instable - clear", existing == null ? "Ajouter un clear" : "Modifier le clear",
                "Cycles de chaque Cavalier, puis le King, et l'équipe de chaque combat.", sections, () -> {
                    if (patch.getText() == null || patch.getText().isBlank()) {
                        Ui.error("Indiquez le patch (ex. 4.7).");
                        return null;
                    }
                    ArbitrationClear c = new ArbitrationClear();
                    if (existing != null) c.id = existing.id;
                    c.patch = patch.getText().trim();
                    for (int i = 0; i < ArbitrationClear.HORSEMEN; i++) {
                        c.cycles.add(HsrDialogs.nz(cycles.get(i)));
                        c.down.add(downs.get(i).isSelected());
                    }
                    c.kingCycles = HsrDialogs.nz(kingCycles);
                    c.kingDown = kingDown.isSelected();
                    c.team = pane.members();
                    for (TeamPane hp : horsemenPanes) c.horsemenTeams.add(hp.members());
                    c.normalize();
                    return c;
                });
    }
}
