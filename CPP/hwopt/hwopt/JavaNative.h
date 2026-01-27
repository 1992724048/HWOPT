#pragma once
#include <unordered_map>
#include <functional>
#include <optional>
#include <string>
#include <vector>
#include <type_traits>

#include <stdpp/logger.h>

template<auto MemFn>
struct MethodThunk;

template<typename C, typename R, typename... Args, R(C::*Fn)(Args...) const>
struct MethodThunk<Fn> {
    static auto call(void* obj, Args... args) -> R {
        return (static_cast<C*>(obj)->*Fn)(std::forward<Args>(args)...);
    }
};

template<typename R, typename... Args, R(*Fn)(Args...)>
struct MethodThunk<Fn> {
    static auto call(void*, Args... args) -> R {
        return Fn(std::forward<Args>(args)...);
    }
};

template<typename C, typename R, typename... Args, R(C::*Fn)(Args...)>
struct MethodThunk<Fn> {
    static auto call(void* obj, Args... args) -> R {
        return (static_cast<C*>(obj)->*Fn)(std::forward<Args>(args)...);
    }
};

class JavaNativeBase {
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
        DLOG << name;
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
