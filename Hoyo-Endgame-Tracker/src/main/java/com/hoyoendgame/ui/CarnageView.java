package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.BossRun;
import com.hoyoendgame.model.CarnageClear;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Stygian Onslaught page (inside the main window): difficulty (1~6) and fight
 * duration are the headline figures.
 */
public final class CarnageView extends VBox {
    private final DataStore store = DataStore.get();
    private final ListView<CarnageClear> list = new ListView<>(store.carnage);
    private final VBox detail = new VBox(14);

    public CarnageView(Runnable onBack) {
        setSpacing(20);
        setPadding(new Insets(28, 48, 40, 48));

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CarnageClear c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                VBox time = new VBox(0, Ui.label(c.totalSeconds() + " s", "h2"), Ui.label("durée totale", "muted", "small"));
                time.setAlignment(Pos.CENTER_RIGHT);
                HBox row = new HBox(12, Ui.diffBadge(c.difficulty, 48),
                        new VBox(2, Ui.label("Patch " + c.patch, "strong"), Ui.label(c.period, "muted", "small")),
                        Ui.grow(), time);
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
        add.setOnAction(e -> CarnageAddDialog.show(add.getScene().getWindow(), null).ifPresent(c -> {
            store.carnage.add(0, c);
            store.saveCarnage();
            list.getSelectionModel().select(0);
        }));

        HBox header = new HBox(16,
                new VBox(8, Ui.label("Genshin Impact", "chip", "chip-gold"), Ui.label("Carnage chtonien", "h1")),
                Ui.grow(), add, back);
        header.setAlignment(Pos.BOTTOM_LEFT);

        VBox left = new VBox(14, Ui.label("Historique", "h3"), list);
        VBox.setVgrow(list, Priority.ALWAYS);
        left.getStyleClass().add("glass");
        left.setPadding(new Insets(20));
        left.setPrefWidth(400);
        left.setMinWidth(360);
        left.setMaxWidth(440);

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

        if (store.carnage.isEmpty()) showDetail(null);
    }

    private void showDetail(CarnageClear c) {
        detail.getChildren().clear();
        if (c == null) {
            detail.getChildren().add(Ui.label("Sélectionnez un clear dans l'historique, ou ajoutez-en un.", "muted"));
            return;
        }
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> CarnageAddDialog.show(edit.getScene().getWindow(), c).ifPresent(nc -> {
            int i = store.carnage.indexOf(c);
            if (i >= 0) store.carnage.set(i, nc);
            store.saveCarnage();
            list.getSelectionModel().select(i);
        }));
        del.setOnAction(e -> {
            if (Ui.confirm("Supprimer ce clear (patch " + c.patch + ") ?")) {
                store.carnage.remove(c);
                store.saveCarnage();
            }
        });

        VBox best = new VBox(0, Ui.label("Meilleur score", "muted", "small"), Ui.label(c.totalSeconds() + " s", "h1"));
        best.setStyle("-fx-background-color:rgba(255,255,255,0.07);-fx-background-radius:14;-fx-padding:8 20 8 20;");
        Label diffTitle = Ui.label("Difficulté", "muted", "small");
        VBox diff = new VBox(2, Ui.diffBadge(c.difficulty, 72), diffTitle);
        diff.setAlignment(Pos.CENTER);

        HBox head = new HBox(16,
                new VBox(4, Ui.label("Patch " + c.patch, "h2"), Ui.label(c.period, "muted")),
                Ui.grow(), diff, best, edit, del);
        head.setAlignment(Pos.CENTER_LEFT);
        detail.getChildren().add(head);

        for (BossRun b : c.bosses) detail.getChildren().add(bossCard(b));
        if (c.bosses.isEmpty()) detail.getChildren().add(Ui.label("Aucun combat renseigné.", "muted"));
    }

    private Node bossCard(BossRun b) {
        StackPane portrait = new StackPane(Ui.label("Portrait\ndu boss", "muted", "small"));
        portrait.setMinSize(92, 92);
        portrait.setPrefSize(92, 92);
        portrait.setMaxSize(92, 92);
        portrait.setStyle("-fx-background-radius:16;-fx-background-color:linear-gradient(to bottom right,rgba(255,255,255,0.12),rgba(255,255,255,0.04));");

        FlowPane team = new FlowPane(8, 8);
        for (TeamMember m : b.team) team.getChildren().add(Ui.avatar(m.name, m.element, "Niv." + m.level, null, 56, 64));
        Label name = Ui.label(b.name.isBlank() ? "Boss" : b.name, "strong");
        name.setWrapText(true);
        VBox mid = new VBox(10, name, team);
        HBox.setHgrow(mid, Priority.ALWAYS);

        VBox dur = new VBox(0, Ui.label("Durée du combat", "muted", "small"), Ui.label(b.seconds + " s", "h2"));
        dur.setAlignment(Pos.CENTER);
        dur.setStyle("-fx-background-color:rgba(0,0,0,0.28);-fx-background-radius:14;-fx-padding:10 18 10 18;");

        HBox card = new HBox(16, portrait, mid, dur);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("inner-box");
        card.setPadding(new Insets(16));
        return card;
    }
}