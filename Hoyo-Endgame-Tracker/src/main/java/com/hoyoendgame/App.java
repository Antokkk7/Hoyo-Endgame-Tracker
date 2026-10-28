package com.hoyoendgame;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.ui.GenshinView;
import com.hoyoendgame.ui.HomeView;
import com.hoyoendgame.ui.Ui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/** Entry point: top navigation bar (Accueil / Genshin Impact / Honkai: Star Rail soon) and page switching. */
public class App extends Application {
    private final BorderPane root = new BorderPane();
    private Button homeBtn;
    private Button genshinBtn;
    private HomeView home;
    private GenshinView genshin;

    @Override
    public void start(Stage stage) {
        DataStore store = DataStore.get();
        store.setErrorHandler(msg -> Platform.runLater(() -> Ui.error(msg)));
        store.load();

        Region logo = new Region();
        logo.setMinSize(36, 36);
        logo.setPrefSize(36, 36);
        logo.setStyle("-fx-background-radius:10;-fx-background-color:linear-gradient(to bottom right,#9B8CFF,#F6D77B);");
        Label name = Ui.label("EndTracker", "h2");
        HBox brand = new HBox(12, logo, name);
        brand.setAlignment(Pos.CENTER_LEFT);

        homeBtn = navButton("Accueil");
        genshinBtn = navButton("Genshin Impact");
        Button hsr = navButton("Honkai: Star Rail");
        hsr.setDisable(true);
        homeBtn.setOnAction(e -> showHome());
        genshinBtn.setOnAction(e -> showGenshin());

        HBox nav = new HBox(8, homeBtn, genshinBtn, hsr);
        nav.setAlignment(Pos.CENTER);
        HBox bar = new HBox(16, brand, Ui.grow(), nav, Ui.grow(), spacerLike(brand));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(22, 48, 18, 48));
        root.setTop(bar);

        home = new HomeView(this::showGenshin);
        genshin = new GenshinView();
        showHome();

        stage.setTitle("Hoyo Endgame Tracker");
        stage.setScene(Ui.scene(root, 1320, 880));
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    @Override
    public void stop() {
        // Everything is already saved after each change; this is a last safety net.
        DataStore.get().saveAll();
    }

    private void showHome() {
        setPage(home);
        setActive(homeBtn);
    }

    private void showGenshin() {
        setPage(genshin);
        setActive(genshinBtn);
    }

    private void setPage(Node page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(sp);
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{homeBtn, genshinBtn}) b.getStyleClass().remove("nav-btn-active");
        active.getStyleClass().add("nav-btn-active");
    }

    private static Button navButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        return b;
    }

    /** Invisible block with the same width as the brand, to keep the nav centred. */
    private static Region spacerLike(HBox brand) {
        Region r = new Region();
        r.setPrefWidth(160);
        r.setMinWidth(160);
        return r;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
