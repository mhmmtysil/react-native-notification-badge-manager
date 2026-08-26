#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BadgeManager, NSObject)

RCT_EXTERN_METHOD(setBadgeCount:(nonnull NSNumber *)count
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(clearBadge:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
