#!/bin/bash
# Runs the Primitive application using Maven
# Usage: ./run_primitive.sh [num_shapes]

SHAPES=${1:-10000} # Default to 100 shapes if not provided

echo "--------------------------------------------------"
echo "  PRIMITIVE - Java Port"
echo "  Input:  Daniel.png"
echo "  Output: output.png"
echo "  Shapes: $SHAPES"
echo "--------------------------------------------------"

# Compile first to ensure latest changes (like the TriangleTest fixes) are built
mvn clean compile

# Run the Main class
# We use exec:java to avoid needing to package a JAR manually
mvn exec:java \
    -Dexec.mainClass="org.trostheide.primitive.Main" \
    -Dexec.args="Daniel.png output.png $SHAPES"

echo "--------------------------------------------------"
echo "  Done! Check output.png for the result."
echo "--------------------------------------------------"