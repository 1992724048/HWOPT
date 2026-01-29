#include <windows.h>

#include "JavaNative.h"

#include "stdpp/logger.h"
#include "stdpp/exception.h"
#include "Noise/PerlinNoise.h"

#define API __declspec(dllexport)

auto APIENTRY DllMain(HMODULE hModule, const DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            stdpp::log::Logger::set_level(stdpp::log::Level::Trace, stdpp::log::LoggerType::ConsoleLogger);
            JavaNativeBase::init_all();
            break;
        case DLL_THREAD_ATTACH:
            thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
            break;
        case DLL_THREAD_DETACH:
            break;
        case DLL_PROCESS_DETACH:
            break;
    }
    return TRUE;
}

extern "C" API auto JAVA_ResolveFunction(const char* name) -> void* {
    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    if (const auto opt = JavaNativeBase::get_method(name)) {
        return opt.value();
    }
    return nullptr;
}