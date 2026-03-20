package org.trostheide.primitive.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @BeforeEach
    void setUp() {
        // Reset to a known state
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
    }

    @Test
    void testInitialThemeIsLight() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        assertEquals(ThemeManager.Theme.LIGHT, ThemeManager.getCurrentTheme());
        assertFalse(ThemeManager.isDark());
    }

    @Test
    void testSetDarkTheme() {
        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        assertEquals(ThemeManager.Theme.DARK, ThemeManager.getCurrentTheme());
        assertTrue(ThemeManager.isDark());
    }

    @Test
    void testToggle() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        ThemeManager.toggle();
        assertTrue(ThemeManager.isDark());

        ThemeManager.toggle();
        assertFalse(ThemeManager.isDark());
    }

    @Test
    void testToggleSymmetry() {
        ThemeManager.Theme initial = ThemeManager.getCurrentTheme();
        ThemeManager.toggle();
        ThemeManager.toggle();
        assertEquals(initial, ThemeManager.getCurrentTheme());
    }

    @Test
    void testChangeListenerFires() {
        AtomicInteger callCount = new AtomicInteger(0);
        ThemeManager.addChangeListener(callCount::incrementAndGet);

        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        assertTrue(callCount.get() >= 1, "Listener should have been called");
    }

    @Test
    void testCanvasBackgroundChangesWithTheme() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        Color lightBg = ThemeManager.getCanvasBackground();

        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        Color darkBg = ThemeManager.getCanvasBackground();

        assertNotEquals(lightBg, darkBg, "Light and dark canvas backgrounds should differ");
        // Dark should be darker
        assertTrue(darkBg.getRed() < lightBg.getRed());
        assertTrue(darkBg.getGreen() < lightBg.getGreen());
        assertTrue(darkBg.getBlue() < lightBg.getBlue());
    }

    @Test
    void testSubtleTextChangesWithTheme() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        Color lightText = ThemeManager.getSubtleText();

        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        Color darkText = ThemeManager.getSubtleText();

        assertNotEquals(lightText, darkText, "Subtle text colors should differ between themes");
    }

    @Test
    void testCardBackgroundChangesWithTheme() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        Color lightCard = ThemeManager.getCardBackground();

        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        Color darkCard = ThemeManager.getCardBackground();

        assertNotEquals(lightCard, darkCard, "Card backgrounds should differ between themes");
    }

    @Test
    void testBorderColorChangesWithTheme() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        Color lightBorder = ThemeManager.getBorderColor();

        ThemeManager.setTheme(ThemeManager.Theme.DARK);
        Color darkBorder = ThemeManager.getBorderColor();

        assertNotEquals(lightBorder, darkBorder, "Border colors should differ between themes");
    }

    @Test
    void testStaticColorsAreConsistent() {
        // These colors should not change with theme
        Color accent1 = ThemeManager.getAccentColor();
        ThemeManager.toggle();
        Color accent2 = ThemeManager.getAccentColor();
        assertEquals(accent1, accent2, "Accent color should be theme-independent");

        Color success1 = ThemeManager.getSuccessColor();
        ThemeManager.toggle();
        Color success2 = ThemeManager.getSuccessColor();
        assertEquals(success1, success2, "Success color should be theme-independent");

        Color error1 = ThemeManager.getErrorColor();
        ThemeManager.toggle();
        Color error2 = ThemeManager.getErrorColor();
        assertEquals(error1, error2, "Error color should be theme-independent");
    }

    @Test
    void testAccentHoverIsDifferentFromAccent() {
        assertNotEquals(ThemeManager.getAccentColor(), ThemeManager.getAccentHover());
    }

    @Test
    void testColorsAreOpaque() {
        assertEquals(255, ThemeManager.getAccentColor().getAlpha());
        assertEquals(255, ThemeManager.getSuccessColor().getAlpha());
        assertEquals(255, ThemeManager.getErrorColor().getAlpha());
        assertEquals(255, ThemeManager.getCanvasBackground().getAlpha());
        assertEquals(255, ThemeManager.getCardBackground().getAlpha());
    }

    @Test
    void testThemeEnumValues() {
        ThemeManager.Theme[] themes = ThemeManager.Theme.values();
        assertEquals(2, themes.length);
        assertNotNull(ThemeManager.Theme.valueOf("LIGHT"));
        assertNotNull(ThemeManager.Theme.valueOf("DARK"));
    }
}
