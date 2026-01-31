#pragma once
#include <unordered_map>
#include <functional>
#include <optional>
#include <string>
#include <vector>
#include <type_traits>
#include <exception>

#include <stdpp/encode.h>
#include <stdpp/exception.h>
#include <stdpp/logger.h>

template<auto MemFn>
struct MethodThunk;

template<typename R, typename... Args, R(*Fn)(Args...)>
struct MethodThunk<Fn> {
    static auto call(Args... args) -> R {
        return Fn(std::forward<Args>(args)...);
    }
};

template<typename C, typename R, typename... Args, R(C::*Fn)(Args...) const>
struct MethodThunk<Fn> {
    static auto call(void* obj, Args... args) -> R {
        return (static_cast<C*>(obj)->*Fn)(std::forward<Args>(args)...);
    }
};

template<typename C, typename R, typename... Args, R(C::*Fn)(Args...)>
struct MethodThunk<Fn> {
    static auto call(void* obj, Args... args) -> R {
        return (static_cast<C*>(obj)->*Fn)(std::forward<Args>(args)...);
    }
};

class JavaNativeBase {
protected:
    ~JavaNativeBase() = default;

public:
    using Method = void*;

    static auto init_all() -> void {
        for (auto& f : creators()) {
            f();
        }
    }

    static auto get_method(const std::string& name) -> std::optional<Method> {
        auto& m = methods();
        if (!m.contains(name)) {
            return std::nullopt;
        }
        return m[name];
    }

    template<auto MemFn>
    static auto register_method(const std::string& name) -> void {
        using Thunk = MethodThunk<MemFn>;
        methods()[name] = reinterpret_cast<Method>(&Thunk::call);
    }

    static auto add_creator(std::function<void()> f) -> void {
        creators().push_back(std::move(f));
    }

private:
    static auto methods() -> std::unordered_map<std::string, Method>& {
        static std::unordered_map<std::string, Method> map;
        return map;
    }

    static auto creators() -> std::vector<std::function<void()>>& {
        static std::vector<std::function<void()>> list;
        return list;
    }
};

template<typename T>
class JavaNative : public JavaNativeBase {
public:
    virtual ~JavaNative() = default;

private:
    JavaNative() = default;

    struct AutoReg {
        AutoReg() {
            JavaNativeBase::add_creator([] {
                T::add_methods();
            });
        }
    };

    virtual auto touch() -> void* {
        return &autoreg_;
    }

    inline static AutoReg autoreg_;
    friend T;
};

class JavaUtil {
public:
    template<typename T>
    static auto to_vector(T* ptr, int size) -> std::vector<T> try {
        std::vector<T> vector(size);
        std::memcpy(vector.data(), ptr, size * sizeof(T));
        return vector;
    } catch (const std::exception& exception) {
        ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
        return {};
    } catch (const stdpp::exception::NativeException& exception) {
        ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
        return {};
    }

    template<typename T>
    static auto vector_copy(const std::vector<T>& vec, T* ptr, const int size) -> int try {
        const int min = std::min(vec.size(), static_cast<size_t>(size));
        std::memcpy(ptr, vec.data(), min * sizeof(T));
        return min;
    } catch (const std::exception& exception) {
        ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
        return 0;
    } catch (const stdpp::exception::NativeException& exception) {
        ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
        return 0;
    }
};
