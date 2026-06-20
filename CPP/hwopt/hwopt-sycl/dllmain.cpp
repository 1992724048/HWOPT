#include <windows.h>
#include <mimalloc/mimalloc.h>

#include "stdpp/logger.h"

auto DllMain(HMODULE, DWORD ul_reason_for_call, LPVOID) -> BOOL {
    if (ul_reason_for_call == DLL_PROCESS_ATTACH) {
        stdpp::log::Logger::set_level(stdpp::log::Level::Trace, stdpp::log::LoggerType::ConsoleLogger);
        mi_stats_reset();
    }
    return TRUE;
}
