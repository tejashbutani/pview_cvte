import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/rendering.dart';

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
  bool textureViewVisible = false;

  MethodChannel? androidViewChannel;
  MethodChannel? textureViewChannel;
  static const platformMethodChannel = MethodChannel('com.example.flutter_android_activity');

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          print("Received Strokes from Channel");

          final strokeData = Map<String, dynamic>.from(call.arguments);
          Stroke stroke = Stroke.fromJson(strokeData);
          print("Received Stroke: ${stroke.points.length} points, color: ${stroke.color}, width: ${stroke.width}");

          // Here you can add the stroke to your Flutter canvas or process it further
          // For example, add to a list of strokes to be drawn on Flutter canvas
        } catch (e) {
          print('Error processing stroke data: $e');
        }
        break;
    }
  }

  Future<dynamic> _handleTextureViewMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          print("Received Stroke from TextureView");

          final strokeData = Map<String, dynamic>.from(call.arguments);
          Stroke stroke = Stroke.fromJson(strokeData);
          print("TextureView Stroke: ${stroke.points.length} points, color: ${stroke.color}, width: ${stroke.width}");

          // Process stroke data from TextureView
          // This is where you'd implement the double canvas approach
        } catch (e) {
          print('Error processing TextureView stroke data: $e');
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
            Visibility(
              maintainState: false,
              visible: textureViewVisible,
              child: const CustomTextureView(),
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
            const SizedBox(height: 16),
            FloatingActionButton(
              onPressed: () {
                setState(() {
                  print("Toggling TextureView Visibility from $textureViewVisible to ${!textureViewVisible}");
                  textureViewVisible = !textureViewVisible;
                });
              },
              backgroundColor: textureViewVisible ? Colors.green : Colors.red,
              heroTag: "texture_view_toggle_fab",
              tooltip: 'Toggle TextureView',
              child: const Icon(Icons.texture),
            ),
          ],
        ),
      ),
    );
  }
}

class CustomTextureView extends StatefulWidget {
  const CustomTextureView({super.key});

  @override
  State<CustomTextureView> createState() => _CustomTextureViewState();
}

class _CustomTextureViewState extends State<CustomTextureView> {
  MethodChannel? textureViewChannel;

  Future<dynamic> _handleTextureViewMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          print("Received Stroke from TextureView");

          final strokeData = Map<String, dynamic>.from(call.arguments);
          Stroke stroke = Stroke.fromJson(strokeData);
          print("TextureView Stroke: ${stroke.points.length} points, color: ${stroke.color}, width: ${stroke.width}");

          // Process stroke data from TextureView
          // This is where you'd implement the double canvas approach
          // You can add the stroke to a Flutter canvas, save it, or process it further
        } catch (e) {
          print('Error processing TextureView stroke data: $e');
        }
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    const String viewType = 'custom_texture_view';
    final Map<String, dynamic> creationParams = <String, dynamic>{
      'color': Colors.black.value,
      'width': 5.0,
    };

    return PlatformViewLink(
      viewType: viewType,
      surfaceFactory: (context, controller) {
        return AndroidViewSurface(
          controller: controller as AndroidViewController,
          gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
          hitTestBehavior: PlatformViewHitTestBehavior.opaque,
        );
      },
      onCreatePlatformView: (params) {
        return PlatformViewsService.initSurfaceAndroidView(
          id: params.id,
          viewType: viewType,
          layoutDirection: TextDirection.ltr,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
          onFocus: () {
            params.onFocusChanged(true);
          },
        )
          ..addOnPlatformViewCreatedListener((int id) {
            print("TextureView PlatformView created with ID: $id");
            textureViewChannel = MethodChannel('custom_texture_view_$id');
            textureViewChannel?.setMethodCallHandler(_handleTextureViewMethodCall);
            params.onPlatformViewCreated(id);
          })
          ..create();
      },
    );
  }

  @override
  void dispose() {
    textureViewChannel?.setMethodCallHandler(null);
    super.dispose();
  }

  // Helper methods to control the TextureView
  Future<void> setStrokeWidth(double width) async {
    try {
      await textureViewChannel?.invokeMethod('setStrokeWidth', {'width': width});
    } catch (e) {
      print('Error setting stroke width: $e');
    }
  }

  Future<void> setPenColor(Color color) async {
    try {
      await textureViewChannel?.invokeMethod('setPenColor', {'color': color.value});
    } catch (e) {
      print('Error setting pen color: $e');
    }
  }

  Future<void> clearStrokes() async {
    try {
      await textureViewChannel?.invokeMethod('clearStrokes');
    } catch (e) {
      print('Error clearing strokes: $e');
    }
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
