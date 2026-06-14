#pragma comment(lib, "ntdll.lib")
// ReSharper disable CppUnusedIncludeDirective
// ReSharper disable CppWrongIncludesOrder
#include <mimalloc/mimalloc.h>
#include <windows.h>
#include <string>
#include <future>
#include <iostream>
#include <ostream>
#include <sstream>
#include <expected>

#include <magic_enum/magic_enum.hpp>
#include <sycl-plugin.h>
#include <stdpp/logger.h>
#include "JavaNative.h"

using namespace std::chrono_literals;

#define API __declspec(dllexport)

auto APIENTRY DllMain(HMODULE hModule, const DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
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

auto init_sycl_device() -> void {
    auto exp = stdpp::sycl::Device::get_device();
    if (!exp) {
        return;
    }

    std::stringstream ss;
    ss << "\n设备列表:\n";

    for (const auto& [type, name, platform] : exp.value()) {
        ss << magic_enum::enum_name<stdpp::sycl::DeviceType>(type) << ": " << name << " (" << platform << ")\n";
    }

    IMSG << ss.str();
}

extern "C" API auto JAVA_ResolveFunction(const char* name) -> void* {
    static std::once_flag flag;
    std::call_once(flag, init_sycl_device);

    if (const auto opt = JavaNativeBase::get_method(name)) {
        return opt.value();
    }
    return nullptr;
}
