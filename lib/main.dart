import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:sentry_flutter/sentry_flutter.dart';

Future<void> main() async {
  await SentryFlutter.init(
    (options) {
      options.dsn = '';
      // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
      // We recommend adjusting this value in production.
      options.tracesSampleRate = 1.0;
    },
    appRunner: () => runApp(const MyApp()),
  );

  FlutterError.onError = (details) {
    final dynamic exception = details.exception;
    final dynamic stackTrace = details.stack;

    // 发送异常到 Sentry
    Sentry.captureException(
      exception,
      stackTrace: stackTrace,
    );
  };

  // or define SENTRY_DSN via Dart environment variable (--dart-define)
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
  String _ticketId = 'Ticket ID from reader';
  String _wristbandId = 'Wristband ID from reader';
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
        _ticketId = message;
      });
    }
  }

  Future<void> _getSyncRFID() async {
    try {
      String rfid = await platform.invokeMethod('getSyncRFID');
      print('rfid: $rfid');
      setState(() {
        _wristbandId = rfid;
      });
    } on PlatformException catch (e) {
      setState(() {
        _wristbandId = "Failed to get rfid: '${e.message}'.";
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
      String msg = await platform.invokeMethod('startScan');
      setState(() {
        _ticketId = msg;
      });
    } on PlatformException catch (e) {
      setState(() {
        _ticketId = "Failed to start scan: '${e.message}'.";
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
      await platform.invokeMethod('getBarCode');
    } on PlatformException catch (e) {
      setState(() {
        _ticketId = "Failed to get bar code: '${e.message}'.";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // appBar: AppBar(
      //   backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      //   title: Text(widget.title),
      // ),
      body: Stack(
        children: [
          Center(
            child: Column(
              children: <Widget>[
                Container(
                  margin: EdgeInsets.only(
                    top: 80,
                  ),
                  child: Column(
                    children: [
                      Container(
                        child: const Text(
                          "Ticket",
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.only(top: 40, bottom: 20),
                        child: Text(_ticketId),
                      ),
                      ElevatedButton(onPressed: _startScan, child: Text('scan'))
                    ],
                  ),
                ),
                Container(
                  margin: EdgeInsets.only(
                    top: 160,
                  ),
                  child: Column(
                    children: [
                      Container(
                        child: const Text(
                          "Wristband",
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.only(
                          top: 40,
                          bottom: 20,
                        ),
                        child: Text(_wristbandId),
                      ),
                      ElevatedButton(
                          onPressed: _getSyncRFID, child: Text('get'))
                    ],
                  ),
                ),
                Text(_result),
              ],
            ),
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: GestureDetector(
              onTap: () {
                // Button click event
                print('Button Clicked');
              },
              child: Container(
                height: 80,
                color: Colors.blue,
                child: Center(
                  child: Text(
                    'Pair & Admit',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
