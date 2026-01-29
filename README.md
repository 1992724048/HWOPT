# HWOPT / 硬件加速优化模组
使用 C++ 实现核心逻辑，并通过 DPC++（SYCL）实现并行与 GPU 加速。  
Core logic is implemented in C++, with parallel and GPU acceleration via DPC++ (SYCL).

## 状态 / Status
正在开发中，距离正式发布还需要一段时间。  
Currently under active development. It will take some time before a public release is available.

## 性能测试 / Benchmark Results

> 测试环境 / Environment:  
> - CPU: Ultra9 285k 24C24T
> - MEM: DDR5 48GB 6400MHz CL32 x2
> - JDK: OpenJDK 25 
> - OS: Windows 11 25H2 Beta
> - Runs: 10 (avg)

| 测试项目 / Benchmark | 执行模式 / Execution Mode | 优化前 / Baseline (Java) | 优化后 / Optimized (FFM) | 性能提升 / Speedup |
|---------------------|---------------------------|-------------------------|--------------------------|-------------------|
| NoiseBench          | 串行 / Serial             | 4613.318 ms             | 3181.600 ms              | **+31.03%**       |
| NoiseBench          | 并行 / Parallel           | N/A                     | N/A                      | **N/A**           |
