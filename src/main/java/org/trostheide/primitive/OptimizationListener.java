package org.trostheide.primitive;

import org.trostheide.primitive.core.ShapeResult;
import java.io.File;

public interface OptimizationListener {
    void onStart(int totalShapes, PrimitiveRunner.Mode mode, int numWorkers);

    void onShapeCommitted(int shapeNumber, int totalShapes, ShapeResult result, long timeElapsedMs,
            org.trostheide.primitive.image.RgbaImage currentImage);

    void onComplete(File pngFile, File svgFile);
}
