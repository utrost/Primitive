# **Primitive (Java Port)**

A high-performance Java port of Michael Fogleman's [primitive](https://github.com/fogleman/primitive). This project reproduces images using geometric primitives (triangles, lines, curves) by hill-climbing optimization.

It transforms raster images (PNG/JPG) into abstract vector art (SVG) and simplified raster approximations.

## **How It Works**

Imagine a "blind" painter trying to recreate the Mona Lisa by layering thousands of random, semi-transparent triangles onto a canvas. For every single shape added, the computer calculates the mathematical difference between the current canvas and the original photo using a metric called Root Mean Square Error. This metric squares the difference between the color values of corresponding pixels to penalize large errors heavily, condensing the total visual mismatch into a single number. If a random shape makes the canvas look even slightly more like the target by lowering this error score, it is kept; otherwise, it is immediately discarded. This process repeats millions of times and slowly evolves a chaotic mess into a recognizable reproduction through sheer brute force. In computer science, this is called Hill Climbing because the algorithm treats similarity as altitude, blindly taking steps uphill toward a better match and never accepting a step down. It is a greedy optimization method that relies on high-speed trial and error rather than artistic intent to find the perfect image.
## **Key Features**

* **High Performance**: Designed with a "Zero-Allocation" hot path. Uses a **Structure of Arrays (SoA)** pattern (ScanlineBuffer) to avoid Garbage Collector thrashing during the millions of mutations required for image generation.
* **Multi-Core Parallelism**: Uses an "embarrassingly parallel" architecture where independent workers race to find the best shape for each step.
* **Multiple Primitives**: Supports **Triangles**, **Thick Lines**, **Quadratic Bezier Curves**, **Rotated Rectangles**, **Polylines**, **Ellipses**, and a **Combo** mode.
* **Dual Output**: Generates both a raster preview (.png) and an infinite-resolution vector file (.svg) automatically.
* **Graphical User Interface**: A Swing-based interactive GUI for real-time visualization, configuration, and control.
* **Analytical Color Solving**: Mathematically calculates the optimal color for every shape to minimize Root Mean Square Error (RMSE), rather than guessing colors randomly.

## **Installation & Requirements**

### **Prerequisites**

* **Java 17** or higher (Required for record types and modern switch syntax).
* **Maven** 3.6+

### **Build**

Clone the repository and build with Maven:

mvn clean package

## **Usage**

### **Basic Command Line**

You can run the resulting JAR file directly:

### **Running (with SIMD Optimization)**

To enable the hardware-accelerated Vector API, you **must** add the incubator module flag:

```bash
java --add-modules jdk.incubator.vector -jar target/primitive-1.0-SNAPSHOT.jar [options] <input_file> <output_file> <num_shapes> [mode]
```

> **Note**: Without this flag, the application will crash.


| Argument | Description | Example |
| :---- | :---- | :---- |
| input\_file | Path to the source image (PNG/JPG). | monalisa.png |
| output\_file | Path for the result (PNG). An SVG will also be saved with the same name. | output.png |
| num\_shapes | Number of geometric shapes to generate. | 200 |
| mode | (Optional) Shape type: TRIANGLE, LINE, BEZIER, RECT, POLYLINE, ELLIPSE, COMBO. Default: TRIANGLE | COMBO |

### **Options**

| Flag | Description | Default |
| :--- | :--- | :--- |
| `--cores <number|all>` | Number of worker threads to use. | `all` |

### **GUI Mode**

To launch the Graphical User Interface:

```bash
java --add-modules jdk.incubator.vector -cp target/primitive-1.0-SNAPSHOT.jar org.trostheide.primitive.gui.PrimitiveFrame
```

**GUI Features:**
*   **Real-time Visualization**: Watch the image evolve step-by-step.
*   **Drag & Drop**: Simply drag an image file onto the window to open it.
*   **Process Log**: A live scrolling log shows exactly what the optimizer is doing (scores, time per shape).
*   **Controls**: Start/Stop button to pause or interrupt generation.
*   **Configuration**: Easy-to-use sliders and dropdowns for Mode, Shapes, and Workers.
*   **Export**: Save the current state to PNG at any time.

### **Helper Scripts**

For convenience, use the provided shell scripts:

**Run with default Triangle mode:**

# Usage: ./run_primitive.sh [num_shapes]  
./run_primitive.sh 500

**Run with specific Shape Modes:**

# Usage: ./run_shapes.sh [mode] [num_shapes]  
./run_shapes.sh rect 200  
./run_shapes.sh polyline 150  
./run_shapes.sh combo 500

**Run GUI:**

# Usage: ./run_gui.sh
./run_gui.sh

## **Architecture & Design Choices**

This project is not a direct syntax translation of the Go original; it is re-engineered for the JVM.

### **1\. Memory Management: The "Structure of Arrays"**

A naive Java port using objects (e.g., new Scanline()) inside the optimization loop would trigger massive Garbage Collection (GC) pauses, as the algorithm generates millions of candidate shapes per second.

* **Solution**: We use a ScanlineBuffer class that wraps three primitive integer arrays (y\[\], x1\[\], x2\[\]).
* **Benefit**: Zero object allocation during the rasterization and scoring phase. The buffer is reset() and reused for every mutation.

### **2\. The Hill Climbing Algorithm**

The image is constructed one shape at a time (Greedy selection).

1. **Initialization**: A shape is generated with random coordinates.
2. **Mutation Loop**:
   * The shape is slightly mutated (vertex moved, width changed).
   * It is rasterized into the ScanlineBuffer.
   * The "Energy Delta" (improvement in RMSE) is calculated.
   * If the mutation improves the image, it is kept; otherwise, it is reverted.
3. **Commit**: After N iterations (e.g., 1000), the best shape found is permanently drawn onto the canvas.

### **3\. Concurrency Model**

The process is parallelized using a ThreadPoolExecutor.

* If you request 1 shape, the system spawns N workers (where N \= CPU cores).
* Each worker runs an independent Hill Climbing simulation on its own thread-local buffers.
* The main thread collects all results, picks the global best shape, and commits it.

* The main thread collects all results, picks the global best shape, and commits it.

### **4. SIMD Optimization (Java Vector API)**

The "hot path" of the application—calculating the root-mean-square error between the target image and the generated candidates—has been optimized using the **Java Vector API (Incubator)**.

*   **Mechanism**: Processes 8 or 16 pixels simultaneously (depending on CPU AVX/AVX-512 support) instead of one by one.
*   **Result**: 
    *   **~2.6x speedup** on 80-core machine.
    *   **~1.9x speedup** on single core.

### **5. Extensibility**

* **Configuration**: A `PrimitiveConfig` record encapsulates all runtime parameters.
* **Events**: An `OptimizationListener` interface allows the runner to be decoupled from the CLI, enabling future UI integrations (e.g., Progress bars, Live Canvas).

## **📂 Project Structure**

src/main/java/org/trostheide/primitive/  
├── Main.java               \# Entry point, CLI argument parsing  
├── PrimitiveRunner.java    \# Orchestrates the thread pool and main loop  
├── core/  
│   ├── Optimizer.java      \# The Hill Climbing logic & Color Solver  
│   └── ShapeResult.java    \# Record to pass results from Workers to Main  
├── image/  
│   └── RgbaImage.java      \# Int-packed pixel storage (0xAARRGGBB)  
├── raster/  
│   └── ScanlineBuffer.java \# SoA container for raster data  
├── gui/
│   ├── PrimitiveFrame.java \# Main Swing Window
│   ├── SettingsPanel.java  \# Configuration Controls
│   └── ImagePanel.java     \# Custom Image Rendering Panel
└── shape/  
├── Shape.java          \# Interface for geometric primitives  
├── Triangle.java       \# Standard triangle rasterizer  
├── Line.java           \# Composite shape (2 Triangles)  
├── QuadraticBezier.java \# Composite shape (Series of Lines)  
├── RotatedRectangle.java \# Composite shape (Rotated Quad)  
└── Polyline.java       \# Composite shape (Series of Lines)

## **Todo & Future Roadmap**

* [x] **Live UI**: Add a Swing/JavaFX window to visualize the generation process in real-time.
* [x] **Rotated Rectangles**: Add support for RotatedRectangle primitive.
* [x] **Polylines**: Add support for Polyline primitive.
* [x] **Ellipse Support**: Add Ellipse rasterization logic.
* [ ] **Alpha Optimization**: Currently uses a fixed alpha (0.5). Implement a search step to find the optimal alpha value per shape.
* [x] **SIMD Optimization**: Implemented using Java Vector API (Incubator).

## **License**

MIT License (Ported from fogleman/primitive)