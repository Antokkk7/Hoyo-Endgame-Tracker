package com.hoyoendgame.ui;

import com.hoyoendgame.data.ExcelService;
import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TeamMember;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Honkai: Star Rail page, same layout as {@link GenshinView}: teams and roster on the left,
 * the four endgames on the right (Arbitration highlighted).
 */
public final class StarRailView extends VBox {
    private final StarRailStore store = StarRailStore.get();
    private final GameProfile profile = GameProfile.starRail();
    private final Runnable openMoc;
    private final Runnable openPf;
    private final Runnable openApoc;
    private final Runnable openArbitration;

    private final ToggleButton teamsTab = new ToggleButton();
    private final ToggleButton charsTab = new ToggleButton();
    private final VBox teamsBox = new VBox(12);
    private final TextField search = new TextField();
    private final FilteredList<GenshinCharacter> filteredChars = new FilteredList<>(store.characters, c -> true);
    private final Button newTeam = Ui.button("+ NEW", "btn-primary");

    public StarRailView(Runnable openMoc, Runnable openPf, Runnable openApoc, Runnable openArbitration) {
        this.openMoc = openMoc;
        this.openPf = openPf;
        this.openApoc = openApoc;
        this.openArbitration = openArbitration;
        Ui.useCss(this, "starrail.css");
        setSpacing(24);
        setPadding(new Insets(8, 48, 40, 48));

        Button imp = Ui.button("Importer depuis Excel");
        Button exp = Ui.button("Exporter vers Excel");
        imp.setOnAction(e -> importExcel());
        exp.setOnAction(e -> exportExcel());

        Label chip = Ui.label("Honkai: Star Rail", "chip", "chip-accent");
        VBox titles = new VBox(6, chip, Ui.label("Équipes et endgames", "h1"),
                Ui.label("Personnages et équipes enregistrés, puis accès à chaque endgame.", "muted"));
        titles.setAlignment(Pos.TOP_LEFT);
        HBox header = new HBox(12, titles, Ui.grow(), imp, exp);
        header.setAlignment(Pos.BOTTOM_LEFT);

        HBox body = new HBox(24, buildLeft(), buildRight());
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(header, body);

        store.teams.addListener((javafx.beans.InvalidationListener) o -> refreshTeams());
        store.characters.addListener((javafx.beans.InvalidationListener) o -> refreshTeams());
        search.textProperty().addListener((o, a, b) -> {
            refreshTeams();
            filteredChars.setPredicate(c -> b == null || b.isBlank() || c.name.toLowerCase().contains(b.trim().toLowerCase()));
        });
        refreshTeams();
    }

    //   left: teams / roster

    private Node buildLeft() {
        ToggleGroup g = new ToggleGroup();
        teamsTab.setToggleGroup(g);
        charsTab.setToggleGroup(g);
        teamsTab.getStyleClass().add("tab");
        charsTab.getStyleClass().add("tab");
        teamsTab.setSelected(true);
        Ui.keepOneSelected(g);
        HBox tabs = new HBox(4, teamsTab, charsTab);
        tabs.getStyleClass().add("tab-bar");

        newTeam.setOnAction(e -> {
            Optional<Team> t = TeamEditorDialog.show(getScene().getWindow(), null, profile);
            t.ifPresent(team -> {
                store.teams.add(team);
                store.saveTeams();
            });
        });

        HBox top = new HBox(12, tabs, Ui.grow(), newTeam);
        top.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Rechercher une équipe ou un personnage");

        ScrollPane teamsScroll = new ScrollPane(teamsBox);
        teamsScroll.setFitToWidth(true);
        teamsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        Node charsPane = buildCharactersPane();

        StackPane content = new StackPane(teamsScroll, charsPane);
        VBox.setVgrow(content, Priority.ALWAYS);
        charsPane.setVisible(false);
        g.selectedToggleProperty().addListener((o, a, b) -> {
            boolean teams = b == teamsTab;
            teamsScroll.setVisible(teams);
            charsPane.setVisible(!teams);
            newTeam.setVisible(teams);
            newTeam.setManaged(teams);
        });

        VBox box = new VBox(14, top, search, content);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(20));
        box.setPrefWidth(540);
        box.setMinWidth(480);
        box.setMaxWidth(580);
        box.setPrefHeight(760);
        return box;
    }

    private void refreshTeams() {
        teamsTab.setText("Équipes (" + store.teams.size() + ")");
        charsTab.setText("Personnages dispos (" + store.characters.size() + ")");
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        teamsBox.getChildren().clear();
        int shown = 0;
        for (Team t : store.teams) {
            boolean match = q.isEmpty() || t.name.toLowerCase().contains(q)
                    || t.members.stream().anyMatch(m -> m.name.toLowerCase().contains(q));
            if (match) {
                teamsBox.getChildren().add(teamCard(t));
                shown++;
            }
        }
        if (shown == 0) {
            teamsBox.getChildren().add(Ui.label(store.teams.isEmpty()
                    ? "Aucune équipe enregistrée. Cliquez sur « + NEW »."
                    : "Aucune équipe ne correspond à la recherche.", "muted"));
        }
    }

    private Node teamCard(Team t) {
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> TeamEditorDialog.show(getScene().getWindow(), t, profile).ifPresent(nt -> {
            int i = store.teams.indexOf(t);
            if (i >= 0) store.teams.set(i, nt);
            store.saveTeams();
        }));
        del.setOnAction(e -> {
            if (Ui.confirm("Supprimer l'équipe « " + t.name + " » ?")) {
                store.teams.remove(t);
                store.saveTeams();
            }
        });
        HBox head = new HBox(8, new VBox(2, Ui.label(t.name, "strong"), Ui.label(t.usage, "muted", "small")), Ui.grow(), edit, del);
        head.setAlignment(Pos.CENTER_LEFT);
        FlowPane members = new FlowPane(8, 8);
        for (TeamMember m : t.members) {
            VBox av = Ui.avatar(profile.characterImages, m.name, m.element, "Niv." + m.level,
                    m.meta(profile.consPrefix, profile.refinePrefix), 56, 64);
            if (m.weapon != null && !m.weapon.isBlank()) {
                Tooltip.install(av, new Tooltip(m.name + "\n" + m.weapon + " (" + profile.refinePrefix + m.refinement + ")"));
            }
            members.getChildren().add(av);
        }
        VBox card = new VBox(12, head, members);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        return card;
    }

    private Node buildCharactersPane() {
        TableView<GenshinCharacter> table = new TableView<>(filteredChars);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(col("Nom", c -> Ui.shortName(c.name), 180));
        table.getColumns().add(col("Élément", c -> c.element == null ? "" : c.element.label(), 110));
        table.getColumns().add(col("Rareté", c -> c.rarity > 0 ? c.rarity + " ★" : "", 70));
        table.getColumns().add(col("Voie", c -> c.path == null ? "" : c.path, 110));
        table.setPlaceholder(Ui.label("Aucun personnage. Éditez honkaiStarRailCharacters.json.", "muted"));
        VBox box = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private static TableColumn<GenshinCharacter, String> col(String title,
                                                              java.util.function.Function<GenshinCharacter, String> f, double width) {
        TableColumn<GenshinCharacter, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    //   right: endgames, in the order MOC, PF, APOC, Arbitration (the last one highlighted)

    private Node buildRight() {
        Label mocBig = Ui.label("", "big-number", "neon-green");
        Label mocSub = Ui.label("", "muted", "small");
        Label pfBig = Ui.label("", "big-number", "neon-green");
        Label pfSub = Ui.label("", "muted", "small");
        Label apocBig = Ui.label("", "big-number", "neon-green");
        Label apocSub = Ui.label("", "muted", "small");

        Runnable refresh = () -> {
            if (store.moc.isEmpty()) {
                mocBig.setText("—");
                mocSub.setText("aucun clear");
            } else {
                var c = store.moc.get(0);
                mocBig.setText(c.totalStars() + "/36");
                mocSub.setText("étoiles · patch " + c.patch + (c.stellar ? " · ★ stellaire" : ""));
            }
            if (store.pf.isEmpty()) {
                pfBig.setText("—");
                pfSub.setText("aucun clear");
            } else {
                var c = store.pf.get(0);
                pfBig.setText(HsrUi.num(c.totalScore()));
                pfSub.setText("points · patch " + c.patch + (c.stellar ? " · ★ stellaire" : ""));
            }
            if (store.apoc.isEmpty()) {
                apocBig.setText("—");
                apocSub.setText("aucun clear");
            } else {
                var c = store.apoc.get(0);
                apocBig.setText(HsrUi.num(c.totalScore()));
                apocSub.setText("patch " + c.patch + (c.stellar() ? " · ★ stellaire" : ""));
            }
        };
        javafx.beans.InvalidationListener l = o -> refresh.run();
        store.moc.addListener(l);
        store.pf.addListener(l);
        store.apoc.addListener(l);
        refresh.run();

        VBox cards = new VBox(20,
                endgameCard("Salle oubliée", store.moc, mocBig, mocSub, openMoc, "glass", false),
                endgameCard("Pure fiction", store.pf, pfBig, pfSub, openPf, "glass", false),
                endgameCard("Fantômes de l'apocalypse", store.apoc, apocBig, apocSub, openApoc, "glass", false),
                arbitrationCard());
        HBox.setHgrow(cards, Priority.ALWAYS);
        cards.setMinWidth(420);
        return cards;
    }

    /** Arbitration card: border colour = frame obtained; frame picture, King stars, then the Horsemen stars as a pyramid. */
    private Node arbitrationCard() {
        Label count = Ui.label("", "muted", "small");
        VBox right = new VBox(8);
        right.setAlignment(Pos.CENTER);
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(32, 28, 32, 28));

        Button btn = Ui.button("Ouvrir", "btn-primary");
        btn.setOnAction(e -> openArbitration.run());
        Label titleLabel = Ui.label("Arbitre de l'instable", "h2");
        titleLabel.setMinWidth(Region.USE_PREF_SIZE);
        VBox left = new VBox(8, titleLabel, count, spacer(14), btn);
        left.setMinWidth(Region.USE_PREF_SIZE);
        row.getChildren().addAll(left, Ui.grow(), right);

        Runnable refresh = () -> {
            count.setText(store.arbitration.size() + " clears enregistrés");
            right.getChildren().clear();
            row.getStyleClass().removeAll("glass", "hsr-featured");
            if (store.arbitration.isEmpty()) {
                row.getStyleClass().add("glass");
                row.setStyle("");
                right.getChildren().addAll(Ui.label("—", "big-number", "neon-magenta"), Ui.label("aucun clear", "muted", "small"));
                return;
            }
            var c = store.arbitration.get(0);
            String frame = c.frame();
            if (frame.isEmpty()) {
                // no frame: same look as the other endgames, only the Horsemen stars on one line
                row.getStyleClass().add("glass");
                row.setStyle("");
                right.getChildren().add(horsemenStars(c, 18));
            } else {
                String color = ArbitrationPage.frameColor(frame);
                row.getStyleClass().add("hsr-featured");
                row.setStyle("-fx-border-color:" + color + ";-fx-effect:dropshadow(gaussian," + Ui.rgba(color, 0.45)
                        + ",28,0.1,0,0);");
                javafx.scene.image.ImageView img = FrameImages.view(frame, 120);
                Node top = img != null ? img : ArbitrationPage.frameText(frame);
                HBox king = HsrUi.stars(c.kingStars(), 3, 20);
                king.setAlignment(Pos.CENTER);
                right.getChildren().addAll(top, king, horsemenStars(c, 14));
            }
            right.getChildren().add(Ui.label("patch " + c.patch + (frame.isEmpty() ? "" : " · " + frame), "muted", "small"));
        };
        javafx.beans.InvalidationListener l = o -> refresh.run();
        store.arbitration.addListener(l);
        refresh.run();
        return row;
    }

    /** The three Horsemen side by side, 3 stars each. */
    private static HBox horsemenStars(com.hoyoendgame.model.ArbitrationClear c, double size) {
        HBox line = new HBox(size);
        line.setAlignment(Pos.CENTER);
        for (int i = 0; i < com.hoyoendgame.model.ArbitrationClear.HORSEMEN; i++) {
            HBox g = HsrUi.stars(c.starsOf(i), 3, size);
            g.setSpacing(2);
            g.setAlignment(Pos.CENTER);
            line.getChildren().add(g);
        }
        return line;
    }

    private Node endgameCard(String title, javafx.collections.ObservableList<?> list, Label big, Label sub,
                             Runnable open, String styleClass, boolean featured) {
        Label count = Ui.label("", "muted", "small");
        Runnable upd = () -> count.setText(list.size() + " clears enregistrés");
        list.addListener((javafx.beans.InvalidationListener) o -> upd.run());
        upd.run();

        Button btn = Ui.button("Ouvrir", "btn-primary");
        btn.setOnAction(e -> open.run());
        Label titleLabel = Ui.label(title, "h2");
        titleLabel.setMinWidth(Region.USE_PREF_SIZE); // never cut with "..."
        VBox left = new VBox(8, titleLabel, count, spacer(featured ? 14 : 6), btn);
        left.setMinWidth(Region.USE_PREF_SIZE);
        VBox right = new VBox(2, big, sub);
        right.setAlignment(Pos.CENTER);
        right.getStyleClass().add("stat-box");
        HBox row = new HBox(16, left, Ui.grow(), right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(styleClass);
        row.setPadding(featured ? new Insets(32, 28, 32, 28) : new Insets(22));
        return row;
    }

    private static Node spacer(double h) {
        VBox s = new VBox();
        s.setMinHeight(h);
        return s;
    }

    //   Excel (characters come from honkaiStarRailCharacters.json, only the teams are imported)

    private void exportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les personnages et les équipes");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("starrail-personnages-equipes.xlsx");
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        if (!f.getName().toLowerCase().endsWith(".xlsx")) f = new File(f.getParentFile(), f.getName() + ".xlsx");
        try {
            ExcelService.export(f, store.characters, store.teams);
            Ui.info("Export terminé :\n" + f.getAbsolutePath());
        } catch (IOException ex) {
            Ui.error("Export impossible : " + ex.getMessage());
        }
    }

    private void importExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer des équipes");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel (*.xlsx)", "*.xlsx"));
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;
        ExcelService.Imported imp;
        try {
            imp = ExcelService.importFrom(f);
        } catch (Exception ex) {
            Ui.error("Import impossible : " + ex.getMessage());
            return;
        }
        if (!Ui.confirm(imp.teams().size() + " équipe(s) trouvée(s).\nElles seront fusionnées avec les équipes actuelles "
                + "(mise à jour par nom). La liste des personnages vient de honkaiStarRailCharacters.json et n'est pas modifiée.")) {
            return;
        }
        for (Team t : imp.teams()) {
            Team existing = store.teams.stream().filter(x -> x.name.equalsIgnoreCase(t.name)).findFirst().orElse(null);
            if (existing == null) {
                store.teams.add(t);
            } else {
                t.id = existing.id;
                store.teams.set(store.teams.indexOf(existing), t);
            }
        }
        store.saveTeams();
    }
}
