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
  final List<Stroke> _strokes = [];
  Rect? _viewFramePx; // Android view frame reported from native in physical px

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          print("Received Strokes from Channel");

          final payload = Map<String, dynamic>.from(call.arguments);
          final strokeData = Map<String, dynamic>.from(payload['stroke'] ?? payload);
          final viewInfoRaw = payload['view'];
          if (viewInfoRaw != null) {
            final viewInfo = Map<String, dynamic>.from(viewInfoRaw);
            final double vx = (viewInfo['x'] as num).toDouble();
            final double vy = (viewInfo['y'] as num).toDouble();
            final double vw = (viewInfo['width'] as num).toDouble();
            final double vh = (viewInfo['height'] as num).toDouble();
            print("View frame(px): x=$vx y=$vy w=$vw h=$vh");
            _viewFramePx = Rect.fromLTWH(vx, vy, vw, vh);
          }

          Stroke stroke = Stroke.fromJson(strokeData);
          print("Received Stroke: ${stroke.points.length}");
          if (mounted) {
            setState(() {
              _strokes.add(stroke);
            });
          }
        } catch (e) {
          print('Error processing stroke data: $e');
        }
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final dpr = MediaQuery.of(context).devicePixelRatio;
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
                viewType: 'accelerated_canvas_view',
                creationParams: {
                  'color': Colors.black.value,
                  'width': 10,
                },
                creationParamsCodec: const StandardMessageCodec(),
                onPlatformViewCreated: (int id) {
                  print("Accelerated canvas platform view created with id: $id");
                  // Note: Method channel communication can be added here if needed
                  // for stroke data communication between native and Flutter
                  androidViewChannel = MethodChannel('accelerated_canvas_view_$id');
                  androidViewChannel?.setMethodCallHandler(_handleMethodCall);
                },
              ),
            ),
            Positioned.fill(
              child: IgnorePointer(
                child: CustomPaint(
                  painter: StrokesPainter(
                    _strokes,
                    devicePixelRatio: dpr,
                    viewFramePx: _viewFramePx,
                  ),
                ),
              ),
            ),
          ],
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            setState(() {
              print("Toggling Canvas Visibility from $androidCanvasVisible to ${!androidCanvasVisible}");
              androidCanvasVisible = !androidCanvasVisible;
            });
          },
          backgroundColor: androidCanvasVisible ? Colors.green : Colors.red,
          child: const Icon(Icons.edit),
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

class StrokesPainter extends CustomPainter {
  final List<Stroke> strokes;
  final double devicePixelRatio;
  final Rect? viewFramePx; // physical px frame of the Android view (origin on screen)

  StrokesPainter(this.strokes, {required this.devicePixelRatio, this.viewFramePx});

  bool overlap = false;

  @override
  void paint(Canvas canvas, Size size) {
    // If native coordinates are in physical px relative to the Android View,
    // account for devicePixelRatio and translate by the view's position, if provided.
    final double scale = devicePixelRatio <= 0 ? 1.0 : (1.0 / devicePixelRatio);
    canvas.save();
    if (viewFramePx != null) {
      canvas.translate(viewFramePx!.left * scale, viewFramePx!.top * scale);
    }
    canvas.scale(scale, scale);

    for (final stroke in strokes) {
      if (stroke.points.isEmpty) continue;
      final paint = Paint()
        ..color = overlap ? stroke.color : Colors.red
        ..strokeWidth = overlap ? stroke.width : stroke.width + 2
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round;

      final path = Path()..moveTo(stroke.points.first.dx, stroke.points.first.dy);
      for (int i = 1; i < stroke.points.length; i++) {
        path.lineTo(stroke.points[i].dx, stroke.points[i].dy);
      }
      canvas.drawPath(path, paint);
    }

    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant StrokesPainter oldDelegate) {
    return oldDelegate.strokes != strokes;
  }
}
