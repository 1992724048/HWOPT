#include <chrono>
#include <future>
#include <windows.h>
#include <magic_enum/magic_enum.hpp>
#include <tbb/tbb.h>

#include <stdpp/logger.h>

#include <sycl-plugin.h>

#include "JavaNative.h"

using namespace std::chrono_literals;

#define API __declspec(dllexport)

auto APIENTRY DllMain(HMODULE hModule, const DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            stdpp::log::Logger::set_level(stdpp::log::Level::Trace, stdpp::log::LoggerType::ConsoleLogger);
            JavaNativeBase::init_all();
            break;
        case DLL_THREAD_ATTACH:
            break;
        case DLL_THREAD_DETACH:
            break;
        case DLL_PROCESS_DETACH:
            break;
        default: ;
    }
    return TRUE;
}

extern "C" API auto JAVA_ResolveFunction(const char* name) -> void* {
    static std::once_flag flag;
    std::call_once(flag,
                   [] -> void {
                       if (auto exp = stdpp::sycl::Device::get_device()) {
                           for (const auto& [type, name, platform] : exp.value()) {
                               ILOG << magic_enum::enum_name<stdpp::sycl::DeviceType>(type) << ": " << name << " (" << platform << ")";
                           }
                       }
                   });
    if (const auto opt = JavaNativeBase::get_method(name)) {
        return opt.value();
    }
    WLOG << name << " not found!";
    return nullptr;
}
