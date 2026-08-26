# react-native-notification-badge-manager

[English](README.md) | [Türkçe](README.tr.md)

![React Native Notification Badge Manager](./docs/banner.png)

React Native’de iOS ve Android **uygulama ikonu bildirim rozetini** ayarlayın, okuyun ve temizleyin.

**React Native 0.73 veya üzeri** gerekir. Autolinking, TypeScript, Expo development build ve hem eski hem yeni React Native mimarisi ile çalışır (eski native modül + New Architecture interop).

[![npm version](https://img.shields.io/npm/v/react-native-notification-badge-manager.svg)](https://www.npmjs.com/package/react-native-notification-badge-manager)

## Özellikler

- `setCount` / `getCount` / `clear`
- `increment` / `decrement` (`0`’ın altına inmez)
- iOS rozet izni (`UserNotifications`)
- Android 13+ `POST_NOTIFICATIONS` izni
- `useNotificationBadge()` React hook’u
- Samsung, Xiaomi, Huawei, OPPO, vivo ve diğer OEM launcher’larda ikon rozeti (bildirim tepsisi yok)
- Pixel/AOSP için isteğe bağlı notification-dot: `configure({ android: { useNotification: true } })`
- OEM launcher rozetleri: Samsung, Huawei, Honor, Xiaomi, OPPO, vivo, Sony, HTC, Asus, Nova, Apex, ZTE
- Android bildirim kanalı başlık/gövde ayarı
- Apple privacy manifest

## Kurulum

```bash
npm install react-native-notification-badge-manager
```

veya

```bash
yarn add react-native-notification-badge-manager
```

### iOS

```bash
cd ios && pod install && cd ..
```

Kurulumdan sonra native uygulamayı yeniden derleyin.

### Android

İzinler kütüphane manifest’inden otomatik birleşir. Android 13+ (API 33) için çalışma anında `POST_NOTIFICATIONS` istemeniz gerekir — `requestPermission()` kullanın.

### Expo

Expo Go **desteklenmez**. Expo SDK **50** veya üzeri gerekir (React Native 0.73+). [Development build](https://docs.expo.dev/develop/development-builds/introduction/) veya prebuild kullanın:

```json
{
  "expo": {
    "plugins": ["react-native-notification-badge-manager"]
  }
}
```

```bash
npx expo prebuild
npx expo run:ios
# veya
npx expo run:android
```

## Kullanım

```ts
import NotificationBadgeManager from 'react-native-notification-badge-manager';

await NotificationBadgeManager.requestPermission();
await NotificationBadgeManager.setCount(5);

const count = await NotificationBadgeManager.getCount();

await NotificationBadgeManager.increment();
await NotificationBadgeManager.decrement(2);
await NotificationBadgeManager.clear();
```

### React hook

```tsx
import { useNotificationBadge } from 'react-native-notification-badge-manager';

function UnreadBadgeButton() {
  const { count, increment, clear } = useNotificationBadge();

  return (
    <>
      <Button title={`Okunmamış: ${count}`} onPress={() => increment()} />
      <Button title="Temizle" onPress={() => clear()} />
    </>
  );
}
```

### Android seçenekleri

Varsayılan davranış iOS ile aynı: **yalnızca uygulama ikonu rozeti**, bildirim tepsisinde öğe yok. Samsung, Xiaomi, Huawei, OPPO, vivo gibi üretici launcher API’leri kullanılır.

Stok Android (Pixel / AOSP) bildirim olmadan ikonda sayı göstermez; böyle bir genel API yok. Orada nokta istiyorsanız açıkça açın:

```ts
await NotificationBadgeManager.configure({
  android: {
    useNotification: true,
    channelName: 'Uygulama ikonu rozeti',
    contentTitle: 'Okunmamış',
    contentText: '%count% okunmamış öğeniz var',
  },
});
```

Bildirim tepsisinde hiçbir şey görünmesini istemiyorsanız `useNotification` vermeyin (veya `false` bırakın).

### Push bildirimleriyle

Okunmamış sayıyı öğrendiğinizde bu kütüphaneyi çağırın — örneğin bir FCM/APNs mesajından sonra veya gelen kutusu açıldığında:

```ts
messaging().onMessage(async (message) => {
  const unread = Number(message.data?.unreadCount ?? 0);
  await NotificationBadgeManager.setCount(unread);
});
```

iOS’ta APNs gövdesi `"badge": 3` ile rozeti de ayarlayabilir. Bu paket aynı ikon rozetinin **uygulama içinden kontrolü** içindir.

## API

| Metot | Dönüş | Açıklama |
| --- | --- | --- |
| `setCount(count)` | `Promise<number>` | Rozeti ayarlar. Negatif / sonlu olmayan değerler `0` olur. |
| `getCount()` | `Promise<number>` | Güncel rozeti okur. |
| `clear()` | `Promise<void>` | Rozeti `0` yapar. |
| `increment(by?)` | `Promise<number>` | `by` kadar artırır (varsayılan `1`). |
| `decrement(by?)` | `Promise<number>` | `by` kadar azaltır (varsayılan `1`), en az `0`. |
| `requestPermission()` | `Promise<boolean>` | iOS rozet izni / Android 13+ bildirim izni. |
| `checkPermission()` | `Promise<'granted' \| 'denied' \| 'undetermined'>` | İzni sormaz, mevcut durumu döner. |
| `configure(options)` | `Promise<void>` | Android bildirim / kanal ayarları. |
| `isSupported()` | `boolean` | iOS ve Android’de `true`. |
| `setBadgeCount(count)` | `Promise<number>` | Kullanımdan kalkıyor; `setCount` kullanın. |
| `clearBadge()` | `Promise<void>` | Kullanımdan kalkıyor; `clear` kullanın. |

İsimli export `BadgeManager`, `NotificationBadgeManager` ile aynı nesnedir.

## Platform notları

### iOS

- iOS 16+: `UNUserNotificationCenter.setBadgeCount`
- Daha eski iOS: `UIApplication.shared.applicationIconBadgeNumber`
- Üretimde rozete güvenmeden önce izin isteyin
- Minimum iOS sürümü: **15.1**

### Android

Launcher rozetleri tek bir genel Android API’si **değildir**. Bu kütüphane:

1. **İkon rozet sayısını** üretici launcher API’leriyle yazar (Samsung, Huawei, Honor, Xiaomi, OPPO, vivo, Sony, …) — bildirim tepsisi yok
2. Pixel / AOSP noktası için yalnızca `useNotification: true` derseniz bildirim gönderir

Kurulum veya güncellemeden sonra native uygulamayı yeniden derleyin (`npx react-native run-android` veya `npx expo run:android`).

Bazı launcher’lar sistem ayarlarında rozet açılmadan sayıyı göstermez. Özellikle Xiaomi/HyperOS uygulamaya rozet izni ister. Pixel, bildirim olmadan iOS tarzı sayı gösteremez; bu bir işletim sistemi sınırıdır.

Android’de `getCount()` sistemden okumaz; bu kütüphanenin en son yazdığı değeri döner (`SharedPreferences`).

## Gereksinimler

| | Destek |
| --- | --- |
| React Native | **0.73 ve üzeri** (üst sınır yok) |
| Mimari | Old Bridge ve New Architecture (interop) |
| iOS | **15.1+** |
| Android | **API 24+** (Android 7.0) |
| Expo | SDK **50+** development / production build |
| Expo Go | Desteklenmez |

React Native **0.72 ve altı** desteklenmez.

React Native 0.76+ sürümlerinde New Architecture varsayılandır. Bu paket native modül köprüsünü kullanır; 0.76+ üzerinde interop katmanı ile çalışır.

Expo SDK 50, React Native 0.73’e karşılık gelir. Daha yeni Expo SDK’ları (51, 52, 53, …) React Native 0.73 veya üzeri taşıdıkları sürece desteklenir.

## Lisans

MIT © Muhammet Yeşil
