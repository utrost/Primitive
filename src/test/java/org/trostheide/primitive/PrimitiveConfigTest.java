package org.trostheide.primitive;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrimitiveConfigTest {

    @Test
    void testValidConstruction() {
        PrimitiveConfig config = new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, 4);
        assertEquals(100, config.numShapes());
        assertEquals(PrimitiveRunner.Mode.TRIANGLE, config.mode());
        assertEquals(4, config.numWorkers());
    }

    @Test
    void testMinimumValues() {
        PrimitiveConfig config = new PrimitiveConfig(1, PrimitiveRunner.Mode.LINE, 1);
        assertEquals(1, config.numShapes());
        assertEquals(1, config.numWorkers());
    }

    @Test
    void testInvalidShapeCountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveConfig(0, PrimitiveRunner.Mode.TRIANGLE, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveConfig(-1, PrimitiveRunner.Mode.TRIANGLE, 4));
    }

    @Test
    void testInvalidWorkerCountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, -1));
    }

    @Test
    void testDefaults() {
        PrimitiveConfig config = PrimitiveConfig.defaults(50, PrimitiveRunner.Mode.ELLIPSE);
        assertEquals(50, config.numShapes());
        assertEquals(PrimitiveRunner.Mode.ELLIPSE, config.mode());
        assertEquals(Runtime.getRuntime().availableProcessors(), config.numWorkers());
    }

    @Test
    void testAllModes() {
        for (PrimitiveRunner.Mode mode : PrimitiveRunner.Mode.values()) {
            PrimitiveConfig config = new PrimitiveConfig(10, mode, 1);
            assertEquals(mode, config.mode());
        }
    }

    @Test
    void testModeEnumValues() {
        PrimitiveRunner.Mode[] modes = PrimitiveRunner.Mode.values();
        assertEquals(7, modes.length);
        assertNotNull(PrimitiveRunner.Mode.valueOf("TRIANGLE"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("LINE"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("BEZIER"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("RECT"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("POLYLINE"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("ELLIPSE"));
        assertNotNull(PrimitiveRunner.Mode.valueOf("COMBO"));
    }

    @Test
    void testRecordEquality() {
        PrimitiveConfig a = new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, 4);
        PrimitiveConfig b = new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, 4);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testRecordInequality() {
        PrimitiveConfig a = new PrimitiveConfig(100, PrimitiveRunner.Mode.TRIANGLE, 4);
        PrimitiveConfig b = new PrimitiveConfig(200, PrimitiveRunner.Mode.TRIANGLE, 4);
        assertNotEquals(a, b);
    }
}
