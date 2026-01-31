#include <windows.h>

#include "JavaNative.h"

#include "stdpp/logger.h"
#include "stdpp/exception.h"
#include "Minecraft/Noise/PerlinNoise.h"
#include "Minecraft/Block/BlockIdRegistry.h"
#include "Minecraft/Chunk/NoiseChunkGenerator.h"

#include <magic_enum/magic_enum.hpp>

#include <sycl-plugin.h>

using namespace std::chrono_literals;

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
    static std::once_flag flag;
    std::call_once(flag,
                   [] {
                       if (auto exp = stdpp::sycl::Device::get_device()) {
                           for (auto& [type, names] : exp.value()) {
                               for (auto& [device, platform] : names) {
                                   ILOG << magic_enum::enum_name<decltype(type)>(type) << ": " << device << " (" << platform << ")";
                               }
                           }
                       }
                   });

    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    if (const auto opt = JavaNativeBase::get_method(name)) {
        return opt.value();
    }
    WLOG << name << " not found!";
    return nullptr;
}
