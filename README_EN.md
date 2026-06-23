<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="icon.png"><source media="(prefers-color-scheme: light)" srcset="icon.png"><img alt="HWOPT" src="icon.png" width="128"></picture>

# HWOPT — Hardware-Accelerated Optimization Mod

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![NeoForge](https://img.shields.io/badge/NeoForge-26.2.0.6-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-WIP-red)

[中文](README.md)

</div>

HWOPT (Hardware Optimization) is a Minecraft optimization mod that rewrites vanilla algorithms in C++, using the Java 25 FFM API for low-overhead native calls, with GPU (SYCL/DPC++) acceleration to boost game performance.

## Design Philosophy

We avoid blindly applying multi-threading or GPU acceleration. Every optimization is evaluated against real-world scenarios to determine whether it genuinely benefits performance — using these techniques for their own sake would only saturate compute resources and degrade system responsiveness with little to no gain. This mod prioritizes compatibility with other mods over pushing performance to its absolute limits.

## Community

- [pd34429710](https://pd.qq.com/s/fcy3gqy4x) (QQ Channel)

## Project Status

**Work in progress...** GPU acceleration (SYCL) and remaining optimizations are under development.

#### Feature List

| Name                              | Category     | Status    | Module Version | Notes                                                                                         |
| --------------------------------- | ------------ | --------- | -------------- | --------------------------------------------------------------------------------------------- |
| Terrain Generation Optimization   | Optimization | Completed | 26.1           | Optimizes vanilla chunk generation speed (compatibility with other terrain mods prioritized)  |
| Entity Collision Optimization     | Optimization | Completed | 26.1           | Batch AABB collision (native C++), fixes race conditions, step-up bugs, twitching             |
| Entity Rendering Culling          | Optimization | Completed | 26.1           | Async background occlusion + 2×2×2 cell grouping + Cullable cache, supports tick/block culling|
| Entity AI Optimization            | Optimization | Completed | 26.1           | Pathfinding cooldown (5 ticks)                                                                |
| Particle Async Tick Optimization  | Optimization | Completed | 26.1           | ForkJoinPool parallel update + light cache + spin-lock ring buffer + throttle extraction      |
| Network Packet Compression        | Optimization | Completed | 26.1           | Batches packets using ICX-optimized ZSTD compression, uses ID indices instead of packet strings |
| Village Spawn                     | Utility      | Completed | 26.1           | Sets world spawn point to a village                                                           |


- Module version format: `year/revision`

#### Mod Loader & Game Version Support

| Game Version | Mod Version | NeoForge | Forge | Fabric |
| ------------ | ----------- | -------- | ----- | ------ |
| 26.2         | 26.1.x      | ✓        | ✕     | ✕      |

#### OS & Hardware Support

| Mod Version | Windows  | Linux | Intel CPU | Intel iGPU | Intel dGPU | AMD CPU | AMD iGPU | AMD dGPU | NVIDIA dGPU  |
| ----------- | -------- | ----- | --------- | ---------- | ---------- | ------- | -------- | -------- | ------------ |
| 26.1.x      | ≥10 19H1 | ✕     | AVX2      | 11th Gen+  | ✓          | AVX2    | ✕        | ✕        | CUDA 12.0+\* |

- `Intel 11th Gen and earlier`/`AMD`/`NVIDIA` — no hardware available for testing; table content is based on documentation references
- Basic CPU functionality: any qualifying CPU works
- SYCL/DPC++ (GPU acceleration): requires compatible hardware, drivers, and runtime — see [Intel oneAPI System Requirements](https://www.intel.com/content/www/us/en/developer/articles/system-requirements/intel-oneapi-base-toolkit-system-requirements.html) for details
- Mod version format: `year/feature version/revision`
- Pre-built binaries are Windows x64 only; self-compilation enables Linux support and other GPU backends
- `*` NVIDIA/AMD GPU backends rely on Codeplay plugins, which have been discontinued — future updates may be affected

#### FAQ

1. Why is this more suitable for `iGPU` than `dGPU`?

- In the SYCL programming model, there are three memory types: `host` memory (system RAM), `device` memory (VRAM), and `shared` memory (automatically migrated by the driver, but still incurs implicit copies). Most GPU acceleration solutions use `device` memory, requiring repeated data copies between system RAM and VRAM at a high cost. `shared` memory simplifies programming but still relies on driver-level data migration and cannot fully eliminate copies. This mod uniformly uses `host` memory, completely avoiding such copy overhead, making it especially favorable for `iGPU`s where system memory doubles as VRAM. Modern `iGPUs` are typically connected via the `Ring Bus` rather than PCIE, and come with large caches, allowing direct access to system memory (some models even feature L3/L4 caches) without additional copying. In contrast, `dGPUs` connected via PCIE suffer from higher latency and mandatory frequent data transfers, making this approach unsuitable.

2. Why choose SYCL over Vulkan compute shaders?

- SYCL is a single-source C++ programming model that allows writing GPU kernels directly in C++ code, eliminating the need to learn shader languages like GLSL/HLSL, reducing code duplication and porting costs. Vulkan compute shaders require managing complex pipeline states, memory barriers, and descriptor sets, resulting in lower development efficiency. For a mod like Minecraft that involves heavy algorithm porting, SYCL's C++ integration and cross-platform support are significantly superior to Vulkan.

3. What advantage does the Intel C++ compiler have over MSVC for compiling native DLLs?

- Across multiple benchmarks against Clang, GCC, MSVC, and other compilers, the Intel C++ compiler (ICX) consistently delivers excellent performance, especially on Intel hardware where it better exploits SIMD instruction sets. Furthermore, the mod's native layer heavily depends on the Intel oneAPI ecosystem (DPC++, oneTBB, etc.). Using ICX ensures ABI consistency across components and avoids cross-compiler linking compatibility issues. Reference: [Tencent boosts MySQL performance up to 85% with Intel® oneAPI tools](https://www.intel.com/content/www/us/en/developer/articles/technical/tencent-gains-85-percent-boost-for-mysql.html)

4. Does SYCL have a performance advantage over Vulkan for this project?

- **Yes.** On the same Intel iGPU, SYCL + Level Zero achieves 55,556 ops/s (f32) — **5.4×** the throughput of Vulkan f32 (10,183 ops/s).
- **Test environment:** Intel Core Ultra 9 285K (iGPU) + Intel oneAPI 2026.0 (SYCL) / Vulkan 1.4.350
- **Workload:** 32³ (32,768 points) × 10,000 iterations

  | Combination           | Device                  | Precision |  Memory  |   Total    |  Per call   |    Throughput    |
  | --------------------- | ----------------------- | :-------: | :------: | :--------: | :---------: | :--------------: |
  | Vulkan                | Ultra 9 285K (iGPU)     |    f32    |   Host   |   982 ms   |   98.2 μs   |   10,183 ops/s   |
  | **SYCL + Level Zero** | **Ultra 9 285K (iGPU)** |  **f32**  | **Host** | **180 ms** | **18.0 μs** | **55,556 ops/s** |
  | SYCL + Level Zero     | Ultra 9 285K (iGPU)     |    f64    |   Host   |   695 ms   |   69.5 μs   |   14,388 ops/s   |
  | Vulkan                | AMD RX 6600             |    f32    |   Host   |   941 ms   |   94.1 μs   |   10,627 ops/s   |

- **Reproduce:** Test code at `CPP/hwopt/hwopt-benchmark/MathBenchmark.cpp`, anyone with compatible hardware and toolchain can re-run the benchmarks.

## Quick Start

### Prerequisites

- **Java 25**
- **NeoForge** 26.1.2.75 (Minecraft 26.1.2)
- **Windows x64** (native DLLs pre-built for win64; other platforms require manual C++ build)

### Build

```bash
cd JAVA
./gradlew build
```

Native DLLs (`hwopt.dll`, `hwopt-sycl.dll`, `Fortran.dll`, etc.) are pre-placed at `src/main/resources/native/win64/` and bundled automatically during build.

C++ Developers:

- Visual Studio 2026
- Intel® oneAPI Base Toolkit 2026.0
- Intel VTune (optional)
- Intel Advisor (optional)

## Issue Submission Guidelines

### Title Format

```
[Crash/Bug/Suggestion/...] Description
```

### Required Information

- **latest.log** (compress as zip and upload)
- **Mod version** — please specify the version number
- **Reproduction steps** — configuration options, world seed, etc.
- **Other mods present** — yes/no
- **Device information**
  - CPU / GPU model
  - OS version (e.g., Windows 11 25H2 26200.8655)
  - Clean system (unmodified image, no GPU model spoofing, no game optimization tools, no system patching tools; otherwise please specify)
  - Hardware drivers installed/updated

## PR Merge Guidelines

No strict requirements beyond confirming the following before submitting a PR:

- [ ] Local build passes
- [ ] Game runs without errors
- [ ] Basic in-game testing of modified/added functionality
- [ ] Branch has no conflicts (not mandatory)
- [ ] AI was involved in development
  - [ ] AI-generated code has been verified/tested
  - [ ] AI-generated code has been reviewed for readability/maintainability
