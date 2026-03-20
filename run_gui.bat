@echo off
REM Runs the Primitive GUI (Windows)

REM Ensure the JAR exists, or build it if missing
if not exist target\primitive-1.0-SNAPSHOT.jar (
    echo JAR not found. Building project...
    call mvn clean package -DskipTests
)

echo Launching GUI...
REM Run with Vector API module enabled
java --add-modules jdk.incubator.vector -cp target\primitive-1.0-SNAPSHOT.jar org.trostheide.primitive.gui.PrimitiveFrame
