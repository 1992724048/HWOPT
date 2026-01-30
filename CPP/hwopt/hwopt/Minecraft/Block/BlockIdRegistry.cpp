#include "BlockIdRegistry.h"

minecraft::BlockIdRegistry::BlockIdRegistry() {
    JavaNative::touch();
}

auto minecraft::BlockIdRegistry::add_methods() -> void {
    register_method<_register>("BlockIdRegistry::registry");
}

auto minecraft::BlockIdRegistry::_register(const char* name, const short id) -> void try {
    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    DLOG << fmt::format("[BlockIdRegistry] block: {} | {}", id, name);
    id_to_block[id] = name;
    block_to_id[name] = id;
} catch (const std::exception& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
}
