package org.trostheide.primitive.raster;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanlineBufferTest {

    @Test
    void testInitialization() {
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        assertEquals(0, buffer.count);
        assertEquals(10, buffer.y.length);
        assertEquals(10, buffer.x1.length);
        assertEquals(10, buffer.x2.length);
    }

    @Test
    void testAdd() {
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        buffer.add(5, 10, 20);

        assertEquals(1, buffer.count);
        assertEquals(5, buffer.y[0]);
        assertEquals(10, buffer.x1[0]);
        assertEquals(20, buffer.x2[0]);

        buffer.add(6, 15, 25);
        assertEquals(2, buffer.count);
        assertEquals(6, buffer.y[1]);
        assertEquals(15, buffer.x1[1]);
        assertEquals(25, buffer.x2[1]);
    }

    @Test
    void testReset() {
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        buffer.add(5, 10, 20);
        buffer.reset();

        assertEquals(0, buffer.count);
        // Data might still be there, but count is 0, which is what matters for the
        // logic

        buffer.add(7, 30, 40);
        assertEquals(1, buffer.count);
        assertEquals(7, buffer.y[0]);
        assertEquals(30, buffer.x1[0]);
        assertEquals(40, buffer.x2[0]);
    }

    @Test
    void testResize() {
        int initialCapacity = 2;
        ScanlineBuffer buffer = new ScanlineBuffer(initialCapacity);

        buffer.add(1, 10, 20);
        buffer.add(2, 10, 20);
        // Next add should trigger resize
        buffer.add(3, 10, 20);

        assertEquals(3, buffer.count);
        assertTrue(buffer.y.length > initialCapacity);
        assertTrue(buffer.x1.length > initialCapacity);
        assertTrue(buffer.x2.length > initialCapacity);

        // Verify data integrity
        assertEquals(1, buffer.y[0]);
        assertEquals(2, buffer.y[1]);
        assertEquals(3, buffer.y[2]);
    }
}
