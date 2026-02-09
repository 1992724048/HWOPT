# HWOPT / 硬件加速优化模组

使用 C++ 实现核心逻辑，并通过 DPC++（SYCL）实现并行与 GPU 加速的我的世界优化模组。
An optimized Minecraft mod using C++ for core logic and DPC++ (SYCL) for parallelization and GPU acceleration.

## 状态 / Status (WIP)

正在开发中，距离正式发布还需要一段时间。  
Currently under active development. It will take some time before a public release is available.

**开发进度 / Progress:**  
[██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 4% \
**当前阶段 / Current Stage:**  
- 🛠 基础框架
  - ✅ 接口层
    - ✅ JAVA ⇄ C++ 接口
    - ✅ Dart ⇄ C++ 接口
  - ⏳ UI 层
    - 🛠 Flutter UI
    - 🛠 交互逻辑
  - ⏳ 算法层
    - 🛠 柏林噪声（BUG修复、GPU适配）
- ⏳ 功能实现
  - ⏳ 地形生成
    - ⏳ 洞穴
---
## 设备支持 / Device Support

- TBB / TBB线程池：Intel Threading Building Blocks (CPU-only execution) / 英特尔线程构建模块 (纯CPU执行)

| Hardware / 硬件                                             | Backend / 后端                                                                 | Requirements / 要求                                                                                                             | Fallback / 回退机制                                                                  | Recommendation / 推荐级别                                        |
|:----------------------------------------------------------|:-----------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------|:-------------------------------------------------------------|
| Intel CPU (11th Gen+, with iGPU) / 英特尔处理器 (11代及以后，带核显)    | SYCL (via OpenCL)                                                            | Core 11th Gen or newer with iGPU, OpenCL 3.0 support required / 需要酷睿11代及以后且带核显，需OpenCL 3.0支持                                  | Auto-fallback to TBB if SYCL acquisition fails / SYCL获取失败时自动回退至TBB               | Optional / 可选 (Default TBB recommended / 默认推荐TBB)            |
| Intel CPU (11th Gen+, without iGPU) / 英特尔处理器 (11代及以后，无核显) | TBB (CPU-only) / TBB (仅CPU)                                                  | No GPU requirement / 无GPU要求                                                                                                   | N/A (TBB only) / 不适用 (仅TBB)                                                      | Default / 默认推荐                                               |
| Intel CPU (Pre-11th Gen) / 英特尔处理器 (11代以前)                 | TBB (CPU-only) / TBB (仅CPU)                                                  | No GPU requirement / 无GPU要求                                                                                                   | N/A (TBB only) / 不适用 (仅TBB)                                                      | Default / 默认推荐                                               |
| Intel iGPU (11th Gen+) / 英特尔核显 (11代及以后 - Iris Xe)         | SYCL (Level-Zero preferred, OpenCL supported) / SYCL (优先Level-Zero，支持OpenCL) | Level-Zero 1.3+ or OpenCL 3.0 support required / 需要Level-Zero 1.3+或OpenCL 3.0支持 (Level-Zero preferred / 优先Level-Zero)         | Auto-fallback to OpenCL → TBB if Level-Zero fails / Level-Zero失败时回退至OpenCL → TBB | Preferred for GPU compute / GPU计算优先推荐                        |
| Intel iGPU (Pre-11th Gen, UHD) / 英特尔核显 (11代以前 - UHD)      | Unknown / 未测试                                                                | Untested, use at your own risk / 未测试，用户自行尝试                                                                                   | Auto-fallback to TBB if SYCL acquisition fails / SYCL获取失败时自动回退至TBB               | Try TBB first / 优先尝试TBB                                      |
| Intel dGPU (Arc series) / 英特尔独显 (Arc系列)                   | SYCL (Level-Zero preferred, OpenCL supported) / SYCL (优先Level-Zero，支持OpenCL) | Level-Zero 1.3+ or OpenCL 3.0 support required / 需要Level-Zero 1.3+或OpenCL 3.0支持 (Level-Zero preferred / 优先Level-Zero)         | Auto-fallback to OpenCL → TBB if Level-Zero fails / Level-Zero失败时回退至OpenCL → TBB | Preferred for GPU compute / GPU计算优先推荐                        |
| NVIDIA GPU / 英伟达显卡                                        | SYCL-CUDA (via oneAPI) / SYCL-CUDA (oneAPI)                                  | CUDA 12 support required via oneAPI SYCL backend / 需要通过oneAPI SYCL后端支持CUDA 12                                                 | Auto-fallback to SYCL-OpenCL → TBB if CUDA fails / CUDA失败时回退至SYCL-OpenCL → TBB   | Primary for NVIDIA / NVIDIA首选                                |
| NVIDIA GPU / 英伟达显卡                                        | SYCL-OpenCL (via oneAPI) / SYCL-OpenCL (oneAPI)                              | OpenCL support required via oneAPI SYCL backend / 需要通过oneAPI SYCL后端支持OpenCL                                                   | Auto-fallback to TBB if OpenCL fails / OpenCL失败时自动回退至TBB                         | Not recommended / 不推荐 (Use SYCL-CUDA instead / 请改用SYCL-CUDA) |
| AMD CPU / AMD处理器                                          | TBB (CPU-only) / TBB (仅CPU)                                                  | No special requirements / 无特殊要求                                                                                               | N/A (TBB only) / 不适用 (仅TBB)                                                      | Default / 默认推荐                                               |
| AMD iGPU / AMD核显                                          | Not Supported / 不支持                                                          | Missing OpenCL extensions (cl\_khr\_il\_program) on Windows / Windows缺少OpenCL扩展 (cl\_khr\_il\_program)                        | Force fallback to TBB / 强制回退至TBB                                                 | Use TBB instead / 请改用TBB                                     |
| AMD dGPU / AMD独显                                          | Not Supported / 不支持                                                          | Missing OpenCL extensions (cl\_khr\_il\_program) on Windows, HIP incompatible with Windows / Windows缺少OpenCL扩展，HIP与Windows不兼容 | Force fallback to TBB / 强制回退至TBB                                                 | Use TBB instead / 请改用TBB                                     |

## 性能测试 / Benchmark Results

> 测试环境 / Environment:
> - CPU: Ultra9 285k 24C24T
> - MEM: DDR5 48GB 6400MHz CL32 x2
> - JDK: OpenJDK 25
> - OS: Windows 11 25H2 Beta
> - Runs: 10 (avg)

| 测试项目 / Benchmark | 执行模式 / Execution Mode | 优化前 / Baseline (Java) | 优化后 / Optimized (FFM) | 性能提升 / Speedup |
|------------------|-----------------------|-----------------------|-----------------------|----------------|
| NoiseBench       | 串行 / Serial           | 469.567 ms            | 246.054 ms            | **+47.60%**    |
| NoiseBench       | 并行 / Parallel         | N/A                   | N/A                   | **N/A**        |
