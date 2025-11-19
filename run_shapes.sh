#!/bin/bash
# Runs Primitive with specific shape modes
# Usage: ./run_shapes.sh [mode] [num_shapes]
# Modes: triangle, line, bezier, combo

MODE=${1:-combo}
SHAPES=${2:-100}

echo "--------------------------------------------------"
echo "  PRIMITIVE - Shape Test"
echo "  Mode:   $MODE"
echo "  Shapes: $SHAPES"
echo "--------------------------------------------------"

mvn clean compile

mvn exec:java \
    -Dexec.mainClass="org.trostheide.primitive.Main" \
    -Dexec.args="Daniel.png output_${MODE}.png $SHAPES $MODE"

echo "--------------------------------------------------"
echo "  Done! Check output_${MODE}.png and output_${MODE}.svg"
echo "--------------------------------------------------"