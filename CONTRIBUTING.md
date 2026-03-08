# Contributing to Primitive (Java Port)

## Development Setup

1. Clone the repository
2. Ensure Java 17+ and Maven 3.6+ are installed
3. Build: `mvn clean package`
4. Run with SIMD: `java --add-modules jdk.incubator.vector -jar target/primitive-1.0-SNAPSHOT.jar`

## Architecture

The codebase follows a performance-first design:

- **ScanlineBuffer** (Structure of Arrays) — zero-allocation hot path for shape rasterization
- **HillClimber** — parallel workers mutating shapes to minimize RMSE
- **ShapeRenderer** — converts shapes to scanlines for pixel-level comparison
- **SvgExporter** — generates infinite-resolution vector output

## Performance Notes

- The `--add-modules jdk.incubator.vector` flag is mandatory for SIMD acceleration
- Hot path allocations must be avoided — use primitive arrays, not objects
- Benchmark before and after changes to rasterization code

## Code Style

- Java 17 features (records, sealed classes, pattern matching)
- 4-space indent, standard Java conventions
- Performance-critical code: document allocation behavior

## Commit Messages

Use conventional prefixes: `feat:`, `fix:`, `perf:`, `docs:`, `refactor:`, `test:`, `chore:`

## License

This project is licensed under AGPL-3.0 (inherited from the original Go implementation by Michael Fogleman).
