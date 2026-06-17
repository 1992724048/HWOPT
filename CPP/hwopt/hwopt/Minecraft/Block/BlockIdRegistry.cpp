// 2026-06-17 03:26:14

#include "BlockIdRegistry.h"

using namespace minecraft::block;

BlockIdRegistry::BlockIdRegistry() {
    JavaNative::touch();
}

auto BlockIdRegistry::add_methods() -> void {
    "BlockIdRegistry::registry"_jf.reg<_register>();
}

auto BlockIdRegistry::_register(const char* name, const short id) -> void {
    DLOG << std::format("id: {} <-> {}", id, name);
    id_to_block[id] = name;
    block_to_id[name] = id;
}
