# HWOPT / 硬件加速优化模组
使用 C++ 实现核心逻辑，并通过 DPC++（SYCL）实现并行与 GPU 加速。  
Core logic is implemented in C++, with parallel and GPU acceleration via DPC++ (SYCL).

## 状态 / Status
正在开发中，距离正式发布还需要一段时间。  
Currently under active development. It will take some time before a public release is available.

## 性能测试 / Benchmark Results

| 测试项目 / Benchmark | 优化前 / Baseline | 优化后 / Optimized | 性能提升 / Improvement |
|----------------------|-------------------|--------------------|------------------------|
| NoiseBench           | 100%              | 144.5%             | +44.5%                 |

> 当前在 NoiseBench 测试中取得了约 44.5% 的性能提升。  
> Currently, a performance improvement of approximately 44.5% has been achieved in the NoiseBench benchmark.
