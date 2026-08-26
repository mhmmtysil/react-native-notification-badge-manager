require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-badge-manager"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = "https://github.com/argedan/react-native-badge-manager"
  s.license      = package["license"]
  s.authors      = package["author"]
  s.platforms    = { :ios => "15.5" }
  s.source       = { :git => "https://github.com/argedan/react-native-badge-manager.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.frameworks   = "UIKit", "UserNotifications"
  s.swift_version = "5.0"
  s.dependency "React-Core"
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES"
  }
end
