package com.hoyoendgame.ui;

import com.hoyoendgame.model.Element;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

/** Small helpers shared by every view (styled controls, avatars, stars, dialogs). */
public final class Ui {
    private static final String CSS = Objects.requireNonNull(Ui.class.getResource("/style.css")).toExternalForm();
    private static final double[] STAR = {50, 0, 62, 38, 100, 50, 62, 62, 50, 100, 38, 62, 0, 50, 38, 38};

    /** Difficulty colours for Stygian Onslaught, index 0 = difficulty 1. */
    public static final String[] DIFF_COLORS = {"#7BD88F", "#B6DC6B", "#E6D45F", "#F2AE55", "#EE7F5E", "#E4577A"};

    /** Colour of an empty (grey) star. */
    public static final String STAR_OFF = "#2A2D3A";
    /** Default colour of a filled star (neon cyan). */
    public static final String STAR_ON = "#00F0FF";

    private static final java.util.Map<String, String> SHORT_NAMES = java.util.Map.of(
            "yumeglorpglorp", "Mizuki",
            "kukishinobu", "Kuki",
            "wriothesley", "Wrio");

    private Ui() {
    }

    // ---------------------------------------------------------------- windows

    public static Scene scene(Parent root, double w, double h) {
        if (!root.getStyleClass().contains("app-bg")) root.getStyleClass().add("app-bg");
        Scene s = new Scene(root, w, h);
        s.getStylesheets().add(CSS);
        return s;
    }

    /** Modal window (forms). */
    public static Stage modal(Window owner, String title, Parent content, double w, double h) {
        Stage s = new Stage();
        s.initOwner(owner);
        s.initModality(Modality.WINDOW_MODAL);
        s.setTitle(title);
        s.setScene(scene(content, w, h));
        return s;
    }

    /** Independent window (endgame pages). */
    public static Stage window(Window owner, String title, Parent content, double w, double h) {
        Stage s = new Stage();
        s.initOwner(owner);
        s.setTitle(title);
        s.setScene(scene(content, w, h));
        return s;
    }

    public static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(CSS);
        a.showAndWait();
    }

    public static void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(CSS);
        a.showAndWait();
    }

    public static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(CSS);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    // ---------------------------------------------------------------- controls

    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    public static Button button(String text, String... styleClasses) {
        Button b = new Button(upper(text));
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(styleClasses);
        return b;
    }

    /** JavaFX CSS has no text-transform, so buttons / chips are upper-cased here. */
    public static String upper(String text) {
        return text == null ? "" : text.toUpperCase(java.util.Locale.FRENCH);
    }

    public static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static ToggleButton pill(String text, ToggleGroup group, Object data) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("pill");
        b.setToggleGroup(group);
        b.setUserData(data);
        return b;
    }

    /** Prevents a toggle group from ending up with nothing selected. */
    public static void keepOneSelected(ToggleGroup g) {
        g.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });
    }

    /** Selects the toggle whose userData equals {@code value}. */
    public static void select(ToggleGroup g, Object value) {
        g.getToggles().stream().filter(t -> Objects.equals(t.getUserData(), value)).findFirst()
                .ifPresent(t -> t.setSelected(true));
    }

    public static int selectedInt(ToggleGroup g, int def) {
        return g.getSelectedToggle() == null ? def : (Integer) g.getSelectedToggle().getUserData();
    }

    /** Integer spinner that commits typed text when it loses focus. */
    public static Spinner<Integer> spinner(int min, int max, int value) {
        Spinner<Integer> s = new Spinner<>();
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, Math.max(min, Math.min(max, value))));
        s.setEditable(true);
        s.setPrefWidth(96);
        s.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) {
                try {
                    s.increment(0);
                } catch (RuntimeException ignored) {
                    // invalid text: the factory keeps the previous value
                }
            }
        });
        return s;
    }

    // ---------------------------------------------------------------- colours and shapes

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    public static String rgba(String hex, double alpha) {
        Color c = Color.web(hex);
        return String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,%.2f)", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255), alpha);
    }

    public static String elementColor(Element e) {
        return e == null ? "#5F656E" : e.color();
    }

    /** Four-pointed star used for Abyss stars, Theater "astres"... */
    public static Polygon star(double size, boolean on, String onColor) {
        Polygon p = new Polygon();
        double k = size / 100.0;
        for (double v : STAR) p.getPoints().add(v * k);
        p.setFill(Color.web(on ? (onColor == null ? STAR_ON : onColor) : STAR_OFF));
        return p;
    }

    public static void tileStyle(Region tile, Element e) {
        Color base = Color.web(elementColor(e));
        String light = hex(base.interpolate(Color.WHITE, 0.35));
        tile.setStyle("-fx-background-radius:4;-fx-background-color:linear-gradient(to bottom," + light + "," + hex(base)
                + ");-fx-border-color:" + rgba(elementColor(e), 0.9) + ";-fx-border-width:1;-fx-border-radius:4;");
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String n = name.trim();
        return n.substring(0, Math.min(2, n.length())).toUpperCase();
    }

    /**
     * Character portrait: the image found in data/genshin/personnagesIMG (cover-cropped, rounded) or,
     * when there is none, an element-coloured tile with the initials.
     */
    public static StackPane portrait(String name, Element element, double w, double h) {
        StackPane tile = new StackPane();
        tile.setPrefSize(w, h);
        tile.setMinSize(w, h);
        tile.setMaxSize(w, h);
        tileStyle(tile, element);
        Image img = CharacterImages.get(name);
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            tile.getChildren().add(cover(img, w, h, 8));
        } else {
            tile.getChildren().add(label(initials(name), "tile-text"));
        }
        return tile;
    }

    /** Image scaled to fill w x h (centre-cropped) with rounded corners. */
    public static ImageView cover(Image img, double w, double h, double arc) {
        double iw = img.getWidth(), ih = img.getHeight();
        double scale = Math.max(w / iw, h / ih);
        double vw = w / scale, vh = h / scale;
        ImageView iv = new ImageView(img);
        iv.setViewport(new Rectangle2D((iw - vw) / 2, (ih - vh) / 2, vw, vh));
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setSmooth(true);
        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        iv.setClip(clip);
        return iv;
    }

    private static ImageFolder weaponFolder;

    /** Square with the weapon picture (armesIMG/&lt;weapon name&gt;.png), or an empty frame when there is none. */
    public static StackPane weaponTile(String weapon, double size) {
        StackPane box = new StackPane();
        box.getStyleClass().add("weapon-tile");
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        Image img;
        synchronized (Ui.class) {
            if (weaponFolder == null) weaponFolder = new ImageFolder(com.hoyoendgame.data.DataStore.get().weaponImagesDir());
            img = weaponFolder.get(weapon);
        }
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            box.getChildren().add(cover(img, size, size, 6));
        } else {
            box.getChildren().add(label("?", "muted", "small"));
        }
        return box;
    }

    private static final java.util.Map<Integer, Color> DIFF_COLOR_CACHE = new java.util.HashMap<>();

    /** Main colour of the difficulty picture (average of its saturated pixels), or null without picture. */
    public static synchronized Color diffColor(int difficulty) {
        int d = Math.max(1, Math.min(6, difficulty));
        if (DIFF_COLOR_CACHE.containsKey(d)) return DIFF_COLOR_CACHE.get(d);
        Color result = null;
        Image img = diffImage(d);
        if (img != null && img.getPixelReader() != null) {
            javafx.scene.image.PixelReader pr = img.getPixelReader();
            double r = 0, g = 0, b = 0, n = 0;
            for (int y = 0; y < (int) img.getHeight(); y++) {
                for (int x = 0; x < (int) img.getWidth(); x++) {
                    Color c = pr.getColor(x, y);
                    if (c.getOpacity() > 0.6 && c.getSaturation() > 0.35 && c.getBrightness() > 0.45) {
                        r += c.getRed();
                        g += c.getGreen();
                        b += c.getBlue();
                        n++;
                    }
                }
            }
            if (n > 0) result = new Color(r / n, g / n, b / n, 1.0);
        }
        DIFF_COLOR_CACHE.put(d, result);
        return result;
    }

    /** Boss picture of a patch (bossIMG/&lt;patch&gt;/&lt;boss&gt;.png), or a "?" frame when there is none. */
    public static StackPane bossPortrait(String patch, String boss, double w, double h) {
        StackPane box = new StackPane();
        box.getStyleClass().add("boss-portrait");
        box.setPrefSize(w, h);
        box.setMinSize(w, h);
        box.setMaxSize(w, h);
        Image img = BossImages.get(boss);
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            box.getChildren().add(cover(img, w, h, 8));
        } else {
            box.getChildren().add(label("?", "muted", "h3"));
        }
        return box;
    }

    /**
     * Name shown on screen: "Traveler (Anemo)" becomes "Traveler" (the picture tells them apart) and a few
     * long names are shortened. The stored name is never changed.
     */
    public static String shortName(String name) {
        if (name == null) return "";
        String n = name.trim();
        String alias = SHORT_NAMES.get(CharacterImages.key(n));
        if (alias != null) return alias;
        if (n.toLowerCase(java.util.Locale.ROOT).startsWith("traveler") && n.contains("(")) return "Traveler";
        return n;
    }

    /**
     * Portrait with optional badge and meta line under it.
     *
     * @param sub  optional badge under the tile (e.g. "Niv.90")
     * @param meta optional muted line under it (e.g. "C2 · R1")
     */
    public static VBox avatar(String name, Element element, String sub, String meta, double w, double h) {
        VBox box = new VBox(4, portrait(name, element, w, h));
        box.setAlignment(Pos.TOP_CENTER);
        box.setMinWidth(w + 8);
        if (sub != null) box.getChildren().add(label(sub, "lv"));
        if (meta != null) box.getChildren().add(label(meta, "muted", "small"));
        return box;
    }

    public static Label chip(String text, String hexColor) {
        Label l = label(upper(text), "chip");
        if (hexColor != null) {
            l.setStyle("-fx-background-color:" + rgba(hexColor, 0.25) + ";-fx-text-fill:"
                    + hex(Color.web(hexColor).interpolate(Color.WHITE, 0.45)) + ";");
        }
        return l;
    }

    private static ImageFolder assets;

    /** Picture of a difficulty (assetsOthers/diff1.png ... diff6.png), or null when it is missing. */
    public static synchronized Image diffImage(int difficulty) {
        if (assets == null) assets = new ImageFolder(com.hoyoendgame.data.DataStore.get().assetsDir());
        Image img = assets.get("diff" + Math.max(1, Math.min(6, difficulty)));
        return img != null && img.getWidth() > 0 && img.getHeight() > 0 ? img : null;
    }

    /**
     * Shows the difficulty picture (size x size) before the text in a label; without the picture the label
     * reads "Diff N" instead.
     */
    public static void setDifficulty(Label l, int difficulty, String suffix, double size) {
        int d = Math.max(1, Math.min(6, difficulty));
        Image img = diffImage(d);
        String tail = suffix == null || suffix.isEmpty() ? "" : suffix;
        if (img != null) {
            l.setGraphic(cover(img, size, size, size * 0.12));
            l.setGraphicTextGap(8);
            l.setText(tail);
        } else {
            l.setGraphic(null);
            l.setText("Diff " + d + (tail.isEmpty() ? "" : " · " + tail));
        }
    }

    /** Difficulty badge: the diffN.png picture, or the coloured square with the number if it is missing. */
    public static Node diffBadge(int difficulty, double size) {
        int d = Math.max(1, Math.min(6, difficulty));
        Image img = diffImage(d);
        if (img != null) return cover(img, size, size, size * 0.12);
        Label l = new Label(String.valueOf(d));
        l.getStyleClass().add("diff-badge");
        l.setMinSize(size, size);
        l.setPrefSize(size, size);
        l.setStyle("-fx-background-color:" + DIFF_COLORS[d - 1] + ";-fx-font-size:" + (int) (size * 0.45) + "px;");
        return l;
    }

    /** "12/36"-style value, "—" safe. */
    public static String seconds(int s) {
        return s + " s";
    }
}
