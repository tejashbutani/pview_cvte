import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool androidCanvasVisible = false;

  MethodChannel? androidViewChannel;
  static const platformMethodChannel = MethodChannel('com.example.flutter_android_activity');

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          print("Received Strokes from Channel");

          final strokeData = Map<String, dynamic>.from(call.arguments);
          Stroke stroke = Stroke.fromJson(strokeData);
          print("Received Stroke: ${stroke.points.length}");
        } catch (e) {
          print('Error processing stroke data: $e');
        }
        break;
    }
  }

  void _launchAndroidActivity() async {
    try {
      await platformMethodChannel.invokeMethod('launchActivity');
      print("Launched MinimalAcceleratedActivity");
    } catch (e) {
      print('Error launching activity: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: Scaffold(
        body: Stack(
          children: [
            Container(
              color: Colors.green.shade200,
            ),
            Visibility(
              maintainState: false,
              visible: androidCanvasVisible,
              child: AndroidView(
                viewType: 'custom_canvas_view',
                creationParams: {
                  'color': Colors.black.value,
                  'width': 10,
                },
                creationParamsCodec: const StandardMessageCodec(),
                onPlatformViewCreated: (int id) {
                  print("Trying to create Platform Channel");
                  androidViewChannel = MethodChannel('custom_canvas_view_$id');
                  androidViewChannel?.setMethodCallHandler(_handleMethodCall);
                },
              ),
            ),
          ],
        ),
        floatingActionButton: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            FloatingActionButton(
              onPressed: _launchAndroidActivity,
              backgroundColor: Colors.blue,
              heroTag: "native_activity_fab",
              tooltip: 'Launch Native Activity',
              child: const Icon(Icons.android),
            ),
            const SizedBox(height: 16),
            FloatingActionButton(
              onPressed: () {
                setState(() {
                  print("Toggling Canvas Visibility from $androidCanvasVisible to ${!androidCanvasVisible}");
                  androidCanvasVisible = !androidCanvasVisible;
                });
              },
              backgroundColor: androidCanvasVisible ? Colors.green : Colors.red,
              heroTag: "canvas_toggle_fab",
              child: const Icon(Icons.edit),
            ),
          ],
        ),
      ),
    );
  }
}

class Stroke {
  List<Offset> points;
  final Color color;
  final double width;
  final bool isDashed;

  Stroke({
    required this.points,
    this.color = Colors.black,
    this.width = 5.0,
    this.isDashed = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'points': points.map((p) => {'x': p.dx, 'y': p.dy}).toList(),
      'color': color.value,
      'width': width,
      'isDashed': isDashed,
    };
  }

  factory Stroke.fromJson(Map<String, dynamic> json) {
    return Stroke(
      points: (json['points'] as List).map((p) => Offset(p['x'] as double, p['y'] as double)).toList(),
      color: Color(json['color'] as int),
      width: json['width'] as double,
      isDashed: json['isDashed'] as bool? ?? false,
    );
  }
}
