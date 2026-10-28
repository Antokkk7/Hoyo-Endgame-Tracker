package com.hoyoendgame.ui;

import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/** "Nouvelle équipe" / "Modifier l'équipe" form. */
public final class TeamEditorDialog {
    public static final List<String> USAGES = List.of(
            "Libre",
            "Abysses · première moitié",
            "Abysses · seconde moitié",
            "Théâtre de l'imaginarium",
            "Carnage chtonien");

    private TeamEditorDialog() {
    }

    /** @param existing the team to edit, or null for a new team. Returns the saved team (a new instance). */
    public static Optional<Team> show(Window owner, Team existing) {
        TextField name = new TextField(existing == null ? "" : existing.name);
        name.setPromptText("Nom de l'équipe");
        ComboBox<String> usage = new ComboBox<>();
        usage.getItems().addAll(USAGES);
        usage.setEditable(false);
        usage.setValue(existing != null && USAGES.contains(existing.usage) ? existing.usage : "Libre");
        usage.setMaxWidth(Double.MAX_VALUE);

        TeamPane pane = new TeamPane(2);
        if (existing != null) pane.setMembers(existing.members);

        HBox.setHgrow(name, Priority.ALWAYS);
        VBox nameBox = new VBox(4, Ui.label("Nom de l'équipe", "muted", "small"), name);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        VBox usageBox = new VBox(4, Ui.label("Usage", "muted", "small"), usage);
        usageBox.setPrefWidth(260);
        HBox fields = new HBox(16, nameBox, usageBox);

        Team[] result = new Team[1];
        Stage[] stage = new Stage[1];
        Button cancel = Ui.button("Annuler");
        Button save = Ui.button("Enregistrer l'équipe", "btn-primary");
        cancel.setOnAction(e -> stage[0].close());
        save.setOnAction(e -> {
            if (name.getText() == null || name.getText().isBlank()) {
                Ui.error("Donnez un nom à l'équipe.");
                return;
            }
            List<TeamMember> members = pane.members();
            if (members.isEmpty()) {
                Ui.error("Choisissez au moins un personnage.");
                return;
            }
            Team t = existing == null ? new Team() : existing.copy();
            t.name = name.getText().trim();
            t.usage = usage.getValue();
            t.members = members;
            result[0] = t;
            stage[0].close();
        });
        HBox actions = new HBox(12, Ui.label("Les personnages du roster pré-remplissent niveau, doublons et arme.", "muted", "small"),
                Ui.grow(), cancel, save);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(18,
                Ui.label(existing == null ? "Nouvelle équipe" : "Modifier l'équipe", "h1"),
                Ui.label("Choisissez 4 personnages avec leur niveau, leurs doublons et l'arme équipée.", "muted"),
                fields, pane);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        VBox root = new VBox(16, scroll, actions);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(28));

        stage[0] = Ui.modal(owner, existing == null ? "Nouvelle équipe" : "Modifier l'équipe", root, 980, 760);
        stage[0].showAndWait();
        return Optional.ofNullable(result[0]);
    }
}
