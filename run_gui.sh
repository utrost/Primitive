#!/bin/bash
# Runs the Primitive GUI

# Ensure the JAR exists, or build it if missing
if [ ! -f target/primitive-1.0-SNAPSHOT.jar ]; then
    echo "JAR not found. Building project..."
    mvn clean package -DskipTests
fi

# Check for X11 Display
if [ -z "$DISPLAY" ]; then
    echo "Error: DISPLAY environment variable is not set."
    echo "Primitive GUI requires an X Server to run."
    exit 1
fi

# Verify X connection works
if ! command -v xdpyinfo &> /dev/null; then
    echo "Warning: xdpyinfo not found. Skipping active X server check."
else
    if ! xdpyinfo >/dev/null 2>&1; then
        echo "Error: DISPLAY is set to '$DISPLAY', but cannot connect to X server."
        echo "This usually happens when SSH X11 forwarding is broken or access is denied."
        echo "Tips:"
        echo "  - Reconnect with 'ssh -X user@host' (or -Y)"
        echo "  - Check if your local X server is running"
        exit 1
    fi
fi

# Detect Java 17 (Full JDK)
JAVA_CMD="java"
if [ -f "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" ]; then
    JAVA_CMD="/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
    echo "Using specific Java binary: $JAVA_CMD"
fi

echo "Launching GUI..."
# Run with Vector API module enabled
"$JAVA_CMD" --add-modules jdk.incubator.vector -cp target/primitive-1.0-SNAPSHOT.jar org.trostheide.primitive.gui.PrimitiveFrame
