import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'great_cam_platform_interface.dart';

/// An implementation of [GreatCamPlatform] that uses method channels.
class MethodChannelGreatCam extends GreatCamPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('great_cam');

  @override
  Future<String?> startCamera() async {
    print('Call getPlatformVersion');
    final version = await methodChannel.invokeMethod<String?>('startCamera');
        print('Call getPlatformVersion. Version is: $version');

    return version;
  }
}
