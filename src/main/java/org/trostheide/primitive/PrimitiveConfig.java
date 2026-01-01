package org.trostheide.primitive;

public record PrimitiveConfig(
        int numShapes,
        PrimitiveRunner.Mode mode,
        int numWorkers) {
    public PrimitiveConfig {
        if (numShapes < 1)
            throw new IllegalArgumentException("numShapes must be >= 1");
        if (numWorkers < 1)
            throw new IllegalArgumentException("numWorkers must be >= 1");
    }

    public static PrimitiveConfig defaults(int numShapes, PrimitiveRunner.Mode mode) {
        return new PrimitiveConfig(numShapes, mode, Runtime.getRuntime().availableProcessors());
    }
}
