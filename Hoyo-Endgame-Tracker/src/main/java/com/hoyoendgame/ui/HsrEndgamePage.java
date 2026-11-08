package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.StarRailClear;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.Function;

/**
 * Generic Honkai: Star Rail endgame page (inside the main window), same layout as the Genshin ones:
 * history on the left (most recent patch first), detail of the selected clear on the right.
 * Each endgame only supplies its row, detail and add / edit window.
 */
public class HsrEndgamePage<T extends StarRailClear> extends VBox {
    /** Opens the add / edit window; returns the new clear, or empty when cancelled. */
    public interface Editor<T> {
        Optional<T> edit(Window owner, T existing);
    }

    private final ObservableList<T> items;
    private final Runnable save;
    private final Editor<T> editor;
    private final Function<T, Node> body;
    private final Function<T, String> subtitle;
    private final ListView<T> list;
    private final VBox detail = new VBox(16);

    /**
     * @param row      history row of a clear
     * @param subtitle line under "Clear du patch X" in the detail header
     * @param body     detail of a clear (below the header)
     */
    public HsrEndgamePage(String title, ObservableList<T> items, Runnable save, Editor<T> editor,
                          Function<T, Node> row, Function<T, String> subtitle, Function<T, Node> body, Runnable onBack) {
        this.items = items;
        this.save = save;
        this.editor = editor;
        this.body = body;
        this.subtitle = subtitle;
        this.list = new ListView<>(items);
        Ui.useCss(this, "starrail.css");
        setSpacing(20);
        setPadding(new Insets(28, 48, 40, 48));

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(row.apply(c));
                setText(null);
            }
        });
        list.setPlaceholder(Ui.label("Aucun clear enregistré.", "muted"));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));

        Button back = Ui.button("← Retour", "btn-small");
        back.setOnAction(e -> onBack.run());
        Button add = Ui.button("+ Ajouter un clear", "btn-primary");
        add.setOnAction(e -> editor.edit(add.getScene().getWindow(), null).ifPresent(c -> {
            items.add(c);
            StarRailStore.sort(items);
            save.run();
            list.getSelectionModel().select(c);
        }));

        HBox header = new HBox(16,
                new VBox(8, Ui.label("Honkai: Star Rail", "chip", "chip-accent"), Ui.label(title, "h1")),
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

        HBox content = new HBox(24, left, right);
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);

        list.getSelectionModel().selectFirst();
        if (items.isEmpty()) showDetail(null);
    }

    private void showDetail(T c) {
        detail.getChildren().clear();
        if (c == null) {
            detail.getChildren().add(Ui.label("Sélectionnez un clear dans l'historique, ou ajoutez-en un.", "muted"));
            return;
        }
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> editor.edit(edit.getScene().getWindow(), c).ifPresent(nc -> {
            int i = items.indexOf(c);
            if (i >= 0) items.set(i, nc);
            StarRailStore.sort(items);
            save.run();
            list.getSelectionModel().select(nc);
        }));
        del.setOnAction(e -> {
            if (Ui.confirm("Supprimer ce clear (patch " + c.patch() + ") ?")) {
                items.remove(c);
                save.run();
            }
        });
        HBox head = new HBox(16,
                new VBox(4, Ui.label("Clear du patch " + c.patch(), "h2"), Ui.label(subtitle.apply(c), "muted")),
                Ui.grow(), edit, del);
        head.setAlignment(Pos.CENTER_LEFT);
        detail.getChildren().addAll(head, body.apply(c));
    }

    /** Standard history row: patch + small text on the left, a highlighted value on the right. */
    protected static Node historyRow(String patch, String small, String value, boolean full) {
        Label v = Ui.label(value, full ? "neon-green" : "strong");
        HBox row = new HBox(10, new VBox(2, Ui.label("Patch " + patch, "strong"), Ui.label(small, "muted", "small")), Ui.grow(), v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
