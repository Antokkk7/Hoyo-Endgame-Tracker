package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.data.ExcelService;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TeamMember;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/** Genshin Impact page: Excel import/export, teams and roster on the left, endgame entries on the right. */
public final class GenshinView extends VBox {
    private final DataStore store = DataStore.get();
    private final Runnable openAbyss;
    private final Runnable openTheatre;
    private final Runnable openCarnage;

    private final ToggleButton teamsTab = new ToggleButton();
    private final ToggleButton charsTab = new ToggleButton();
    private final VBox teamsBox = new VBox(12);
    private final TextField search = new TextField();
    private final FilteredList<GenshinCharacter> filteredChars = new FilteredList<>(store.characters, c -> true);
    private final Button newTeam = Ui.button("+ Nouvelle équipe", "btn-primary");

    /** Each endgame opens inside the main window: the three runnables switch the current page. */
    public GenshinView(Runnable openAbyss, Runnable openTheatre, Runnable openCarnage) {
        this.openAbyss = openAbyss;
        this.openTheatre = openTheatre;
        this.openCarnage = openCarnage;
        setSpacing(24);
        setPadding(new Insets(8, 48, 40, 48));

        Button imp = Ui.button("Importer depuis Excel");
        Button exp = Ui.button("Exporter vers Excel");
        imp.setOnAction(e -> importExcel());
        exp.setOnAction(e -> exportExcel());

        Label chip = Ui.label("Genshin Impact", "chip", "chip-gold");
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

    // ------------------------------------------------------------------ left: teams / roster

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
            Optional<Team> t = TeamEditorDialog.show(getScene().getWindow(), null);
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
        teamsTab.setText("Équipes enregistrées (" + store.teams.size() + ")");
        charsTab.setText("Personnages disponibles (" + store.characters.size() + ")");
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
                    ? "Aucune équipe enregistrée. Cliquez sur « Nouvelle équipe »."
                    : "Aucune équipe ne correspond à la recherche.", "muted"));
        }
    }

    private Node teamCard(Team t) {
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.setOnAction(e -> TeamEditorDialog.show(getScene().getWindow(), t).ifPresent(nt -> {
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
            VBox av = Ui.avatar(m.name, m.element, "Niv." + m.level, m.meta(), 56, 64);
            if (m.weapon != null && !m.weapon.isBlank()) {
                Tooltip.install(av, new Tooltip(m.name + "\n" + m.weapon + " (R" + m.refinement + ")"));
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
        table.getColumns().add(col("Nom", c -> c.name, 220));
        table.getColumns().add(col("Élément", c -> c.element == null ? "" : c.element.label(), 120));
        table.setPlaceholder(Ui.label("Aucun personnage. Ajoutez-en ou importez un fichier Excel.", "muted"));

        Button add = Ui.button("Ajouter", "btn-small", "btn-primary");
        Button edit = Ui.button("Modifier", "btn-small");
        Button del = Ui.button("Supprimer", "btn-small", "btn-danger");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        del.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(e -> CharacterDialog.show(getScene().getWindow(), null).ifPresent(c -> {
            store.characters.add(c);
            store.saveCharacters();
        }));
        Runnable editSelected = () -> {
            GenshinCharacter sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            CharacterDialog.show(getScene().getWindow(), sel).ifPresent(c -> {
                int i = store.characters.indexOf(sel);
                if (i >= 0) store.characters.set(i, c);
                store.saveCharacters();
            });
        };
        edit.setOnAction(e -> editSelected.run());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) editSelected.run();
        });
        del.setOnAction(e -> {
            GenshinCharacter sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && Ui.confirm("Supprimer « " + sel.name + " » du roster ? (les équipes déjà enregistrées gardent ses infos)")) {
                store.characters.remove(sel);
                store.saveCharacters();
            }
        });
        HBox actions = new HBox(8, add, edit, del);
        VBox box = new VBox(10, table, actions);
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

    // ------------------------------------------------------------------ right: endgames

    private Node buildRight() {
        Label abyssBig = Ui.label("", "big-number", "gold");
        Label abyssSub = Ui.label("", "muted", "small");
        Label theatreBig = Ui.label("", "big-number", "violet");
        Label theatreSub = Ui.label("", "muted", "small");
        Label carnageBig = Ui.label("", "big-number");
        Label carnageSub = Ui.label("", "muted", "small");

        Runnable refresh = () -> {
            if (store.abyss.isEmpty()) {
                abyssBig.setText("—");
                abyssSub.setText("aucun clear");
            } else {
                abyssBig.setText(store.abyss.get(0).total() + "/" + store.abyss.get(0).maxTotal());
                abyssSub.setText("étoiles · patch " + store.abyss.get(0).patch);
            }
            if (store.theatre.isEmpty()) {
                theatreBig.setText("—");
                theatreSub.setText("aucun clear");
            } else {
                var t = store.theatre.get(0);
                theatreBig.setText(t.stars + "/" + t.mode.acts);
                theatreSub.setText("astres" + (t.mode.hasArcanum() ? " · Arcanum " + t.arcanum + "/" + t.mode.arcanum : ""));
            }
            if (store.carnage.isEmpty()) {
                carnageBig.setText("—");
                carnageSub.setText("aucun clear");
            } else {
                var c = store.carnage.get(0);
                carnageBig.setText("Diff " + c.difficulty);
                carnageSub.setText(c.totalSeconds() + " s");
            }
        };
        javafx.beans.InvalidationListener l = o -> refresh.run();
        store.abyss.addListener(l);
        store.theatre.addListener(l);
        store.carnage.addListener(l);
        refresh.run();

        VBox cards = new VBox(20,
                endgameCard("Abysses spiralées", store.abyss, "clears enregistrés", abyssBig, abyssSub, openAbyss, "glass-gold"),
                endgameCard("Théâtre de l'imaginarium", store.theatre, "clears enregistrés", theatreBig, theatreSub, openTheatre, "glass"),
                endgameCard("Carnage chtonien", store.carnage, "clears enregistrés", carnageBig, carnageSub, openCarnage, "glass"));
        HBox.setHgrow(cards, Priority.ALWAYS);
        cards.setMinWidth(420);
        return cards;
    }

    private Node endgameCard(String title, javafx.collections.ObservableList<?> list, String what, Label big, Label sub,
                             Runnable open, String styleClass) {
        Label count = Ui.label("", "muted", "small");
        Runnable upd = () -> count.setText(list.size() + " " + what);
        list.addListener((javafx.beans.InvalidationListener) o -> upd.run());
        upd.run();

        Button btn = Ui.button("Ouvrir", "btn-primary");
        btn.setOnAction(e -> open.run());
        VBox left = new VBox(8, Ui.label(title, "h2"), count, spacer(6), btn);
        VBox right = new VBox(2, big, sub);
        right.setAlignment(Pos.CENTER);
        right.setStyle("-fx-background-color:rgba(255,255,255,0.06);-fx-background-radius:16;-fx-padding:14 24 14 24;");
        HBox row = new HBox(16, left, Ui.grow(), right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(styleClass);
        row.setPadding(new Insets(22));
        return row;
    }

    private static Node spacer(double h) {
        VBox s = new VBox();
        s.setMinHeight(h);
        return s;
    }

    // ------------------------------------------------------------------ Excel

    private void exportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les personnages et les équipes");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("genshin-personnages-equipes.xlsx");
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
        Window w = getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer des personnages et des équipes");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel (*.xlsx)", "*.xlsx"));
        File f = fc.showOpenDialog(w);
        if (f == null) return;
        ExcelService.Imported imp;
        try {
            imp = ExcelService.importFrom(f);
        } catch (Exception ex) {
            Ui.error("Import impossible : " + ex.getMessage());
            return;
        }
        ButtonType merge = new ButtonType("Fusionner", javafx.scene.control.ButtonBar.ButtonData.YES);
        ButtonType replace = new ButtonType("Remplacer", javafx.scene.control.ButtonBar.ButtonData.NO);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                imp.characters().size() + " personnage(s) et " + imp.teams().size() + " équipe(s) trouvés.\n\n"
                        + "Fusionner : met à jour les éléments de même nom et ajoute les autres.\n"
                        + "Remplacer : efface les personnages et équipes actuels.",
                merge, replace, ButtonType.CANCEL);
        a.setHeaderText("Importer « " + f.getName() + " »");
        Optional<ButtonType> choice = a.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) return;

        if (choice.get() == replace) {
            store.characters.setAll(imp.characters());
            store.teams.setAll(imp.teams());
        } else {
            for (GenshinCharacter c : imp.characters()) {
                GenshinCharacter existing = store.characters.stream()
                        .filter(x -> x.name.equalsIgnoreCase(c.name)).findFirst().orElse(null);
                if (existing == null) {
                    store.characters.add(c);
                } else {
                    c.id = existing.id;
                    store.characters.set(store.characters.indexOf(existing), c);
                }
            }
            for (Team t : imp.teams()) {
                Team existing = store.teams.stream()
                        .filter(x -> x.name.equalsIgnoreCase(t.name)).findFirst().orElse(null);
                if (existing == null) {
                    store.teams.add(t);
                } else {
                    t.id = existing.id;
                    store.teams.set(store.teams.indexOf(existing), t);
                }
            }
        }
        store.saveCharacters();
        store.saveTeams();
    }
}