package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
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

/** Home page: one card per game (Genshin active, Honkai: Star Rail coming later). */
public final class HomeView extends VBox {
    private final DataStore store = DataStore.get();

    public HomeView(Runnable openGenshin) {
        setSpacing(18);
        setPadding(new Insets(24, 48, 40, 48));

        Label heading = Ui.label("Tous vos clears, au même endroit.", "h1");
        heading.setStyle("-fx-font-size:44px;");
        Label sub = Ui.label("Enregistrez vos équipes, suivez vos étoiles et comparez vos résultats patch après patch, jeu par jeu.", "muted");
        sub.setStyle("-fx-font-size:17px;");
        VBox intro = new VBox(10, Ui.label("Suivi des endgames", "chip"), heading, sub);
        intro.setPadding(new Insets(0, 0, 18, 0));

        HBox cards = new HBox(24, genshinCard(openGenshin), hsrCard());
        cards.setFillHeight(true);

        Label foot = Ui.label("Tout est enregistré en local dans le dossier data/ ", "muted", "small");
        getChildren().addAll(intro, cards, foot);
    }

    private Node genshinCard(Runnable open) {
        Label abyss = Ui.label("", "gold");
        Label theatre = Ui.label("", "gold");
        Label carnage = Ui.label("", "gold");
        Label counts = Ui.label("", "muted");

        Runnable refresh = () -> {
            abyss.setText(store.abyss.isEmpty() ? "—" : lastAbyss(store.abyss.get(0)));
            theatre.setText(store.theatre.isEmpty() ? "—" : lastTheatre(store.theatre.get(0)));
            carnage.setText(store.carnage.isEmpty() ? "—" : lastCarnage(store.carnage.get(0)));
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

        HBox title = new HBox(12, Ui.label("Genshin Impact", "h1"), Ui.grow(), Ui.label("Actif", "chip", "chip-gold"));
        title.setAlignment(Pos.CENTER_LEFT);
        ((Label) title.getChildren().get(0)).setStyle("-fx-font-size:28px;");

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
        box.getStyleClass().add("glass-gold");
        box.setPadding(new Insets(30));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefWidth(500);
        return box;
    }

    private Node hsrCard() {
        Button soon = Ui.button("Page à venir");
        soon.setDisable(true);
        Label t = Ui.label("Honkai: Star Rail", "h1");
        t.setStyle("-fx-font-size:28px;-fx-opacity:0.7;");
        HBox title = new HBox(12, t, Ui.grow(), Ui.label("Bientôt", "chip"));
        title.setAlignment(Pos.CENTER_LEFT);
        Label text = Ui.label("La page Honkai: Star Rail sera ajoutée dans une prochaine étape.", "muted");
        text.setWrapText(true);
        HBox bottom = new HBox(Ui.grow(), soon);
        VBox box = new VBox(10, title, text, Ui.grow(), bottom);
        box.getStyleClass().add("glass-dashed");
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
        return c.total() + "/36 · patch " + c.patch;
    }

    static String lastTheatre(TheatreClear c) {
        return "Acte " + c.bestAct + (c.mode.hasArcanum() ? " · Arcanum " + c.arcanum : "") + " · " + c.mode.label;
    }

    static String lastCarnage(CarnageClear c) {
        return "Diff " + c.difficulty + " · " + c.totalSeconds() + " s";
    }
}
