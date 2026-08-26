import Foundation
import React
import UIKit
import UserNotifications

@objc(BadgeManager)
final class BadgeManager: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool {
    false
  }

  @objc(setBadgeCount:resolver:rejecter:)
  func setBadgeCount(
    _ count: NSNumber,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyBadge(max(0, count.intValue), resolve: resolve, reject: reject)
  }

  @objc(clearBadge:rejecter:)
  func clearBadge(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyBadge(0, resolve: resolve, reject: reject)
  }

  private func applyBadge(
    _ badge: Int,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async {
      if #available(iOS 16.0, *) {
        UNUserNotificationCenter.current().setBadgeCount(badge) { error in
          if let error {
            reject("badge_error", error.localizedDescription, error)
            return
          }
          resolve(NSNull())
        }
        return
      }

      UIApplication.shared.applicationIconBadgeNumber = badge
      resolve(NSNull())
    }
  }
}
