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
        case DLL_THREAD_ATTACH:
        case DLL_THREAD_DETACH:
        case DLL_PROCESS_DETACH:
            break;
    }
    return TRUE;
}

extern "C" API auto PerlinNoise_create(const int first_octave, const double* amplitudes, const int size, const bool use_new_initialization) -> std::uint64_t try {
    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    std::vector<double> amps(size);
    std::memcpy(amps.data(), amplitudes, sizeof(double) * size);
    return reinterpret_cast<std::uint64_t>(new PerlinNoise({first_octave, amps}, use_new_initialization));
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
} catch (std::exception& exception) {
    ELOG << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_destroy(const std::uint64_t this_) -> void try {
    if (this_) {
        delete reinterpret_cast<PerlinNoise*>(this_);
    }
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return;
}

extern "C" API auto PerlinNoise_getValue_3(const PerlinNoise* this_, const double x, const double y, const double z) -> double try {
    return this_->get_value(x, y, z);
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_getValue_6(const PerlinNoise* this_, const double x, const double y, const double z, const double yScale, const double yFudge, const int yFlatHack) -> double try {
    return this_->get_value(x, y, z, yScale, yFudge, yFlatHack);
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_edgeValue(const std::uint64_t this_, const double noise_value) -> double try {
    return reinterpret_cast<PerlinNoise*>(this_)->edge_value(noise_value);
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_firstOctave(const std::uint64_t this_) -> double try {
    return reinterpret_cast<PerlinNoise*>(this_)->get_first_octave();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_amplitudes_size(const std::uint64_t this_) -> int try {
    return reinterpret_cast<PerlinNoise*>(this_)->get_amplitudes().size();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_amplitudes(const std::uint64_t this_, double* ptr, const int size) -> int try {
    const std::vector<double> vec = reinterpret_cast<PerlinNoise*>(this_)->get_amplitudes();
    std::memcpy(ptr, vec.data(), sizeof(double) * size);
    return size;
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto PerlinNoise_maxValue(const std::uint64_t this_) -> double try {
    return reinterpret_cast<PerlinNoise*>(this_)->get_max_value();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    return 0;
}

extern "C" API auto JAVA_ResolveFunction(const char* name) -> void* try {
    if (std::string(name) == "PerlinNoise_create") {
        return PerlinNoise_create;
    }
    if (std::string(name) == "PerlinNoise_destroy") {
        return PerlinNoise_destroy;
    }
    if (std::string(name) == "PerlinNoise_getValue_3") {
        return PerlinNoise_getValue_3;
    }
    if (std::string(name) == "PerlinNoise_getValue_6") {
        return PerlinNoise_getValue_6;
    }
    if (std::string(name) == "PerlinNoise_edgeValue") {
        return PerlinNoise_edgeValue;
    }
    if (std::string(name) == "PerlinNoise_firstOctave") {
        return PerlinNoise_firstOctave;
    }
    if (std::string(name) == "PerlinNoise_amplitudes_size") {
        return PerlinNoise_amplitudes_size;
    }
    if (std::string(name) == "PerlinNoise_amplitudes") {
        return PerlinNoise_amplitudes;
    }
    if (std::string(name) == "PerlinNoise_maxValue") {
        return PerlinNoise_maxValue;
    }
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << std::hex << exception.code() << " " << exception.what();
    MessageBoxA(nullptr, std::to_string(exception.code()).data(), exception.what(), 0);
    return nullptr;
} catch (std::exception& exception) {
    ELOG << exception.what();
    MessageBoxA(nullptr, "ERROR", exception.what(), 0);
    return nullptr;
}
