package com.hoyoendgame.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** A button showing the current value; clicking it opens a {@link Picker} to change it. */
public final class PickerButton<T> extends Button {
    private final String placeholder;
    private final Function<T, String> label;
    private T value;
    private Consumer<T> onPick = v -> {
    };

    /**
     * @param items supplies the items to choose from each time the dialog opens
     */
    public PickerButton(String dialogTitle, String placeholder, Function<T, String> label, Supplier<List<T>> items) {
        this.placeholder = placeholder;
        this.label = label;
        getStyleClass().add("btn");
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        refreshText();
        setOnAction(e -> {
            if (getScene() == null) return;
            Picker.pick(getScene().getWindow(), dialogTitle, items.get(), label).ifPresent(v -> {
                setValue(v);
                onPick.accept(v);
            });
        });
    }

    public T getValue() {
        return value;
    }

    /** Sets the value without calling the pick listener. */
    public void setValue(T v) {
        this.value = v;
        refreshText();
    }

    /** Called only when the user picks something in the dialog. */
    public void setOnPick(Consumer<T> listener) {
        this.onPick = listener;
    }

    private void refreshText() {
        setText(value == null ? placeholder : label.apply(value));
    }
}
