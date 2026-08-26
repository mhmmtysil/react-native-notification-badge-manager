require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-notification-badge-manager"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = { package["author"]["name"] => package["author"]["email"] }
  s.platforms    = { :ios => "15.1" }
  s.source       = { :git => "https://github.com/mhmmtysil/react-native-notification-badge-manager.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.resource_bundles = {
    "NotificationBadgeManagerPrivacyInfo" => ["ios/PrivacyInfo.xcprivacy"]
  }
  s.frameworks   = "UIKit", "UserNotifications"
  s.module_name  = "NotificationBadgeManager"
  s.swift_version = "5.0"
  s.dependency "React-Core"
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES"
  }
end
