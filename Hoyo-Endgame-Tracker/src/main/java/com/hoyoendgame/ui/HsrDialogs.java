package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.Team;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Frame and form helpers shared by the four Honkai: Star Rail "add / edit a clear" windows. */
final class HsrDialogs {
    private HsrDialogs() {
    }

    /**
     * Opens the modal window and returns what {@code build} returned when "Enregistrer" was pressed.
     * {@code build} returns null (after showing an error) to keep the window open.
     */
    static <T> Optional<T> show(Window owner, String windowTitle, String heading, String hint,
                                List<Node> sections, Supplier<T> build) {
        Object[] result = new Object[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            T r = build.get();
            if (r != null) {
                result[0] = r;
                stage[0].close();
            }
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, Ui.label(heading, "h1"), Ui.label(hint, "muted"));
        body.getChildren().addAll(sections);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        VBox root = new VBox(16, scroll, actions);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(24, 28, 24, 28));
        Ui.useCss(root, "starrail.css");

        stage[0] = Ui.modal(owner, windowTitle, root, 1180, 860);
        stage[0].showAndWait();
        @SuppressWarnings("unchecked")
        T t = (T) result[0];
        return Optional.ofNullable(t);
    }

    static VBox field(String label, Node control) {
        return new VBox(4, Ui.label(label, "muted", "small"), control);
    }

    static VBox section(String title, Node... content) {
        VBox box = new VBox(14, Ui.label(title, "h3"));
        box.getChildren().addAll(content);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(22));
        return box;
    }

    static int nz(Spinner<Integer> s) {
        return s.getValue() == null ? 0 : s.getValue();
    }

    /** A number spinner wide enough for scores like 120000. */
    static Spinner<Integer> scoreSpinner(int max, int value) {
        Spinner<Integer> s = Ui.spinner(0, max, value);
        s.setPrefWidth(140);
        return s;
    }

    static void setMax(Spinner<Integer> s, int max) {
        SpinnerValueFactory.IntegerSpinnerValueFactory f = (SpinnerValueFactory.IntegerSpinnerValueFactory) s.getValueFactory();
        f.setMax(max);
        if (f.getValue() == null || f.getValue() > max) f.setValue(max);
    }

    /** Changes the maximum of a spinner without raising the value (lowers it only when above the new maximum). */
    static void setMaxKeep(Spinner<Integer> s, int max) {
        SpinnerValueFactory.IntegerSpinnerValueFactory f = (SpinnerValueFactory.IntegerSpinnerValueFactory) s.getValueFactory();
        f.setMax(max);
        if (f.getValue() != null && f.getValue() > max) f.setValue(max);
    }

    /** One team editor (4 slots) with a "saved team" picker that fills it. */
    static VBox teamBox(String title, TeamPane pane) {
        ComboBox<Team> preset = new ComboBox<>();
        preset.getItems().addAll(StarRailStore.get().teams);
        preset.setPromptText("Équipe enregistrée…");
        preset.setPrefWidth(360);
        preset.setMaxWidth(360);
        preset.valueProperty().addListener((o, a, t) -> {
            if (t != null) pane.setMembers(t.copy().members);
        });
        VBox box = new VBox(10, field("Équipe pré-enregistrée", preset), pane);
        if (title != null && !title.isBlank()) box.getChildren().add(0, Ui.label(title, "strong"));
        return box;
    }

    /** Team editors one under the other, each one laid out horizontally (4 slots side by side). */
    static VBox teamsFlow(List<VBox> boxes) {
        VBox flow = new VBox(18);
        flow.getChildren().addAll(boxes);
        return flow;
    }

    static void setShown(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }
}
