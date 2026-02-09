import 'package:flutter/material.dart';
import 'package:ui/ffi.dart';

void main() {
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    FFI.registerMethod("test", (Map<dynamic, dynamic>? args) async {
      await FFI.invoke("test", params: {"arg1": args?["1"]});
      return 1145;
    });
    return const MaterialApp(
      home: Scaffold(body: Center(child: Text('Hello World!'))),
    );
  }
}
