
import 'great_cam_platform_interface.dart';

class GreatCam {



  Future<String?> startCamera(){
    return GreatCamPlatform.instance.startCamera();
  }

  
}
