# تعليمات بناء التطبيق

## المتطلبات
- Android Studio أو JDK 17
- Android SDK

## خطوات البناء المحلي

### 1. بناء نسخة Debug (للتجربة)
```bash
# في مجلد المشروع
./gradlew assembleDebug
```

الملف سيكون في:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. بناء نسخة Release (للنشر)
```bash
./gradlew assembleRelease
```

الملف سيكون في:
```
app/build/outputs/apk/release/app-release.apk
```

### 3. بناء نسخة DebugInsecure (للتطوير)
```bash
./gradlew assembleDebugInsecure
```

الملف سيكون في:
```
app/build/outputs/apk/debugInsecure/app-debugInsecure.apk
```

## ملاحظات مهمة

### للبناء المحلي على Windows:
استخدم `gradlew.bat` بدلاً من `./gradlew`:
```cmd
gradlew.bat assembleDebug
```

### إذا واجهت مشكلة في الأذونات:
```bash
chmod +x gradlew
```

### لتنظيف البناء السابق:
```bash
./gradlew clean
```

## الحصول على APK من GitHub Actions

1. اذهب إلى: https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway
2. اضغط على تبويب "Actions"
3. اختر "Manual Build APK"
4. اضغط "Run workflow"
5. اختر نوع البناء والإصدار
6. انتظر حتى ينتهي البناء
7. حمل الملف من قسم "Artifacts"

## أنواع البناء المتاحة

- **debug**: نسخة تجريبية مع debugging
- **debugInsecure**: نسخة تجريبية بدون قيود الشبكة
- **insecure**: نسخة موقعة بدون قيود الشبكة
- **release**: النسخة النهائية للنشر (تحتاج signing key)

## حجم الملف المتوقع
- Debug: ~15-20 MB
- Release: ~10-15 MB (بعد التحسين)
