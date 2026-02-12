// 遂沫 NoiseStrategy.h
// 2026-02-12 19:28:29

#pragma once

#include <glm/glm.hpp>
#include <proxy/proxy.h>
#include "ImprovedNoise.h"

PRO_DEF_MEM_DISPATCH(PerlinNoiseGetValue, get_value);

struct PerlinNoiseStrategy : pro::facade_builder::add_convention<PerlinNoiseGetValue, std::vector<double>(const std::vector<glm::vec3>& pos_vec)>::build {};

class NoiseContext {
public:
    template<typename MODE>
    auto set() -> void {
        strategy = pro::make_proxy<PerlinNoiseStrategy, MODE>();
    }

private:
    pro::proxy<PerlinNoiseStrategy> strategy;
};
