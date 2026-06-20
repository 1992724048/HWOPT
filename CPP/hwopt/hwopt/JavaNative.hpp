#pragma once
#include <exception>
#include <functional>
#include <optional>
#include <span>
#include <string>
#include <type_traits>
#include <unordered_map>
#include <vector>

#include <stdpp/encode.h>
#include <stdpp/exception.h>
#include <stdpp/logger.h>

#ifdef HWOPT_EXPORTS
#define HWOPT_API __declspec(dllexport)
#else
#define HWOPT_API __declspec(dllimport)
#endif

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

class HWOPT_API JavaNativeBase {
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

    class MethodBinder {
    public:
        explicit MethodBinder(const std::string_view name) {
            this->name = name;
        }

        template<auto MemFn>
        auto reg() const -> void {
            using Thunk = MethodThunk<MemFn>;
            methods()[name] = reinterpret_cast<Method>(&Thunk::call);
        }
    private:
        std::string name;
    };
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

template<typename T, bool AutoCreation = true>
class JavaNative : public JavaNativeBase {
    JavaNative() {
        JavaNative::touch();
    }
protected:
    ~JavaNative() = default;
public:
    static constexpr bool isAutoCreation = AutoCreation;
private:
    struct AutoReg {
        AutoReg() {
            JavaNativeBase::add_creator([] {
                T::add_methods();
            });
        }
    };

    virtual auto touch() -> void* {
        return &auto_reg_;
    }

    static AutoReg auto_reg_;
    friend T;
};

template<typename T, bool AutoCreation>
inline JavaNative<T, AutoCreation>::AutoReg JavaNative<T, AutoCreation>::auto_reg_;

inline auto operator""_jf(const char* name, size_t _) -> JavaNativeBase::MethodBinder {
    return JavaNativeBase::MethodBinder{name};
}

class JavaUtil {
public:
    template<typename T>
    static auto to_span(T* ptr, int size) -> std::span<T> {
        return std::span<T>(ptr, size);
    }
};
