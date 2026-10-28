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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
 * Add / edit a Theater clear: the 3 elements of the month, month + patch, mode (single-line
 * carousel), clickable stars (and Arcanum for Défi Lunaire), best act and the MVP character
 * (with an intro / borrowed precision).
 */
public final class TheatreAddDialog {
    private static final String ASTRE_COLOR = "#B79BFF";
    private static final String ARCANUM_COLOR = "#F6D77B";
    private static final int VISIBLE_MODES = 3;

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

        // ---- 3. mode: single-line carousel < [mode] [mode] [mode] >
        TheatreMode[] modes = TheatreMode.values();
        ToggleGroup modeGroup = new ToggleGroup();
        Ui.keepOneSelected(modeGroup);
        FlowPane modeStrip = new FlowPane(8, 8);
        modeStrip.setAlignment(Pos.CENTER_LEFT);
        Label modeReq = Ui.label("", "muted", "small");

        TheatreMode initialMode = existing == null ? TheatreMode.VISIONARY : existing.mode;
        // state[]: 0 = first visible mode index, 1 = selected mode index
        int[] state = {1, initialMode.ordinal()};
        if (state[0] > modes.length - VISIBLE_MODES) state[0] = modes.length - VISIBLE_MODES;
        if (state[1] > state[0] + VISIBLE_MODES - 1) state[0] = Math.min(state[1], modes.length - VISIBLE_MODES);

        Button prev = Ui.button("<", "btn-small", "carousel-btn");
        Button next = Ui.button(">", "btn-small", "carousel-btn");
        Runnable updCarousel = () -> {
            modeStrip.getChildren().clear();
            for (int i = state[0]; i < state[0] + VISIBLE_MODES && i < modes.length; i++) {
                TheatreMode m = modes[i];
                ToggleButton tb = new ToggleButton(m.label);
                tb.getStyleClass().add("pill");
                tb.setToggleGroup(modeGroup);
                tb.setUserData(m);
                tb.setSelected(i == state[1]);
                Tooltip t = new Tooltip(m.label + "\n" + m.requirements());
                Tooltip.install(tb, t);
                tb.setOnAction(e -> {
                    state[1] = m.ordinal();
                    modeReq.setText(m.requirements().replace("\n", " · "));
                });
                modeStrip.getChildren().add(tb);
            }
            prev.setDisable(state[0] == 0);
            next.setDisable(state[0] >= modes.length - VISIBLE_MODES);
            // Blue glow on ">" when the next mode to reveal is the Défi Lunaire
            boolean lunarNext = state[0] + VISIBLE_MODES < modes.length
                    && modes[state[0] + VISIBLE_MODES] == TheatreMode.LUNAR;
            next.getStyleClass().remove("glow-blue");
            if (lunarNext) next.getStyleClass().add("glow-blue");
        };
        prev.setOnAction(e -> {
            if (state[0] > 0) {
                state[0]--;
                updCarousel.run();
            }
        });
        next.setOnAction(e -> {
            if (state[0] < modes.length - VISIBLE_MODES) {
                state[0]++;
                updCarousel.run();
            }
        });
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b != null) {
                TheatreMode m = (TheatreMode) b.getUserData();
                state[1] = m.ordinal();
                modeReq.setText(m.requirements().replace("\n", " · "));
            }
        });
        HBox carousel = new HBox(8, prev, modeStrip, next);
        carousel.setAlignment(Pos.CENTER_LEFT);
        updCarousel.run();
        TheatreMode current = modes[state[1]];
        modeReq.setText(current.requirements().replace("\n", " · "));

        // ---- 4. result: clickable stars
        List<Boolean> starFlags = new ArrayList<>();
        if (existing != null) starFlags.addAll(existing.starFlagsOrFallback());
        while (starFlags.size() < current.acts) starFlags.add(starFlags.size() < existingStars(existing));
        HBox starsBox = new HBox(8);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        Label starsLabel = Ui.label("", "h3", "violet");
        Runnable[] rebuildStars = new Runnable[1];
        rebuildStars[0] = () -> {
            starsBox.getChildren().clear();
            TheatreMode m = modes[state[1]];
            int acts = m.acts;
            // In Défi Lunaire, the last stars (11 and 12) are the Arcanum ones: gold + separator.
            int arcStart = acts - (m.hasArcanum() ? m.arcanum : 0);
            while (starFlags.size() > acts) starFlags.remove(starFlags.size() - 1);
            while (starFlags.size() < acts) starFlags.add(false);
            for (int i = 0; i < acts; i++) {
                final int idx = i;
                final String onColor = idx >= arcStart ? ARCANUM_COLOR : ASTRE_COLOR;
                if (m.hasArcanum() && idx == arcStart) {
                    Region divider = new Region();
                    divider.setMinSize(2, 26);
                    divider.setPrefSize(2, 26);
                    divider.setStyle("-fx-background-color:rgba(255,255,255,0.35);-fx-background-radius:1;");
                    HBox.setMargin(divider, new Insets(0, 4, 0, 4));
                    starsBox.getChildren().add(divider);
                }
                javafx.scene.shape.Polygon star = Ui.star(28, flag(starFlags, idx), onColor);
                star.getStyleClass().add("clickable-star");
                star.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                    starFlags.set(idx, !flag(starFlags, idx));
                    star.setFill(Color.web(flag(starFlags, idx) ? onColor : "#3C3B5A"));
                    starsLabel.setText(countTrue(starFlags) + "/" + acts);
                });
                starsBox.getChildren().add(star);
            }
            starsLabel.setText(countTrue(starFlags) + "/" + acts);
        };
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> rebuildStars[0].run());
        rebuildStars[0].run();

        Spinner<Integer> best = Ui.spinner(0, current.acts, existing == null ? current.acts : existing.bestAct);
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) return;
            TheatreMode m = (TheatreMode) b.getUserData();
            setMax(best, m.acts);
        });

        HBox starsRow = new HBox(24,
                new VBox(4, Ui.label("Astres (cliquez sur les étoiles)", "muted", "small"),
                        new HBox(12, starsBox, starsLabel)),
                new VBox(4, Ui.label("Meilleur acte atteint", "muted", "small"), best));
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
        ScrollPane candidatesScroll = new ScrollPane(candidates);
        candidatesScroll.setFitToWidth(true);
        candidatesScroll.setPrefViewportHeight(210);
        candidatesScroll.setMaxHeight(230);

        CheckBox trial = new CheckBox("Personnage d'introduction / de prêt");
        trial.getStyleClass().add("mvp-trial");
        if (existing != null) trial.setSelected(existing.mvpTrial);
        VBox mvpContent = new VBox(14, candidatesScroll, trial);

        // ---- layout
        VBox s1 = section("1. Les 3 éléments du mois", elementsRow, elementCount);
        VBox s2 = section("2. Période", new HBox(18, field("Mois", month), field("Année", year), field("Patch", patch)), null);
        VBox s3 = section("3. Mode", new VBox(10, carousel, modeReq), null);
        VBox s4 = section("4. Résultat", starsRow,
                Ui.label("Astres maximum : Facile 3 · Normal 6 · Difficile 8 · Visionnaire 10 · Lunaire 12. "
                        + "Une étoile violette = obtenue, une étoile grise = manquée. "
                        + "En Défi Lunaire, les étoiles dorées comptent comme arcanum (1 étoile dorée = 1, 2 = 2).", "muted", "small"));
        VBox s5 = section("5. Personnage MVP", mvpContent,
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
            c.starFlags = new ArrayList<>(starFlags);
            c.stars = countTrue(starFlags);
            c.bestAct = best.getValue();
            c.arcanum = countArcanum(starFlags, c.mode);
            c.mvpTrial = trial.isSelected();
            if (mvpGroup.getSelectedToggle() == null) {
                c.mvp = null;
            } else {
                c.mvp = TeamMember.from((GenshinCharacter) mvpGroup.getSelectedToggle().getUserData());
            }
            c.normalize();
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

    private static boolean flag(List<Boolean> flags, int i) {
        return flags.get(i) != null && flags.get(i);
    }

    private static int countTrue(List<Boolean> flags) {
        int n = 0;
        for (Boolean b : flags) if (b != null && b) n++;
        return n;
    }

    private static int existingStars(TheatreClear existing) {
        return existing == null ? 0 : Math.max(0, Math.min(existing.mode == null ? 0 : existing.mode.acts, existing.stars));
    }

    private static int countArcanum(List<Boolean> flags, TheatreMode m) {
        if (m == null || !m.hasArcanum()) return 0;
        int arcStart = m.acts - m.arcanum;
        int n = 0;
        for (int i = arcStart; i < m.acts; i++) if (flag(flags, i)) n++;
        return n;
    }

    private static void setMax(Spinner<Integer> s, int max) {
        javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory f =
                (javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory) s.getValueFactory();
        f.setMax(max);
        if (f.getValue() != null && f.getValue() > max) f.setValue(max);
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