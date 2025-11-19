# **Primitive (Java Port)**

A Java port of Michael Fogleman's [primitive](https://github.com/fogleman/primitive). This project reproduces images using geometric primitives (triangles, lines, curves) by hill-climbing optimization.

It transforms raster images (PNG/JPG) into abstract vector art (SVG) and simplified raster approximations.

## ** Key Features**

* **High Performance**: Designed with a "Zero-Allocation" hot path. Uses a **Structure of Arrays (SoA)** pattern (ScanlineBuffer) to avoid Garbage Collector thrashing during the millions of mutations required for image generation.
* **Multi-Core Parallelism**: Uses an "embarrassingly parallel" architecture where independent workers race to find the best shape for each step.
* **Multiple Primitives**: Supports Triangles, Thick Lines, Quadratic Bezier Curves, and a "Combo" mode.
* **Dual Output**: Generates both a raster preview (.png) and an infinite-resolution vector file (.svg).
* **Analytical Color Solving**: Mathematically calculates the optimal color for every shape to minimize Root Mean Square Error (RMSE), rather than guessing colors randomly.

## **📦 Installation & Requirements**

### **Prerequisites**

* **Java 17** or higher (Required for record types and modern switch syntax).
* **Maven** 3.6+

### **Build**

Clone the repository and build with Maven:

mvn clean package

## **🏃 Usage**

### **Basic Command Line**

You can run the resulting JAR file directly:

java \-jar target/primitive-1.0-SNAPSHOT.jar \<input\_file\> \<output\_file\> \<num\_shapes\> \[mode\]

| Argument | Description | Example |
| :---- | :---- | :---- |
| input\_file | Path to the source image (PNG/JPG). | monalisa.png |
| output\_file | Path for the result (PNG). An SVG will also be saved with the same name. | output.png |
| num\_shapes | Number of geometric shapes to generate. | 200 |
| mode | (Optional) Shape type: TRIANGLE, LINE, BEZIER, COMBO. Default: TRIANGLE | COMBO |

### **Helper Scripts**

For convenience, use the provided shell scripts:

**Run with default Triangle mode:**

\# Usage: ./run\_primitive.sh \[num\_shapes\]  
./run\_primitive.sh 500

**Run with specific Shape Modes:**

\# Usage: ./run\_shapes.sh \[mode\] \[num\_shapes\]  
./run\_shapes.sh bezier 200  
./run\_shapes.sh combo 500

## **🏗 Architecture & Design Choices**

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

## ** Project Structure**

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
└── shape/  
├── Shape.java          \# Interface for geometric primitives  
├── Triangle.java       \# Standard triangle rasterizer  
├── Line.java           \# Composite shape (2 Triangles)  
└── QuadraticBezier.java \# Composite shape (Series of Lines)

## ** Todo & Future Roadmap**

* \[ \] **Live UI**: Add a Swing/JavaFX window to visualize the generation process in real-time.
* \[ \] **Rotated Rectangles**: Add support for RotatedRectangle primitive.
* \[ \] **Ellipse Support**: Add Ellipse rasterization logic.
* \[ \] **Alpha Optimization**: Currently uses a fixed alpha (0.5). Implement a search step to find the optimal alpha value per shape.
* \[ \] **SIMD Optimization**: Explore Java's Vector API (Incubator) to speed up the RMSE pixel scoring loop.

## ** License**

MIT License (Ported from fogleman/primitive)