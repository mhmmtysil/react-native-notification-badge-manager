module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.reactnativebadgemanager.BadgeManagerPackage;',
        packageInstance: 'new BadgeManagerPackage()',
      },
    },
  },
};
