package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.AbyssClear;
import com.hoyoendgame.model.CarnageClear;
import com.hoyoendgame.model.TheatreClear;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Home page: one card per game (Genshin Impact, Honkai: Star Rail). */
public final class HomeView extends VBox {
    private final DataStore store = DataStore.get();
    private final StarRailStore hsr = StarRailStore.get();

    public HomeView(Runnable openGenshin, Runnable openStarRail) {
        Ui.useCss(this, "home.css", "genshin.css");
        setSpacing(18);
        setPadding(new Insets(24, 48, 40, 48));

        Label heading = Ui.label("Tous vos clears,", "hero");
        Label heading2 = Ui.label("au même endroit.", "hero", "neon-cyan");
        Label sub = Ui.label("Enregistrez vos équipes, suivez vos étoiles et comparez vos résultats patch après patch, jeu par jeu.", "muted");
        sub.setWrapText(true);
        VBox intro = new VBox(6, Ui.label("Suivi des endgames", "chip", "chip-accent"), heading, heading2, sub);
        intro.setPadding(new Insets(0, 0, 18, 0));

        HBox cards = new HBox(24, genshinCard(openGenshin), hsrCard(openStarRail));
        cards.setFillHeight(true);

        Label foot = Ui.label("Tout est enregistré en local dans le dossier data/ ", "muted", "small");
        getChildren().addAll(intro, cards, foot);
    }

    private Node genshinCard(Runnable open) {
        Label abyss = Ui.label("", "neon-green");
        Label theatre = Ui.label("", "neon-green");
        Label carnage = Ui.label("", "neon-green");
        Label counts = Ui.label("", "muted");

        Runnable refresh = () -> {
            abyss.setText(store.abyss.isEmpty() ? "—" : lastAbyss(store.abyss.get(0)));
            theatre.setText(store.theatre.isEmpty() ? "—" : lastTheatre(store.theatre.get(0)));
            if (store.carnage.isEmpty()) {
                carnage.setGraphic(null);
                carnage.setStyle("");
                carnage.setText("—");
            } else {
                CarnageClear c = store.carnage.get(0);
                Ui.setDifficulty(carnage, c.difficulty, c.totalSeconds() + " s", 30);
                // Glow in the colour of the difficulty picture; the text stays green.
                javafx.scene.paint.Color gc = Ui.diffColor(c.difficulty);
                carnage.setStyle(gc == null ? "" : "-fx-effect:dropshadow(gaussian," + Ui.rgba(Ui.hex(gc), 0.75) + ",12,0.25,0,0);");
            }
            counts.setText(store.teams.size() + " équipes · " + store.characters.size() + " personnages");
        };
        InvalidationListener l = o -> refresh.run();
        store.abyss.addListener(l);
        store.theatre.addListener(l);
        store.carnage.addListener(l);
        store.teams.addListener(l);
        store.characters.addListener(l);
        refresh.run();

        Button openBtn = Ui.button("Ouvrir Genshin Impact", "btn-primary");
        openBtn.setOnAction(e -> open.run());

        HBox title = new HBox(12, Ui.label("Genshin Impact", "h1"), Ui.grow(), Ui.label("Actif", "chip", "chip-accent"));
        title.setAlignment(Pos.CENTER_LEFT);
        title.getChildren().get(0).getStyleClass().add("h2");

        HBox foot = new HBox(12, counts, Ui.grow(), openBtn);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.setPadding(new Insets(12, 0, 0, 0));
        foot.getStyleClass().add("stat-line");

        VBox box = new VBox(4, title,
                Ui.label("Abysses spiralées, Théâtre de l'imaginarium et Carnage chtonien.", "muted"),
                spacer(10),
                statLine("Abysses spiralées", abyss),
                statLine("Théâtre de l'imaginarium", theatre),
                statLine("Carnage chtonien", carnage),
                spacer(8), foot);
        box.getStyleClass().add("glass-accent");
        box.setPadding(new Insets(30));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefWidth(500);
        return box;
    }

    private Node hsrCard(Runnable open) {
        Label moc = Ui.label("", "neon-green");
        Label pf = Ui.label("", "neon-green");
        Label apoc = Ui.label("", "neon-green");
        Label arb = Ui.label("", "neon-magenta", "strong");
        Label counts = Ui.label("", "muted");

        Runnable refresh = () -> {
            moc.setText(hsr.moc.isEmpty() ? "—" : MocPage.summary(hsr.moc.get(0)) + " · patch " + hsr.moc.get(0).patch);
            pf.setText(hsr.pf.isEmpty() ? "—" : PureFictionPage.summary(hsr.pf.get(0)) + " · patch " + hsr.pf.get(0).patch);
            apoc.setText(hsr.apoc.isEmpty() ? "—" : ApocPage.summary(hsr.apoc.get(0)) + " · patch " + hsr.apoc.get(0).patch);
            arb.setText(hsr.arbitration.isEmpty() ? "—" : ArbitrationPage.summary(hsr.arbitration.get(0))
                    + " · patch " + hsr.arbitration.get(0).patch + ArbitrationPage.frameSuffix(hsr.arbitration.get(0)));
            counts.setText(hsr.teams.size() + " équipes · " + hsr.characters.size() + " personnages");
        };
        InvalidationListener l = o -> refresh.run();
        hsr.moc.addListener(l);
        hsr.pf.addListener(l);
        hsr.apoc.addListener(l);
        hsr.arbitration.addListener(l);
        hsr.teams.addListener(l);
        hsr.characters.addListener(l);
        refresh.run();

        Button openBtn = Ui.button("Ouvrir Honkai: Star Rail", "btn-primary");
        openBtn.setOnAction(e -> open.run());

        HBox title = new HBox(12, Ui.label("Honkai: Star Rail", "h1"), Ui.grow(), Ui.label("Actif", "chip", "chip-accent"));
        title.setAlignment(Pos.CENTER_LEFT);
        title.getChildren().get(0).getStyleClass().add("h2");

        HBox foot = new HBox(12, counts, Ui.grow(), openBtn);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.setPadding(new Insets(12, 0, 0, 0));
        foot.getStyleClass().add("stat-line");

        // Arbitration is highlighted: purple frame, bigger line.
        HBox arbLine = statLine("Arbitre de l'instable", arb);
        arbLine.getStyleClass().add("hsr-featured-line");
        arbLine.setPadding(new Insets(20, 16, 20, 16));
        VBox.setMargin(arbLine, new Insets(6, 0, 6, 0));
        // contour = frame colour (none: like the other lines)
        Runnable styleArb = () -> {
            arbLine.getStyleClass().remove("hsr-featured-line");
            String frame = hsr.arbitration.isEmpty() ? "" : hsr.arbitration.get(0).frame();
            if (frame.isEmpty()) {
                arbLine.setStyle("");
            } else {
                String color = ArbitrationPage.frameColor(frame);
                arbLine.getStyleClass().add("hsr-featured-line");
                arbLine.setStyle("-fx-border-color:" + color + ";-fx-effect:dropshadow(gaussian," + Ui.rgba(color, 0.35)
                        + ",16,0,0,0);");
            }
        };
        hsr.arbitration.addListener((InvalidationListener) o -> styleArb.run());
        styleArb.run();

        VBox box = new VBox(4, title,
                Ui.label("Salle oubliée, Pure fiction, Fantômes de l'apocalypse et Arbitre de l'instable.", "muted"),
                spacer(10),
                statLine("Salle oubliée", moc),
                statLine("Pure fiction", pf),
                statLine("Fantômes de l'apocalypse", apoc),
                arbLine,
                spacer(8), foot);
        box.getStyleClass().add("glass-accent");
        box.setPadding(new Insets(30));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefWidth(500);
        return box;
    }

    private HBox statLine(String name, Label value) {
        HBox h = new HBox(12, Ui.label(name), Ui.grow(), value);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 0, 14, 0));
        h.getStyleClass().add("stat-line");
        return h;
    }

    private static Node spacer(double h) {
        VBox s = new VBox();
        s.setMinHeight(h);
        return s;
    }

    static String lastAbyss(AbyssClear c) {
        return c.total() + "/" + c.maxTotal() + " · patch " + c.patch;
    }

    static String lastTheatre(TheatreClear c) {
        return c.stars + "/" + c.mode.acts + " astres" + (c.mode.hasArcanum() ? " · Arcanum " + c.arcanum : "") + " · " + c.mode.label;
    }

    static String lastCarnage(CarnageClear c) {
        return "Diff " + c.difficulty + " · " + c.totalSeconds() + " s";
    }
}