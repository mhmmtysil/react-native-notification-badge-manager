module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.notificationbadgemanager.NotificationBadgeManagerPackage;',
        packageInstance: 'new NotificationBadgeManagerPackage()',
      },
      ios: {
        podspecPath: './react-native-notification-badge-manager.podspec',
      },
    },
  },
};
