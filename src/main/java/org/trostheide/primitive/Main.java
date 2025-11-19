package org.trostheide.primitive;

import org.trostheide.primitive.image.RgbaImage;
import java.io.File;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar primitive.jar <input.png> <output.png> <num_shapes> [mode]");
            System.out.println("Modes: triangle, line, bezier, rect, polyline, combo (default: triangle)");
            System.exit(1);
        }

        try {
            File input = new File(args[0]);
            File output = new File(args[1]);
            int count = Integer.parseInt(args[2]);

            // Parse Mode (default to TRIANGLE if not specified)
            PrimitiveRunner.Mode mode = PrimitiveRunner.Mode.TRIANGLE;

            if (args.length > 3) {
                String modeStr = args[3].toUpperCase(Locale.ROOT);
                try {
                    mode = PrimitiveRunner.Mode.valueOf(modeStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown mode: " + args[3] + ". Using TRIANGLE.");
                    System.err.println("Available modes: TRIANGLE, LINE, BEZIER, RECT, POLYLINE, COMBO");
                    mode = PrimitiveRunner.Mode.TRIANGLE;
                }
            }

            // 1. Load Input
            RgbaImage target = RgbaImage.load(input);

            // 2. Run
            PrimitiveRunner runner = new PrimitiveRunner(target, count, mode);
            runner.run(output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}