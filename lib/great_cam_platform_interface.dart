import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'great_cam_method_channel.dart';

abstract class GreatCamPlatform extends PlatformInterface {
  /// Constructs a GreatCamPlatform.
  GreatCamPlatform() : super(token: _token);

  static final Object _token = Object();

  static GreatCamPlatform _instance = MethodChannelGreatCam();

  /// The default instance of [GreatCamPlatform] to use.
  ///
  /// Defaults to [MethodChannelGreatCam].
  static GreatCamPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [GreatCamPlatform] when
  /// they register themselves.
  static set instance(GreatCamPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    return _instance.getPlatformVersion();
  }

  Future<String?> startCamera() => _instance.startCamera();
}
