#include "BlockIdRegistry.h"
#include <eh.h>
#include <exception>

using namespace minecraft::block;

BlockIdRegistry::BlockIdRegistry() {
    JavaNative::touch();
}

auto BlockIdRegistry::add_methods() -> void {
    "BlockIdRegistry::registry"_jf.reg<_register>();
}

auto BlockIdRegistry::_register(const char* name, const short id) -> void try {
    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    DLOG << std::format("[BlockIdRegistry] block: {} | {}", id, name);
    id_to_block[id] = name;
    block_to_id[name] = id;
} catch (const std::exception& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
}
