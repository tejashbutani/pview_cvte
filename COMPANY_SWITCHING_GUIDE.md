# Company-Specific Drawing Surface Implementation Guide

This Flutter whiteboarding tool now supports switching between two different company-specific drawing surface implementations:

## 1. Standard DrawSurfaceView (com.x.DrawSurfaceView)
- Basic SurfaceView implementation with custom drawing
- Suitable for Company A's requirements
- Uses traditional Android Canvas drawing

## 2. MinimalAcceleratedSurfaceView (com.x.MinimalAcceleratedSurfaceView)  
- Advanced implementation using MinimalAcceleratedActivity architecture
- Suitable for Company B's requirements
- Includes hardware acceleration and advanced rendering features
- Integrates with StateHolder and RenderAcceleratorManager

## Usage

### Flutter Side Configuration

In your Flutter code (`lib/main.dart`), set the `useAccelerated` parameter:

```dart
AndroidView(
  viewType: 'custom_canvas_view',
  creationParams: {
    'color': Colors.black.value,
    'width': 10,
    'useAccelerated': false, // false = DrawSurfaceView (Company A)
                            // true  = MinimalAcceleratedSurfaceView (Company B)
  },
  // ... rest of configuration
)
```

### Company A (Standard Implementation)
```dart
creationParams: {
  'color': Colors.black.value,
  'width': 10,
  'useAccelerated': false, // Use DrawSurfaceView
},
```

### Company B (Accelerated Implementation)
```dart
creationParams: {
  'color': Colors.black.value,
  'width': 10,
  'useAccelerated': true, // Use MinimalAcceleratedSurfaceView
},
```

## Architecture Details

### Standard DrawSurfaceView Features:
- Basic multi-touch drawing
- Color and width customization
- Dashed line support
- Clear functionality
- Stroke data extraction for Flutter

### MinimalAcceleratedSurfaceView Features:
- All DrawSurfaceView features
- Hardware acceleration via RenderAcceleratorManager
- StateHolder integration for advanced state management
- Automatic lifecycle management (onResume/onPause)
- Enhanced rendering performance
- Company B's proprietary acceleration features

## Method Channels

Both implementations support the same method channel interface:

- `updatePenColor(color: int)`
- `updatePenWidth(width: double)`
- `setDashed(dashed: boolean)`
- `clear()`
- `resume()` (MinimalAcceleratedSurfaceView only)
- `pause()` (MinimalAcceleratedSurfaceView only)

## Files Created/Modified

### New Files:
- `android/app/src/main/java/com/x/IDrawer.java` - Drawing tool interface
- `android/app/src/main/java/com/x/Pencil.java` - Pencil tool implementation
- `android/app/src/main/java/com/x/Util.java` - Utility class for screen dimensions
- `android/app/src/main/java/com/x/MinimalAcceleratedSurfaceView.java` - Company B's accelerated surface

### Modified Files:
- `android/app/src/main/kotlin/play/app/CustomPlatformView.kt` - Added company switching logic
- `lib/main.dart` - Added useAccelerated parameter example

## Switching Between Companies

To switch between company implementations, simply change the `useAccelerated` parameter in your Flutter code. No other changes are required as the interface remains consistent between both implementations.
