#pragma once
#include <string>

#include "../../JavaNative.h"
#include "parallel_hashmap/phmap.h"
#include "fmt/format.h"

namespace minecraft {
    class BlockIdRegistry : JavaNative<BlockIdRegistry> {
    public:
        BlockIdRegistry();

        static auto add_methods() -> void ;

    private:
        inline static phmap::flat_hash_map<short, std::string> id_to_block;
        inline static phmap::flat_hash_map<std::string, short> block_to_id;

        static auto _register(const char* name, short id) -> void ;
    };
}
