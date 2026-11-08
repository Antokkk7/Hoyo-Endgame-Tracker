package com.hoyoendgame.ui;

import com.hoyoendgame.model.TeamMember;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Small building blocks shared by the Honkai: Star Rail pages. */
final class HsrUi {
    private HsrUi() {
    }

    /** 117520 becomes "117 520". */
    static String num(int n) {
        return String.valueOf(n).replaceAll("(?<=\\d)(?=(\\d{3})+$)", " ");
    }

    /** A row of gold stars, the first {@code on} of {@code max} lit. */
    static HBox stars(int on, int max, double size) {
        HBox row = new HBox(4);
        for (int k = 0; k < max; k++) row.getChildren().add(Ui.star(size, k < on, Ui.STAR_GOLD));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    /** The purple "prismatic" star (stellar mode). */
    static Node prismStar(boolean on, double size) {
        return Ui.star(size, on, Ui.STAR_PRISM);
    }

    /** A team shown as portraits with level, eidolon and cone superposition. */
    static VBox teamBox(String title, List<TeamMember> team) {
        GameProfile p = GameProfile.starRail();
        HBox avatars = new HBox(10);
        for (TeamMember m : team) {
            avatars.getChildren().add(Ui.avatar(p.characterImages, m.name, m.element, "Niv." + m.level,
                    m.meta(p.consPrefix, p.refinePrefix), 56, 64));
        }
        if (team.isEmpty()) avatars.getChildren().add(Ui.label("Équipe non renseignée", "muted", "small"));
        // No frame around the team; the small title is shown only when it brings something (several teams).
        VBox box = new VBox(8);
        if (title != null && !title.isBlank()) box.getChildren().add(Ui.label(title, "muted", "small", "strong"));
        box.getChildren().add(avatars);
        box.setPadding(new Insets(4, 0, 4, 0));
        return box;
    }

    /** "Équipe 1", "Équipe 2"... boxes side by side (wrapping). */
    static Node teamsRow(String prefix, List<List<TeamMember>> teams) {
        if (teams.size() == 1) {
            return teamBox(null, teams.get(0));
        }
        VBox row = new VBox(12);
        for (int i = 0; i < teams.size(); i++) row.getChildren().add(teamBox(prefix + " " + (i + 1), teams.get(i)));
        if (teams.isEmpty()) row.getChildren().add(Ui.label("Équipes non renseignées", "muted", "small"));
        return row;
    }

    /** Label on the left, value on the right. */
    static HBox line(String name, Node value) {
        HBox h = new HBox(12, Ui.label(name, "muted"), Ui.grow(), value);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    static Label big(String text, String... extra) {
        Label l = Ui.label(text, "big-number");
        l.getStyleClass().addAll(extra);
        return l;
    }
}
