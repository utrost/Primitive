#!/bin/bash
# Runs the Primitive GUI (Linux / macOS)

# Ensure the JAR exists, or build it if missing
if [ ! -f target/primitive-1.0-SNAPSHOT.jar ]; then
    echo "JAR not found. Building project..."
    mvn clean package -DskipTests
fi

OS="$(uname -s)"

# On Linux, verify X11 display is available
if [ "$OS" = "Linux" ]; then
    if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
        echo "Error: No display server detected (DISPLAY / WAYLAND_DISPLAY not set)."
        echo "Primitive GUI requires a graphical environment to run."
        exit 1
    fi

    # Verify X connection works (only if using X11)
    if [ -n "$DISPLAY" ]; then
        if command -v xdpyinfo &> /dev/null; then
            if ! xdpyinfo >/dev/null 2>&1; then
                echo "Error: DISPLAY is set to '$DISPLAY', but cannot connect to X server."
                echo "Tips:"
                echo "  - Reconnect with 'ssh -X user@host' (or -Y)"
                echo "  - Check if your local X server is running"
                exit 1
            fi
        fi
    fi
fi

# Detect Java
JAVA_CMD="java"
if [ "$OS" = "Linux" ] && [ -f "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" ]; then
    JAVA_CMD="/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
    echo "Using specific Java binary: $JAVA_CMD"
fi

echo "Launching GUI..."
# Run with Vector API module enabled
"$JAVA_CMD" --add-modules jdk.incubator.vector -cp "target/primitive-1.0-SNAPSHOT.jar:target/lib/*" org.trostheide.primitive.gui.PrimitiveFrame
