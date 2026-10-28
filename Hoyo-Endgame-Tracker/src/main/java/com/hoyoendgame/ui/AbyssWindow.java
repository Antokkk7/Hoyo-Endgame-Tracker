package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.AbyssClear;
import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/** Spiral Abyss window: clear history on the left, room-by-room detail on the right. */
public final class AbyssWindow {
    private static Stage instance;

    public static void open(Window owner) {
        if (instance != null && instance.isShowing()) {
            instance.toFront();
            return;
        }
        instance = new AbyssWindow(owner).stage;
        instance.show();
    }

    private final DataStore store = DataStore.get();
    private final Stage stage;
    private final ListView<AbyssClear> list = new ListView<>(store.abyss);
    private final VBox detail = new VBox(16);
    private int floor = 12;

    private AbyssWindow(Window owner) {
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AbyssClear c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean full = c.total() == 36;
                Label stars = Ui.label(c.total() + "/36", full ? "gold" : "strong");
                HBox row = new HBox(10,
                        new VBox(2, Ui.label("Patch " + c.patch, "strong"), Ui.label(c.rotation, "muted", "small")),
                        Ui.grow(), Ui.star(18, true, null), stars);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        list.setPlaceholder(Ui.label("Aucun clear enregistré.", "muted"));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));

        Button add = Ui.button("+ Ajouter un clear", "btn-primary");
        add.setOnAction(e -> AbyssAddDialog.show(add.getScene().getWindow(), null).ifPresent(c -> {
            store.abyss.add(0, c);
            store.saveAbyss();
            list.getSelectionModel().select(0);
        }));

        HBox header = new HBox(16,
                new VBox(8, Ui.label("Genshin Impact", "chip", "chip-gold"), Ui.label("Abysses spiralées", "h1")),
                Ui.grow(), add);
        header.setAlignment(Pos.BOTTOM_LEFT);

        VBox left = new VBox(14, Ui.label("Historique", "h3"), list);
        VBox.setVgrow(list, Priority.ALWAYS);
        left.getStyleClass().add("glass");
        left.setPadding(new Insets(20));
        left.setPrefWidth(380);
        left.setMinWidth(340);
        left.setMaxWidth(420);

        ScrollPane scroll = new ScrollPane(detail);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox right = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        right.getStyleClass().add("glass");
        right.setPadding(new Insets(12, 8, 12, 12));
        HBox.setHgrow(right, Priority.ALWAYS);
        detail.setPadding(new Insets(12));

        HBox body = new HBox(24, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(20, header, body);
        root.setPadding(new Insets(28, 36, 32, 36));

        stage = Ui.window(owner, "Abysses spiralées", root, 1220, 880);
        list.getSelectionModel().selectFirst();
        if (store.abyss.isEmpty()) showDetail(null);
    }

    private void showDetail(AbyssClear c) {
        detail.getChildren().clear();
        if (c == null) {
            detail.getChildren().add(Ui.label("Sélectionnez un clear dans l'historique, ou ajoutez-en un.", "muted"));
            return;
        }
        ToggleGroup g = new ToggleGroup();
        ToggleButton t11 = tab("Étage 11", g);
        ToggleButton t12 = tab("Étage 12", g);
        (floor == 11 ? t11 : t12).setSelected(true);
        Ui.keepOneSelected(g);
        t11.setOnAction(e -> {
            floor = 11;
            showDetail(c);
        });
        t12.setOnAction(e -> {
            floor = 12;
            showDetail(c);
        });
        HBox tabs = new HBox(4, t11, t12);
        tabs.getStyleClass().add("tab-bar");

        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> AbyssAddDialog.show(edit.getScene().getWindow(), c).ifPresent(nc -> {
            int i = store.abyss.indexOf(c);
            if (i >= 0) store.abyss.set(i, nc);
            store.saveAbyss();
            list.getSelectionModel().select(i);
        }));
        del.setOnAction(e -> {
            if (Ui.confirm("Supprimer ce clear (patch " + c.patch + ") ?")) {
                store.abyss.remove(c);
                store.saveAbyss();
            }
        });

        HBox head = new HBox(16,
                new VBox(4, Ui.label("Clear du patch " + c.patch, "h2"),
                        Ui.label(c.total() + "/36 étoiles · " + c.rotation, "muted")),
                Ui.grow(), tabs, edit, del);
        head.setAlignment(Pos.CENTER_LEFT);

        int stars = floor == 11 ? c.floor11 : c.floor12;
        Label badge = Ui.label(String.valueOf(floor), "strong");
        badge.setMinSize(40, 40);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-radius:20;-fx-background-color:#E9E7F5;-fx-text-fill:#15142B;");
        HBox floorBar = new HBox(12, badge, Ui.label("Étage " + floor, "h3"), Ui.grow(),
                Ui.star(20, true, null), Ui.label(stars + "/9", "gold"));
        floorBar.setAlignment(Pos.CENTER_LEFT);
        floorBar.getStyleClass().add("inner-box");
        floorBar.setPadding(new Insets(12, 16, 12, 16));

        detail.getChildren().addAll(head, floorBar);
        for (int i = 0; i < 3; i++) {
            int roomStars = Math.max(0, Math.min(3, stars - 3 * i));
            HBox starRow = new HBox(4);
            for (int k = 0; k < 3; k++) starRow.getChildren().add(Ui.star(20, k < roomStars, null));
            HBox roomHead = new HBox(Ui.label("Salle " + (i + 1), "gold"), Ui.grow(), starRow);
            roomHead.setAlignment(Pos.CENTER_LEFT);
            HBox halves = new HBox(12, half("Première moitié", c.team1), half("Seconde moitié", c.team2));
            detail.getChildren().add(new VBox(8, roomHead, halves));
        }
    }

    private static ToggleButton tab(String text, ToggleGroup g) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("tab");
        b.setToggleGroup(g);
        return b;
    }

    private Node half(String title, List<TeamMember> team) {
        FlowPane avatars = new FlowPane(8, 8);
        for (TeamMember m : team) avatars.getChildren().add(Ui.avatar(m.name, m.element, "Niv." + m.level, null, 56, 64));
        if (team.isEmpty()) avatars.getChildren().add(Ui.label("Équipe non renseignée", "muted", "small"));
        VBox box = new VBox(10, Ui.label(title, "muted", "small", "strong"), avatars);
        box.getStyleClass().add("inner-box");
        box.setPadding(new Insets(14));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefWidth(300);
        return box;
    }
}
