package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.TeamMember;
import com.hoyoendgame.model.TheatreClear;
import com.hoyoendgame.model.TheatreMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Add / edit a Theater clear: the 3 elements of the month, month + patch, mode, stars (and Arcanum for
 * Défi Lunaire), best act and the MVP character.
 */
public final class TheatreAddDialog {
    private TheatreAddDialog() {
    }

    public static Optional<TheatreClear> show(Window owner, TheatreClear existing) {
        DataStore store = DataStore.get();
        LocalDate today = LocalDate.now();

        // ---- 1. elements (exactly 3)
        List<ToggleButton> elementButtons = new ArrayList<>();
        Label elementCount = Ui.label("", "muted", "small");
        FlowPane elementsRow = new FlowPane(10, 10);
        for (Element el : Element.values()) {
            ToggleButton tb = new ToggleButton(el.label(), new Circle(6, Color.web(el.color())));
            tb.getStyleClass().add("element-toggle");
            tb.setUserData(el);
            if (existing != null && existing.elements.contains(el)) tb.setSelected(true);
            elementButtons.add(tb);
            elementsRow.getChildren().add(tb);
        }
        Runnable updElements = () -> {
            long n = elementButtons.stream().filter(ToggleButton::isSelected).count();
            elementButtons.forEach(b -> b.setDisable(n >= 3 && !b.isSelected()));
            elementCount.setText("Sélection : " + n + "/3");
        };
        elementButtons.forEach(b -> b.selectedProperty().addListener((o, a, v) -> updElements.run()));
        updElements.run();

        // ---- 2. period
        ComboBox<String> month = new ComboBox<>();
        month.getItems().addAll(TheatreClear.months());
        month.getSelectionModel().select((existing == null ? today.getMonthValue() : existing.month) - 1);
        Spinner<Integer> year = Ui.spinner(2020, 2100, existing == null ? today.getYear() : existing.year);
        TextField patch = new TextField(existing == null ? "" : existing.patch);
        patch.setPromptText("ex. 6.1");
        patch.setPrefWidth(120);

        // ---- 3. mode
        ToggleGroup modeGroup = new ToggleGroup();
        FlowPane modes = new FlowPane(12, 12);
        TheatreMode initialMode = existing == null ? TheatreMode.LUNAR : existing.mode;
        for (TheatreMode m : TheatreMode.values()) {
            Label title = Ui.label(m.label, "strong");
            Label req = Ui.label(m.requirements(), "muted", "small");
            ToggleButton card = new ToggleButton();
            card.setGraphic(new VBox(6, title, req));
            card.getStyleClass().add("mode-card");
            card.setToggleGroup(modeGroup);
            card.setUserData(m);
            card.setPrefWidth(190);
            card.setSelected(m == initialMode);
            modes.getChildren().add(card);
        }
        Ui.keepOneSelected(modeGroup);

        // ---- 4. result
        Spinner<Integer> stars = Ui.spinner(0, initialMode.acts, existing == null ? initialMode.acts : existing.stars);
        Label starsMax = Ui.label("/" + initialMode.acts, "violet");
        Spinner<Integer> best = Ui.spinner(0, initialMode.acts, existing == null ? initialMode.acts : existing.bestAct);
        Spinner<Integer> arcanum = Ui.spinner(0, Math.max(2, initialMode.arcanum), existing == null ? 0 : existing.arcanum);
        Label arcMax = Ui.label("/2", "violet");
        VBox arcBox = new VBox(4, Ui.label("Arcanum (Défi Lunaire)", "muted", "small"), new HBox(8, arcanum, arcMax));
        ((HBox) arcBox.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        Runnable onMode = () -> {
            TheatreMode m = (TheatreMode) modeGroup.getSelectedToggle().getUserData();
            setMax(stars, m.acts);
            setMax(best, m.acts);
            starsMax.setText("/" + m.acts);
            arcBox.setVisible(m.hasArcanum());
            arcBox.setManaged(m.hasArcanum());
            if (!m.hasArcanum()) arcanum.getValueFactory().setValue(0);
        };
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> onMode.run());
        onMode.run();

        HBox starsRow = new HBox(24,
                new VBox(4, Ui.label("Astres (selon le mode)", "muted", "small"), hbox(8, stars, starsMax)),
                new VBox(4, Ui.label("Meilleur acte atteint", "muted", "small"), best),
                arcBox);
        starsRow.setAlignment(Pos.BOTTOM_LEFT);

        // ---- 5. MVP
        ToggleGroup mvpGroup = new ToggleGroup();
        FlowPane candidates = new FlowPane(8, 8);
        for (GenshinCharacter c : store.characters) {
            ToggleButton tb = new ToggleButton();
            tb.setGraphic(Ui.avatar(c.name, c.element, null, c.name, 56, 64));
            tb.getStyleClass().add("candidate");
            tb.setToggleGroup(mvpGroup);
            tb.setUserData(c);
            if (existing != null && existing.mvp != null && existing.mvp.name.equalsIgnoreCase(c.name)) tb.setSelected(true);
            candidates.getChildren().add(tb);
        }
        if (store.characters.isEmpty()) {
            candidates.getChildren().add(Ui.label("Ajoutez d'abord des personnages dans la page Genshin.", "muted"));
        }

        // ---- layout
        VBox s1 = section("1. Les 3 éléments du mois", elementsRow, elementCount);
        VBox s2 = section("2. Période", new HBox(18, field("Mois", month), field("Année", year), field("Patch", patch)), null);
        VBox s3 = section("3. Mode", modes, null);
        VBox s4 = section("4. Résultat", starsRow,
                Ui.label("Astres maximum : Facile 3 · Normal 6 · Difficile 8 · Visionnaire 10 · Lunaire 12. Exemple : 6/6 en mode Normal.", "muted", "small"));
        VBox s5 = section("5. Personnage MVP", candidates,
                Ui.label("Choisi parmi vos personnages enregistrés (cliquez à nouveau pour désélectionner).", "muted", "small"));

        TheatreClear[] result = new TheatreClear[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer le clear", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            List<Element> picked = new ArrayList<>();
            for (ToggleButton b : elementButtons) if (b.isSelected()) picked.add((Element) b.getUserData());
            if (picked.size() != 3) {
                Ui.error("Sélectionnez exactement 3 éléments.");
                return;
            }
            if (patch.getText() == null || patch.getText().isBlank()) {
                Ui.error("Indiquez le patch (ex. 6.1).");
                return;
            }
            TheatreClear c = new TheatreClear();
            if (existing != null) c.id = existing.id;
            c.elements = picked;
            c.month = month.getSelectionModel().getSelectedIndex() + 1;
            c.year = year.getValue();
            c.patch = patch.getText().trim();
            c.mode = (TheatreMode) modeGroup.getSelectedToggle().getUserData();
            c.stars = stars.getValue();
            c.bestAct = best.getValue();
            c.arcanum = c.mode.hasArcanum() ? arcanum.getValue() : 0;
            c.mvp = mvpGroup.getSelectedToggle() == null ? null
                    : TeamMember.from((GenshinCharacter) mvpGroup.getSelectedToggle().getUserData());
            result[0] = c;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, Ui.label(existing == null ? "Ajouter un clear" : "Modifier le clear", "h1"),
                Ui.label("Éléments du mois, période, mode, résultat et personnage MVP.", "muted"), s1, s2, s3, s4, s5);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        VBox root = new VBox(16, scroll, actions);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(24, 28, 24, 28));

        stage[0] = Ui.modal(owner, "Théâtre de l'imaginarium - clear", root, 1100, 880);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static void setMax(Spinner<Integer> s, int max) {
        SpinnerValueFactory.IntegerSpinnerValueFactory f = (SpinnerValueFactory.IntegerSpinnerValueFactory) s.getValueFactory();
        f.setMax(max);
        if (f.getValue() != null && f.getValue() > max) f.setValue(max);
    }

    private static HBox hbox(double gap, Node... nodes) {
        HBox h = new HBox(gap, nodes);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static VBox field(String label, Node control) {
        return new VBox(4, Ui.label(label, "muted", "small"), control);
    }

    private static VBox section(String title, Node content, Node footer) {
        VBox box = new VBox(14, Ui.label(title, "h3"), content);
        if (footer != null) box.getChildren().add(footer);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(22));
        return box;
    }
}
