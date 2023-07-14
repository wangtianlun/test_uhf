import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'v1.0.1'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _result = '';
  static const platform = MethodChannel('aaa');

  @override
  void initState() {
    super.initState();
    platform.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    if (call.method == 'getBarCode') {
      dynamic message = call.arguments;
      setState(() {
        _result = message;
      });
    }
  }

  Future<void> _getSyncRFID() async {
    try {
      String rfid = await platform.invokeMethod('getSyncRFID');
      print('rfid: $rfid');
      setState(() {
        _result = rfid;
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to get rfid: '${e.message}'.";
      });
    }
  }

  Future<void> _getPower() async {
    try {
      String power = await platform.invokeMethod('getPower');
      print('power: $power');
      setState(() {
        _result = power;
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to get power: '${e.message}'.";
      });
    }
  }

  Future<void> _setPower() async {
    try {
      bool isSuccess = await platform.invokeMethod('setPower');
      setState(() {
        _result = isSuccess ? 'success' : 'fail';
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to set power: '${e.message}'.";
      });
    }
  }

  Future<void> _getHardware() async {
    try {
      String hardware = await platform.invokeMethod('getHardware');
      setState(() {
        _result = hardware;
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to get hardware: '${e.message}'.";
      });
    }
  }

  Future<void> _close() async {
    try {
      bool isSuccess = await platform.invokeMethod('close');
      setState(() {
        _result = isSuccess ? 'close success' : 'close fail';
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to close: '${e.message}'.";
      });
    }
  }

  Future<void> _startScan() async {
    try {
      await platform.invokeMethod('startScan');
      setState(() {
        _result = 'start scan success';
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to start scan: '${e.message}'.";
      });
    }
  }

  Future<void> _stopScan() async {
    try {
      await platform.invokeMethod('stopScan');
      setState(() {
        _result = 'stop scan success';
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to stop scan: '${e.message}'.";
      });
    }
  }

  Future<void> _getBarCode() async {
    try {
      String code = await platform.invokeMethod('getBarCode');
      setState(() {
        _result = code;
      });
    } on PlatformException catch (e) {
      setState(() {
        _result = "Failed to get bar code: '${e.message}'.";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            ElevatedButton(
              onPressed: _getHardware,
              child: const Text('get hardware'),
            ),
            ElevatedButton(
              onPressed: _getSyncRFID,
              child: const Text('get rfid'),
            ),
            ElevatedButton(
              onPressed: _getPower,
              child: const Text('get power'),
            ),
            ElevatedButton(
              onPressed: _setPower,
              child: const Text('set power'),
            ),
            ElevatedButton(
              onPressed: _close,
              child: const Text('close'),
            ),
            ElevatedButton(
              onPressed: _startScan,
              child: const Text('start scan'),
            ),
            ElevatedButton(
              onPressed: _stopScan,
              child: const Text('stop scan'),
            ),
            ElevatedButton(
              onPressed: _getBarCode,
              child: const Text('get barcode'),
            ),
            Text(_result),
          ],
        ),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
