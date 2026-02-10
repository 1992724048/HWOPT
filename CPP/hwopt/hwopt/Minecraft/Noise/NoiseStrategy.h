// 遂沫 NoiseStrategy.h
// 2026-02-11 02:05:27

#pragma once

#include <glm/glm.hpp>
#include <proxy/proxy.h>

PRO_DEF_MEM_DISPATCH(MemNoise, noise);

struct NoiseStrategy : pro::facade_builder::add_convention<MemNoise, std::vector<double>(const std::vector<glm::vec3>& pos_vec)>::build {};

class NoiseContext {
public:
    auto noise(const std::vector<glm::vec3>& pos_vec) -> std::vector<double> {
        return strategy ? strategy->noise(pos_vec) : std::vector<double>();
    }

    template<typename MODE>
    auto set() -> void {
        strategy = pro::make_proxy<NoiseStrategy, MODE>();
    }

private:
    pro::proxy<NoiseStrategy> strategy;
};
