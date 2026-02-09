import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class FFIException implements Exception {
  final String code;
  final String message;

  FFIException(this.code, this.message);

  @override
  String toString() => 'FFIException($code): $message';
}

const _kTag = 'FFI';

abstract final class FFI {
  FFI._();

  static const MethodChannel _channel = MethodChannel('ffi_cpp');
  static bool isSetMethodCallHandler = false;

  static Future<dynamic> invoke(String method, {Map<String, dynamic>? params}) async {
    try {
      final Map<String, dynamic> arguments = {if (params != null) ...params};
      return await _channel.invokeMethod(method, arguments);
    } on PlatformException catch (e) {
      throw FFIException(e.code, "${e.message ?? 'Unknown platform error'}\nMethod: $method\nParams:$params");
    } catch (e, s) {
      debugPrint('[$_kTag] invoke($method) error: $e\n$s');
      throw FFIException('INVOKE_ERROR', "$e\nMethod: $method\nParams:$params");
    }
  }

  static final Map<String, Future<dynamic> Function(Map?)> _methodHandler = {};

  static void registerMethod(String method, Future<dynamic> Function(Map<dynamic, dynamic>? args) handler) {
    if (!isSetMethodCallHandler) {
      _channel.setMethodCallHandler(_onPlatformCall);
      isSetMethodCallHandler = true;
    }
    _methodHandler[method] = handler;
  }

  static void unregisterMethod(String method) => _methodHandler.remove(method);

  static Future<dynamic> _onPlatformCall(MethodCall call) async {
    if (!_methodHandler.containsKey(call.method)) {
      throw MissingPluginException('FFI: method "${call.method}" not registered on Dart side');
    }
    final handler = _methodHandler[call.method];
    if (handler == null) {
      throw MissingPluginException('FFI: method "${call.method}"  call is null');
    }
    try {
      return await handler(call.arguments as Map?);
    } catch (e, s) {
      debugPrint('[$_kTag] handler(${call.method}) throw: $e\n$s');
      rethrow;
    }
  }
}
