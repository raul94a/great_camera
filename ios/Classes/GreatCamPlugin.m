#import "GreatCamPlugin.h"
#if __has_include(<great_cam/great_cam-Swift.h>)
#import <great_cam/great_cam-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "great_cam-Swift.h"
#endif

@implementation GreatCamPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftGreatCamPlugin registerWithRegistrar:registrar];
}
@end
