# 🎉 بوابة الرسائل النصية العربية - الإصدار النهائي v2.0.0

[![العربية](https://img.shields.io/badge/اللغة-العربية-green.svg)](README_AR.md)
[![الإصدار](https://img.shields.io/badge/الإصدار-v2.0.0--final-blue.svg)](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/releases/tag/v2.0.0-final)
[![الحالة](https://img.shields.io/badge/الحالة-جاهز%20للإنتاج-brightgreen.svg)](#)
[![الترخيص](https://img.shields.io/badge/الترخيص-MIT-yellow.svg)](LICENSE)

## 🌟 **مشروع متكامل ومستقل للرسائل النصية مع دعم المدفوعات الإلكترونية**

تطبيق Android متقدم يوفر بوابة شاملة لإدارة الرسائل النصية مع دعم كامل للمحافظ الإلكترونية المصرية وواجهة عربية متطورة.

---

## ✨ **الميزات الرئيسية**

### 🇪🇬 **التعريب الكامل**
- واجهة مستخدم عربية 100% مع دعم RTL
- رسائل خطأ وإشعارات باللغة العربية
- تجربة مستخدم محسنة للمستخدمين العرب

### 💳 **نظام الدفع الإلكتروني المتقدم**
- **7 محافظ إلكترونية مصرية مدعومة**:
  - InstaPay (فوري) ✅
  - Vodafone Cash (فودافون كاش) ✅
  - Orange Cash (أورانج كاش) ✅
  - Etisalat Cash (اتصالات كاش) ✅
  - Fawry (فوري) ✅
  - CIB (البنك التجاري الدولي) ✅
  - NBE (البنك الأهلي المصري) ✅

- **ميزات متقدمة**:
  - كشف تلقائي للمدفوعات من الرسائل النصية
  - نظام webhooks للمعالجة الفورية
  - إحصائيات مفصلة للمدفوعات
  - تأكيد المدفوعات التلقائي/اليدوي

### 🌐 **حل مشاكل الاتصال بالخادم السحابي**
- **أوضاع خادم متعددة**:
  - محلي فقط (موصى به) - لا يحتاج اتصال إنترنت
  - سحابي فقط - للاستخدام مع الخادم السحابي
  - تلقائي - يتكيف حسب حالة الاتصال

- **معالجة أخطاء ذكية**:
  - رسائل خطأ واضحة باللغة العربية
  - تشخيص تلقائي لمشاكل الشبكة
  - إعادة محاولة ذكية
  - نظام سجلات مفصل

### ⚙️ **واجهة إعدادات متقدمة**
- إعدادات سهلة للمستخدم العادي
- خيارات متقدمة للمطورين
- اختبار اتصال تفاعلي
- عرض حالة الاتصال الحالية
- إحصائيات الأداء

---

## 🚀 **التثبيت والاستخدام**

### متطلبات النظام:
- Android 7.0+ (API 24)
- 50 MB مساحة تخزين
- إذن الرسائل النصية
- اتصال إنترنت (اختياري للوضع المحلي)

### خطوات التثبيت:

1. **تحميل المشروع**:
```bash
git clone https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway.git
cd arabic-sms-payment-gateway
```

2. **تحديث Firebase** (للاستخدام مع الخادم السحابي):
```bash
# استبدل app/google-services.json بملف Firebase الخاص بك
cp your-google-services.json app/google-services.json
```

3. **البناء والتثبيت**:
```bash
./gradlew assembleDebug
# أو للإصدار النهائي
./gradlew assembleRelease
```

### الاستخدام السريع:

1. **تثبيت التطبيق** على الجهاز
2. **منح الأذونات** المطلوبة (الرسائل النصية)
3. **اختيار وضع الخادم** (محلي موصى به للبداية)
4. **تفعيل كشف المدفوعات** في الإعدادات
5. **البدء في الاستخدام** فوراً!

---

## 📱 **لقطات الشاشة**

| الشاشة الرئيسية | إعدادات الدفع | إعدادات الخادم |
|-----------------|---------------|----------------|
| ![الرئيسية](docs/screenshots/home.png) | ![الدفع](docs/screenshots/payment.png) | ![الخادم](docs/screenshots/server.png) |

---

## 🔧 **التكوين المتقدم**

### إعدادات الخادم:
```kotlin
// في الإعدادات
gatewaySettings.serverMode = GatewaySettings.ServerMode.LOCAL_ONLY  // محلي فقط
gatewaySettings.serverMode = GatewaySettings.ServerMode.CLOUD_ONLY  // سحابي فقط
gatewaySettings.serverMode = GatewaySettings.ServerMode.AUTO        // تلقائي
```

### إعدادات المدفوعات:
```kotlin
// تفعيل المحافظ المطلوبة
paymentSettings.enabledWalletTypes = setOf(
    PaymentWalletType.INSTAPAY,
    PaymentWalletType.VODAFONE_CASH,
    PaymentWalletType.ORANGE_CASH
)
```

### Webhooks للمدفوعات:
```kotlin
// إعداد webhook URL
paymentSettings.paymentWebhookUrl = "https://your-server.com/webhook"
```

---

## 🛠️ **التطوير**

### البنية التقنية:
- **Kotlin** - لغة البرمجة الأساسية
- **Koin** - حقن التبعيات
- **Room** - قاعدة البيانات المحلية
- **Ktor** - الخادم المحلي وعميل HTTP
- **Coroutines** - البرمجة غير المتزامنة
- **WorkManager** - المهام في الخلفية

### هيكل المشروع:
```
app/src/main/java/me/capcom/smsgateway/
├── modules/
│   ├── gateway/          # إدارة الاتصال بالخادم
│   ├── payment/          # نظام المدفوعات الإلكترونية
│   ├── messages/         # إدارة الرسائل النصية
│   ├── localserver/      # الخادم المحلي
│   └── ...
├── ui/                   # واجهات المستخدم
├── data/                 # طبقة البيانات
└── services/             # الخدمات الأساسية
```

### إضافة محفظة إلكترونية جديدة:
```kotlin
// 1. إضافة نوع المحفظة
enum class PaymentWalletType {
    // المحافظ الموجودة...
    NEW_WALLET
}

// 2. إضافة نمط الكشف
class PaymentParser {
    private fun parseNewWallet(message: String): PaymentInfo? {
        // منطق كشف المدفوعات
    }
}
```

---

## 📚 **الوثائق**

### الأدلة المتوفرة:
- [دليل المستخدم العربي](user-docs) - للمستخدمين العاديين
- [دليل المطور](dev-docs) - للمطورين والتكامل
- [دليل API](API_INTEGRATION.md) - لتكامل الواجهات البرمجية
- [دليل استكشاف الأخطاء](CLOUD_SERVER_TROUBLESHOOTING.md) - لحل المشاكل

### ملفات التوثيق:
- `CONVERSATION_SUMMARY.md` - ملخص تطوير المشروع
- `PROJECT_COMPLETION_SUMMARY.md` - ملخص الإنجازات
- `FINAL_PROJECT_COMPLETION.md` - التقرير النهائي
- `CLOUD_CONNECTION_ANALYSIS.md` - تحليل مشاكل الاتصال

---

## 🤝 **المساهمة**

نرحب بالمساهمات! يرجى اتباع الخطوات التالية:

1. Fork المشروع
2. إنشاء branch جديد (`git checkout -b feature/amazing-feature`)
3. Commit التغييرات (`git commit -m 'Add amazing feature'`)
4. Push إلى Branch (`git push origin feature/amazing-feature`)
5. فتح Pull Request

### إرشادات المساهمة:
- استخدم اللغة العربية في التعليقات والوثائق
- اتبع معايير Kotlin الرسمية
- أضف اختبارات للميزات الجديدة
- حدث الوثائق عند الحاجة

---

## 🐛 **الإبلاغ عن الأخطاء**

إذا واجهت أي مشاكل، يرجى:

1. التحقق من [الأسئلة الشائعة](docs/FAQ.md)
2. البحث في [Issues الموجودة](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/issues)
3. إنشاء [Issue جديد](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/issues/new) مع:
   - وصف المشكلة
   - خطوات إعادة الإنتاج
   - معلومات الجهاز والنظام
   - لقطات شاشة إن أمكن

---

## 📄 **الترخيص**

هذا المشروع مرخص تحت رخصة MIT - راجع ملف [LICENSE](LICENSE) للتفاصيل.

---

## 🙏 **الشكر والتقدير**

- **المشروع الأصلي**: [android-sms-gateway](https://github.com/android-sms-gateway/android-sms-gateway) للإلهام والأساس التقني
- **المجتمع العربي** للمطورين لدعم التعريب
- **البنوك والمحافظ المصرية** لتوفير الخدمات الإلكترونية

---

## 📞 **التواصل**

- **GitHub Issues**: [طرح الأسئلة والمشاكل](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/issues)
- **Discussions**: [المناقشات العامة](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/discussions)

---

## 🎯 **خارطة الطريق**

### الإصدارات القادمة:
- [ ] v2.1.0 - دعم محافظ إضافية (Banque Misr, QNB)
- [ ] v2.2.0 - واجهة ويب للإدارة
- [ ] v2.3.0 - تحليلات متقدمة بالذكاء الاصطناعي
- [ ] v3.0.0 - دعم العملات المشفرة

### الميزات المخططة:
- تكامل مع أنظمة المحاسبة
- تطبيق سطح المكتب
- دعم المزيد من البلدان العربية
- API متقدم للمؤسسات

---

<div align="center">

**🎉 مشروع مكتمل وجاهز للاستخدام الفوري! 🎉**

[![تحميل الإصدار الأحدث](https://img.shields.io/badge/تحميل-الإصدار%20الأحدث-brightgreen.svg?style=for-the-badge)](https://github.com/drnopoh2810-spec/arabic-sms-payment-gateway/releases/latest)

**صنع بـ ❤️ للمجتمع العربي**

</div>