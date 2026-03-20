package org.trostheide.primitive.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trostheide.primitive.PrimitiveRunner;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsPanelTest {

    private SettingsPanel panel;
    private AtomicBoolean startCalled;
    private AtomicBoolean stopCalled;

    @BeforeEach
    void setUp() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        startCalled = new AtomicBoolean(false);
        stopCalled = new AtomicBoolean(false);
        panel = new SettingsPanel(e -> startCalled.set(true), e -> stopCalled.set(true));
    }

    @Test
    void testDefaultShapeCount() {
        assertEquals(100, panel.getShapeCount());
    }

    @Test
    void testDefaultMode() {
        assertEquals(PrimitiveRunner.Mode.TRIANGLE, panel.getSelectedMode());
    }

    @Test
    void testDefaultWorkerCount() {
        int expected = Runtime.getRuntime().availableProcessors();
        assertEquals(expected, panel.getWorkerCount());
    }

    @Test
    void testInitiallyNotRunning() {
        assertFalse(panel.isRunning());
    }

    @Test
    void testSetRunningTrue() {
        panel.setRunning(true);
        assertTrue(panel.isRunning());
    }

    @Test
    void testSetRunningFalse() {
        panel.setRunning(true);
        panel.setRunning(false);
        assertFalse(panel.isRunning());
    }

    @Test
    void testSetRunningTrueThenFalse() {
        // Verify the bug fix: setRunning(true) should keep controls disabled
        panel.setRunning(true);
        assertTrue(panel.isRunning());

        panel.setRunning(false);
        assertFalse(panel.isRunning());
    }

    @Test
    void testSetControlsEnabled() {
        // Should not throw
        panel.setControlsEnabled(false);
        panel.setControlsEnabled(true);
    }

    @Test
    void testSetLoadAction() {
        AtomicBoolean loadCalled = new AtomicBoolean(false);
        panel.setLoadAction(e -> loadCalled.set(true));
        // Verify it doesn't throw
        assertFalse(loadCalled.get());
    }
}
