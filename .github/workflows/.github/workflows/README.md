# MyVPN — هيكل تطبيق Android (VLESS + WebSocket + TLS)

## 1. ما الذي تحتاجه
- Android Studio (أحدث إصدار)
- JDK 17
- سيرفر خاص بك يعمل بـ Xray/V2Ray (inbound: VLESS، transport: WebSocket، security: TLS)

## 2. الإعداد
1. افتح المشروع في Android Studio.
2. عدّل `applicationId` في `app/build.gradle.kts` إلى معرّف تطبيقك.
3. عدّل `app/src/main/assets/servers.json` وضع بيانات سيرفراتك:
   - `address`: الدومين أو الـ IP
   - `port`: عادة 443
   - `uuid`: معرّف المستخدم في سيرفر Xray
   - `wsPath`: مسار WebSocket كما هو في إعداد السيرفر
   - `sni` و`host`: الدومين الخاص بالسيرفر
4. تحقق من إصدار المكتبة `AndroidLibXrayLite` في `app/build.gradle.kts` من صفحة مشروعها على GitHub، وتأكد أن أسماء الواجهات (`Libv2ray.newCoreController`، `startLoop`، `stopLoop`، `CoreCallbackHandler`) مطابقة للإصدار الذي اخترته، لأنها تتغير بين الإصدارات.

## 3. البناء
- من القائمة: Build > Generate Signed App Bundle / APK
- أو من الطرفية: `./gradlew assembleRelease`

## 4. ملاحظات مهمة قبل النشر
- **التوقيع:** أنشئ مفتاح توقيع (keystore) واحفظه في مكان آمن؛ فقدانه يمنعك من تحديث التطبيق لاحقاً.
- **الخصوصية:** Google Play يطلب سياسة خصوصية ووصفاً واضحاً لاستخدام VpnService.
- **الإعلانات:** لم أُضف AdMob بعد. عند الحاجة، أضف معرّفات تطبيقك من لوحة AdMob الخاصة بك فقط.
- **السيرفرات:** تأكد أنك تملك السيرفرات أو لديك حق استخدامها.
