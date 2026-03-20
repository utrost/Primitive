package org.trostheide.primitive.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static final String PREF_KEY = "primitive.theme";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    private static Theme currentTheme = Theme.LIGHT;
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void initialize() {
        String saved = PREFS.get(PREF_KEY, "LIGHT");
        currentTheme = saved.equals("DARK") ? Theme.DARK : Theme.LIGHT;
        applyTheme();
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    public static void toggle() {
        setTheme(currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        PREFS.put(PREF_KEY, theme.name());
        applyTheme();
        listeners.forEach(Runnable::run);
    }

    public static void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private static void applyTheme() {
        try {
            if (currentTheme == Theme.DARK) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            // Update all existing windows
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Theme-aware colors
    public static Color getCanvasBackground() {
        return isDark() ? new Color(30, 30, 30) : new Color(224, 224, 224);
    }

    public static Color getAccentColor() {
        return new Color(98, 0, 238); // Material Purple 500
    }

    public static Color getAccentHover() {
        return new Color(123, 31, 255); // Material Purple 400
    }

    public static Color getSuccessColor() {
        return new Color(76, 175, 80); // Material Green 500
    }

    public static Color getErrorColor() {
        return new Color(244, 67, 54); // Material Red 500
    }

    public static Color getSubtleText() {
        return isDark() ? new Color(158, 158, 158) : new Color(117, 117, 117);
    }

    public static Color getCardBackground() {
        return isDark() ? new Color(45, 45, 45) : Color.WHITE;
    }

    public static Color getBorderColor() {
        return isDark() ? new Color(60, 60, 60) : new Color(224, 224, 224);
    }
}
