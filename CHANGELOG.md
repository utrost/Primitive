# Changelog

All notable changes to Primitive (Java Port) will be documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- CONTRIBUTING.md
- This CHANGELOG
- CI matrix extended to Java 21

## [1.0-SNAPSHOT] — 2025

### Added
- High-performance Java port of fogleman/primitive
- 7 shape modes: Triangle, Line, Bézier, RotatedRect, Polyline, Ellipse, Combo
- Zero-allocation hot path (Structure of Arrays pattern)
- Multi-core parallelism (embarrassingly parallel workers)
- SIMD optimization via Java Vector API (~2.6x speedup)
- Swing GUI with real-time visualization and drag & drop
- Dual output: PNG + SVG
- Analytical color solving (RMSE minimization)
- AGPL-3.0 license
- GitHub Actions CI
