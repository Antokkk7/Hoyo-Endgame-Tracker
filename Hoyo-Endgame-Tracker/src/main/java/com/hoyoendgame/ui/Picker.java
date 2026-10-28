package com.hoyoendgame.ui;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Modal "search and pick" dialog, used for the long lists (characters, weapons). */
public final class Picker {
    private Picker() {
    }

    /**
     * @param label text shown for each item, also what the search box filters on
     * @return the chosen item, or empty if the dialog was cancelled
     */
    public static <T> Optional<T> pick(Window owner, String title, List<T> items, Function<T, String> label) {
        TextField search = new TextField();
        search.setPromptText("Rechercher…");

        FilteredList<T> filtered = new FilteredList<>(FXCollections.observableArrayList(items), t -> true);
        search.textProperty().addListener((o, oldText, q) -> {
            String s = q == null ? "" : q.trim().toLowerCase();
            filtered.setPredicate(t -> s.isEmpty() || label.apply(t).toLowerCase().contains(s));
        });

        ListView<T> list = new ListView<>(filtered);
        list.setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : label.apply(item));
            }
        });
        list.setPlaceholder(Ui.label("Aucun résultat.", "muted"));

        AtomicReference<T> result = new AtomicReference<>();
        Stage[] stage = new Stage[1];
        Runnable choose = () -> {
            T sel = list.getSelectionModel().getSelectedItem();
            if (sel == null && !filtered.isEmpty()) sel = filtered.get(0);
            if (sel != null) {
                result.set(sel);
                stage[0].close();
            }
        };
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) choose.run();
        });
        search.setOnAction(e -> choose.run());

        Button cancel = Ui.button("Annuler");
        Button ok = Ui.button("Choisir", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        ok.setOnAction(e -> choose.run());
        HBox actions = new HBox(12, Ui.grow(), cancel, ok);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, Ui.label(title, "h3"), search, list, actions);
        VBox.setVgrow(list, Priority.ALWAYS);
        root.setPadding(new Insets(22));

        stage[0] = Ui.modal(owner, title, root, 460, 640);
        stage[0].setOnShown(e -> search.requestFocus());
        stage[0].showAndWait();
        return Optional.ofNullable(result.get());
    }
}
