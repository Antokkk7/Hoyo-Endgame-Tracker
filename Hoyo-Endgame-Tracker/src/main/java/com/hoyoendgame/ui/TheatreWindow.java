package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.TeamMember;
import com.hoyoendgame.model.TheatreClear;
import com.hoyoendgame.model.TheatreMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Imaginarium Theater window: monthly history on the left, retrospective-style detail on the right. */
public final class TheatreWindow {
    private static final String ASTRE_COLOR = "#B79BFF";
    private static Stage instance;

    public static void open(Window owner) {
        if (instance != null && instance.isShowing()) {
            instance.toFront();
            return;
        }
        instance = new TheatreWindow(owner).stage;
        instance.show();
    }

    private final DataStore store = DataStore.get();
    private final Stage stage;
    private final ListView<TheatreClear> list = new ListView<>(store.theatre);
    private final VBox detail = new VBox(8);

    private TheatreWindow(Window owner) {
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TheatreClear c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String arc = c.mode.hasArcanum() ? "Arcanum " + c.arcanum + "/" + c.mode.arcanum : "Arcanum —";
                HBox row = new HBox(10,
                        new VBox(2, Ui.label(c.monthLabel(), "strong"),
                                Ui.label("Patch " + c.patch + " · " + c.mode.label, "muted", "small")),
                        Ui.grow(),
                        new VBox(2, Ui.label("Astres " + c.stars + "/" + c.mode.acts, "violet"), Ui.label(arc, "muted", "small")));
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        list.setPlaceholder(Ui.label("Aucun clear enregistré.", "muted"));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));

        Button add = Ui.button("+ Ajouter un clear", "btn-primary");
        add.setOnAction(e -> TheatreAddDialog.show(add.getScene().getWindow(), null).ifPresent(c -> {
            store.theatre.add(c);
            store.sortTheatre();
            store.saveTheatre();
            list.getSelectionModel().select(c);
        }));

        HBox header = new HBox(16,
                new VBox(8, Ui.label("Genshin Impact", "chip", "chip-gold"), Ui.label("Théâtre de l'imaginarium", "h1")),
                Ui.grow(), add);
        header.setAlignment(Pos.BOTTOM_LEFT);

        VBox left = new VBox(14, Ui.label("Historique", "h3"), list);
        VBox.setVgrow(list, Priority.ALWAYS);
        left.getStyleClass().add("glass");
        left.setPadding(new Insets(20));
        left.setPrefWidth(420);
        left.setMinWidth(360);
        left.setMaxWidth(460);

        ScrollPane scroll = new ScrollPane(detail);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox right = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        right.getStyleClass().add("glass");
        right.setPadding(new Insets(12, 8, 12, 12));
        HBox.setHgrow(right, Priority.ALWAYS);
        detail.setPadding(new Insets(14));

        HBox body = new HBox(24, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(20, header, body);
        root.setPadding(new Insets(28, 36, 32, 36));

        stage = Ui.window(owner, "Théâtre de l'imaginarium", root, 1220, 820);
        list.getSelectionModel().selectFirst();
        if (store.theatre.isEmpty()) showDetail(null);
    }

    private void showDetail(TheatreClear c) {
        detail.getChildren().clear();
        if (c == null) {
            detail.getChildren().add(Ui.label("Sélectionnez un clear dans l'historique, ou ajoutez-en un.", "muted"));
            return;
        }
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> TheatreAddDialog.show(edit.getScene().getWindow(), c).ifPresent(nc -> {
            int i = store.theatre.indexOf(c);
            if (i >= 0) store.theatre.set(i, nc);
            store.sortTheatre();
            store.saveTheatre();
            list.getSelectionModel().select(nc);
        }));
        del.setOnAction(e -> {
            if (Ui.confirm("Supprimer le clear de " + c.monthLabel() + " ?")) {
                store.theatre.remove(c);
                store.saveTheatre();
            }
        });

        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().add(Ui.label(c.mode.label, "chip"));
        for (Element el : c.elements) chips.getChildren().add(Ui.chip(el.label(), el.color()));
        HBox head = new HBox(16,
                new VBox(10, Ui.label(c.monthLabel() + " · Patch " + c.patch, "h2"), chips),
                Ui.grow(), edit, del);
        head.setAlignment(Pos.TOP_LEFT);

        String best = "Acte " + c.bestAct + (c.mode.hasArcanum() ? " · Arcanum " + c.arcanum : "");
        Label bestLabel = Ui.label(best, "h3");

        // Stars strip ("astres de défi de vedette"), with a divider before the Arcanum stars.
        HBox strip = new HBox(6);
        strip.setAlignment(Pos.CENTER);
        strip.getStyleClass().add("strip");
        int max = c.mode.acts;
        int arcStart = c.mode.hasArcanum() ? max - c.mode.arcanum : max;
        for (int i = 0; i < max; i++) {
            if (c.mode.hasArcanum() && i == arcStart) {
                Region divider = new Region();
                divider.setMinSize(1, 22);
                divider.setPrefSize(1, 22);
                divider.setStyle("-fx-background-color:rgba(255,255,255,0.3);");
                HBox.setMargin(divider, new Insets(0, 4, 0, 4));
                strip.getChildren().add(divider);
            }
            strip.getChildren().add(Ui.star(24, i < c.stars, ASTRE_COLOR));
        }
        HBox starsValue = new HBox(16, strip, Ui.label(c.stars + "/" + max, "h3", "violet"));
        starsValue.setAlignment(Pos.CENTER_RIGHT);

        detail.getChildren().addAll(head,
                spacer(8),
                line("Meilleur score", bestLabel),
                line("Astres de défi de vedette", starsValue),
                line("Personnage MVP", mvp(c.mvp)));
    }

    private Node mvp(TeamMember m) {
        if (m == null) return Ui.label("Non défini", "muted");
        Label name = Ui.label(m.name, "h3");
        Label meta = Ui.label("Niv." + m.level + " · C" + m.constellation, "muted");
        VBox text = new VBox(4, name, meta, Ui.chip(m.element == null ? "" : m.element.label(),
                m.element == null ? null : m.element.color()));
        text.setAlignment(Pos.CENTER_RIGHT);
        HBox box = new HBox(16, text, Ui.avatar(m.name, m.element, null, null, 84, 96));
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private static HBox line(String label, Node value) {
        HBox h = new HBox(16, Ui.label(label), Ui.grow(), value);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(16, 0, 16, 0));
        h.getStyleClass().add("stat-line");
        return h;
    }

    private static Node spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}
