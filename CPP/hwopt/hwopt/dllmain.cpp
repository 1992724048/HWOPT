#pragma comment(lib, "ntdll.lib")
// ReSharper disable CppUnusedIncludeDirective
// ReSharper disable CppWrongIncludesOrder
#include <mimalloc/mimalloc.h>
#include <windows.h>
#include <sycl-plugin.h>
#include <utility>
#include <stdpp/logger.h>
#include <stdpp/exception.h>

#include "Global.hpp"
#include "JavaNative.hpp"

using namespace std::chrono_literals;

#define API __declspec(dllexport)

auto init_sycl_device() -> void {
    sycl::Device::log_devices();

    if (auto dev = sycl::Device::create_device()) {
        hwopt::global::handle = std::move(dev.value());
    } else {
        CMSG(CLOG) << dev.error();
    }
}

auto APIENTRY DllMain(HMODULE hModule, const DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            stdpp::exception::Crash::set_callback(nullptr);
            stdpp::log::Logger::set_level(stdpp::log::Level::Trace, stdpp::log::LoggerType::ConsoleLogger);
            mi_stats_reset();
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
    std::call_once(flag, init_sycl_device);
    if (const auto opt = JavaNativeBase::get_method(name)) {
        DLOG << "函数 '" << name << "' 地址: 0x" << std::hex << reinterpret_cast<int64_t>(opt.value());
        return opt.value();
    }

    ELOG << "函数 '" << name << "' 未找到!";
    return nullptr;
}
