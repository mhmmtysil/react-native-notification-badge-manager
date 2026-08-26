import Foundation
import React
import UIKit
import UserNotifications

@objc(NotificationBadgeManager)
final class NotificationBadgeManager: NSObject {
  private let lock = NSLock()
  private var lastBadge: Int?

  @objc static func requiresMainQueueSetup() -> Bool {
    false
  }

  @objc(setBadgeCount:resolver:rejecter:)
  func setBadgeCount(
    _ count: NSNumber,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyBadge(normalized(count.intValue), resolve: resolve, reject: reject)
  }

  @objc(getBadgeCount:rejecter:)
  func getBadgeCount(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async { [weak self] in
      let current = UIApplication.shared.applicationIconBadgeNumber
      self?.store(current)
      resolve(current)
    }
  }

  @objc(clearBadge:rejecter:)
  func clearBadge(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyBadge(0, resolve: resolve, reject: reject)
  }

  @objc(increment:resolver:rejecter:)
  func increment(
    _ by: NSNumber,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyDelta(normalized(by.intValue), resolve: resolve, reject: reject)
  }

  @objc(decrement:resolver:rejecter:)
  func decrement(
    _ by: NSNumber,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    applyDelta(-normalized(by.intValue), resolve: resolve, reject: reject)
  }

  @objc(requestPermission:rejecter:)
  func requestPermission(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    UNUserNotificationCenter.current().requestAuthorization(options: [.badge]) { granted, error in
      if let error {
        reject("permission_error", error.localizedDescription, error)
        return
      }
      resolve(granted)
    }
  }

  @objc(checkPermission:rejecter:)
  func checkPermission(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      switch settings.authorizationStatus {
      case .authorized, .provisional, .ephemeral:
        resolve("granted")
      case .denied:
        resolve("denied")
      case .notDetermined:
        resolve("undetermined")
      @unknown default:
        resolve("undetermined")
      }
    }
  }

  @objc(configure:resolver:rejecter:)
  func configure(
    _ options: NSDictionary,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    resolve(NSNull())
  }

  private func applyDelta(
    _ delta: Int,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      let current = self.currentBadge()
      self.applyBadge(max(0, current + delta), resolve: resolve, reject: reject)
    }
  }

  private func applyBadge(
    _ badge: Int,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async { [weak self] in
      if #available(iOS 16.0, *) {
        UNUserNotificationCenter.current().setBadgeCount(badge) { error in
          if let error {
            reject("badge_error", error.localizedDescription, error)
            return
          }
          self?.store(badge)
          resolve(badge)
        }
        return
      }

      UIApplication.shared.applicationIconBadgeNumber = badge
      self?.store(badge)
      resolve(badge)
    }
  }

  private func currentBadge() -> Int {
    lock.lock()
    let cached = lastBadge
    lock.unlock()
    return cached ?? UIApplication.shared.applicationIconBadgeNumber
  }

  private func store(_ value: Int) {
    lock.lock()
    lastBadge = value
    lock.unlock()
  }

  private func normalized(_ value: Int) -> Int {
    max(0, value)
  }
}
