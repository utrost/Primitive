package org.trostheide.primitive.core;

import org.trostheide.primitive.shape.Shape;

public record ShapeResult(Shape shape, long energyDelta, int color) {
}