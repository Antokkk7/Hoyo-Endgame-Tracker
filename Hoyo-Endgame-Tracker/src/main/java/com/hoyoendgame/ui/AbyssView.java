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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Spiral Abyss page (inside the main window): clear history on the left,
 * room-by-room detail of the cleared floor on the right.
 */
public final class AbyssView extends VBox {
    private final DataStore store = DataStore.get();
    private final ListView<AbyssClear> list = new ListView<>(store.abyss);
    private final VBox detail = new VBox(16);

    public AbyssView(Runnable onBack) {
        setSpacing(20);
        setPadding(new Insets(28, 48, 40, 48));

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AbyssClear c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean full = c.total() == c.maxTotal();
                Label stars = Ui.label(c.total() + "/" + c.maxTotal(), full ? "gold" : "strong");
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

        Button back = Ui.button("← Retour", "btn-small");
        back.setOnAction(e -> onBack.run());
        Button add = Ui.button("+ Ajouter un clear", "btn-primary");
        add.setOnAction(e -> AbyssAddDialog.show(add.getScene().getWindow(), null).ifPresent(c -> {
            store.abyss.add(0, c);
            store.saveAbyss();
            list.getSelectionModel().select(0);
        }));

        HBox header = new HBox(16,
                new VBox(8, Ui.label("Genshin Impact", "chip", "chip-gold"), Ui.label("Abysses spiralées", "h1")),
                Ui.grow(), add, back);
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
        HBox.setHgrow(body, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(header, body);

        list.getSelectionModel().selectFirst();
        if (store.abyss.isEmpty()) showDetail(null);
    }

    private void showDetail(AbyssClear c) {
        detail.getChildren().clear();
        if (c == null) {
            detail.getChildren().add(Ui.label("Sélectionnez un clear dans l'historique, ou ajoutez-en un.", "muted"));
            return;
        }

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
                        Ui.label(c.total() + "/" + c.maxTotal() + " étoiles · " + c.rotation, "muted")),
                Ui.grow(), edit, del);
        head.setAlignment(Pos.CENTER_LEFT);

        Label badge = Ui.label(String.valueOf(c.floor), "strong");
        badge.setMinSize(40, 40);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-radius:20;-fx-background-color:#E9E7F5;-fx-text-fill:#15142B;");
        HBox floorBar = new HBox(12, badge, Ui.label("Étage " + c.floor, "h3"), Ui.grow(),
                Ui.star(20, true, null), Ui.label(c.total() + "/" + c.maxTotal(), "gold"));
        floorBar.setAlignment(Pos.CENTER_LEFT);
        floorBar.getStyleClass().add("inner-box");
        floorBar.setPadding(new Insets(12, 16, 12, 16));

        Label baseInfo = Ui.label("Étoiles avant l'étage " + c.floor + " : " + c.baseStars + "/" + c.maxBase(), "muted");

        detail.getChildren().addAll(head, floorBar, baseInfo);
        for (int i = 0; i < 3; i++) {
            int roomStars = c.roomStars(i);
            HBox starRow = new HBox(4);
            for (int k = 0; k < 3; k++) starRow.getChildren().add(Ui.star(20, k < roomStars, null));
            HBox roomHead = new HBox(Ui.label("Salle " + (i + 1), "gold"), Ui.grow(), starRow);
            roomHead.setAlignment(Pos.CENTER_LEFT);
            HBox halves = new HBox(12, half("Équipe utilisée", c.team));
            detail.getChildren().add(new VBox(8, roomHead, halves));
        }
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