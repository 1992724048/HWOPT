# HWOPT / 硬件加速优化模组

使用 C++ 实现核心逻辑，并通过 DPC++（SYCL）实现并行与 GPU 加速的我的世界优化模组。
An optimized Minecraft mod using C++ for core logic and DPC++ (SYCL) for parallelization and GPU acceleration.

## 状态 / Status (WIP)

正在开发中，距离正式发布还需要一段时间。  
Currently under active development. It will take some time before a public release is available.

## 设备支持 / Device Support

| Hardware   | 硬件      | Requirements                                                     | 要求                                            |
|------------|---------|------------------------------------------------------------------|-----------------------------------------------|
| Intel CPU  | 英特尔处理器  | OpenCL support required                                          | 需要 OpenCL 支持                                  |
| Intel iGPU | 英特尔核显   | OpenCL or Level-Zero support required                            | 需要 OpenCL 或 Level-Zero 支持                     |
| Intel dGPU | 英特尔独显   | OpenCL or Level-Zero support required                            | 需要 OpenCL 或 Level-Zero 支持                     |
| NVIDIA GPU | 英伟达显卡   | CUDA 12 or OpenCL support required                               | 需要 CUDA 12 或 OpenCL 支持                        |
| AMD CPU    | AMD 处理器 | OpenCL support required                                          | 需要 OpenCL 支持                                  |
| AMD iGPU   | AMD 核显  | Drivers lack OpenCL extensions on Windows (cl\_khr\_il\_program) | Windows 驱动缺少 OpenCL 扩展 (cl\_khr\_il\_program) |
| AMD dGPU   | AMD 独显  | Drivers lack OpenCL extensions on Windows (cl\_khr\_il\_program) | Windows 驱动缺少 OpenCL 扩展 (cl\_khr\_il\_program) |

## 性能测试 / Benchmark Results

> 测试环境 / Environment:
> - CPU: Ultra9 285k 24C24T
> - MEM: DDR5 48GB 6400MHz CL32 x2
> - JDK: OpenJDK 25
> - OS: Windows 11 25H2 Beta
> - Runs: 10 (avg)

| 测试项目 / Benchmark | 执行模式 / Execution Mode | 优化前 / Baseline (Java) | 优化后 / Optimized (FFM) | 性能提升 / Speedup |
|------------------|-----------------------|-----------------------|-----------------------|----------------|
| NoiseBench       | 串行 / Serial           | 469.578 ms            | 250.387 ms            | **+46.68%**    |
| NoiseBench       | 并行 / Parallel         | N/A                   | N/A                   | **N/A**        |