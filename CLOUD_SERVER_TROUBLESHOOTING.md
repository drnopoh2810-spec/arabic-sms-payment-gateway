# دليل استكشاف أخطاء الخادم السحابي - بوابة الرسائل النصية العربية

## 🔍 **تشخيص المشكلة**

### الأعراض الشائعة:
- فشل الاتصال بالخادم السحابي عند بدء التطبيق
- رسائل خطأ "Connection failed" أو "Server unreachable"
- عدم تلقي الرسائل من الخادم السحابي
- فشل تسجيل الجهاز

## 🛠️ **الحلول المطبقة**

### 1. **نظام أوضاع الخادم الجديد**
```kotlin
enum class ServerMode {
    LOCAL_ONLY,    // الوضع المحلي فقط - لا يحتاج اتصال إنترنت
    CLOUD_ONLY,    // الخادم السحابي فقط - يتطلب اتصال إنترنت
    AUTO           // تلقائي - محلي أولاً، ثم سحابي عند الحاجة
}
```

### 2. **معالجة أخطاء محسنة**
- رسائل خطأ باللغة العربية
- تشخيص تلقائي لمشاكل الشبكة
- سجلات مفصلة لتتبع المشاكل
- إعادة محاولة ذكية

### 3. **اختبار الاتصال التلقائي**
```kotlin
suspend fun testCloudConnection(): Boolean {
    return try {
        val response = api.getDevice(settings.registrationInfo?.token)
        logsService.insert(LogEntry.Priority.INFO, "نجح اختبار الاتصال")
        true
    } catch (e: Exception) {
        logsService.insert(LogEntry.Priority.ERROR, "فشل اختبار الاتصال: ${e.message}")
        false
    }
}
```

## 🔧 **خطوات الإصلاح**

### الخطوة 1: تفعيل الوضع المحلي
```kotlin
// في إعدادات التطبيق
gatewaySettings.serverMode = GatewaySettings.ServerMode.LOCAL_ONLY
```

### الخطوة 2: فحص السجلات
```bash
# عرض سجلات التطبيق
adb logcat | grep "GatewayService\|NetworkError\|Connection"
```

### الخطوة 3: اختبار الاتصال
```kotlin
// في كود التطبيق
val isConnected = gatewayService.testConnection()
if (!isConnected) {
    // التبديل إلى الوضع المحلي
    gatewaySettings.serverMode = ServerMode.LOCAL_ONLY
}
```

## 📱 **إعدادات المستخدم المقترحة**

### واجهة إعدادات الخادم:
```xml
<!-- في ملف الإعدادات -->
<PreferenceCategory android:title="@string/server_settings">
    
    <ListPreference
        android:key="server_mode"
        android:title="@string/server_mode_title"
        android:summary="@string/server_mode_summary"
        android:entries="@array/server_mode_entries"
        android:entryValues="@array/server_mode_values"
        android:defaultValue="LOCAL_ONLY" />
    
    <EditTextPreference
        android:key="cloud_server_url"
        android:title="@string/cloud_server_url"
        android:summary="@string/cloud_server_url_summary"
        android:defaultValue="https://api.sms-gate.app/mobile/v1"
        android:dependency="server_mode" />
    
    <Preference
        android:key="test_connection"
        android:title="@string/test_connection"
        android:summary="@string/test_connection_summary" />
        
</PreferenceCategory>
```

### النصوص العربية:
```xml
<!-- في strings-ar.xml -->
<string name="server_settings">إعدادات الخادم</string>
<string name="server_mode_title">وضع الخادم</string>
<string name="server_mode_summary">اختر كيفية الاتصال بالخادم</string>
<string name="test_connection">اختبار الاتصال</string>
<string name="test_connection_summary">اختبر الاتصال بالخادم السحابي</string>

<string-array name="server_mode_entries">
    <item>محلي فقط (موصى به)</item>
    <item>سحابي فقط</item>
    <item>تلقائي</item>
</string-array>

<string-array name="server_mode_values">
    <item>LOCAL_ONLY</item>
    <item>CLOUD_ONLY</item>
    <item>AUTO</item>
</string-array>
```

## 🚨 **المشاكل الشائعة والحلول**

### 1. **خطأ "Unknown Host"**
**السبب**: عدم توفر اتصال إنترنت أو DNS غير صحيح
**الحل**: 
```kotlin
if (!networkErrorHandler.isNetworkAvailable()) {
    // التبديل إلى الوضع المحلي
    settings.serverMode = ServerMode.LOCAL_ONLY
}
```

### 2. **خطأ "Connection Timeout"**
**السبب**: بطء الإنترنت أو الخادم غير متجاوب
**الحل**:
```kotlin
// زيادة مهلة الاتصال
val client = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000 // 30 ثانية
        connectTimeoutMillis = 15000 // 15 ثانية
    }
}
```

### 3. **خطأ "SSL Certificate"**
**السبب**: مشكلة في شهادة الأمان
**الحل**:
```kotlin
// تجاهل شهادات SSL في التطوير (غير آمن للإنتاج)
val client = HttpClient {
    install(HttpsRedirect) {
        checkHttpsUpgrade = false
    }
}
```

### 4. **خطأ "401 Unauthorized"**
**السبب**: انتهاء صلاحية الرمز المميز
**الحل**:
```kotlin
try {
    api.getDevice(token)
} catch (e: ClientRequestException) {
    if (e.response.status == HttpStatusCode.Unauthorized) {
        // إعادة تسجيل الجهاز
        registerDevice(pushToken, RegistrationMode.Anonymous)
    }
}
```

## 📊 **مراقبة الأداء**

### إحصائيات الاتصال:
```kotlin
class ConnectionStats {
    var successfulConnections = 0
    var failedConnections = 0
    var averageResponseTime = 0L
    var lastConnectionTime = 0L
    
    fun recordSuccess(responseTime: Long) {
        successfulConnections++
        averageResponseTime = (averageResponseTime + responseTime) / 2
        lastConnectionTime = System.currentTimeMillis()
    }
    
    fun recordFailure() {
        failedConnections++
    }
    
    fun getSuccessRate(): Double {
        val total = successfulConnections + failedConnections
        return if (total > 0) successfulConnections.toDouble() / total else 0.0
    }
}
```

## 🔄 **خطة التعافي التلقائي**

### 1. **مراقبة مستمرة**
```kotlin
class ConnectionMonitor {
    private val checkInterval = 60000L // دقيقة واحدة
    
    fun startMonitoring() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkConnection()
            }
        }, 0, checkInterval)
    }
    
    private suspend fun checkConnection() {
        if (settings.serverMode == ServerMode.CLOUD_ONLY) {
            val isConnected = gatewayService.testConnection()
            if (!isConnected) {
                // التبديل إلى الوضع التلقائي
                settings.serverMode = ServerMode.AUTO
                logsService.insert(
                    LogEntry.Priority.WARN,
                    "ConnectionMonitor",
                    "تم التبديل إلى الوضع التلقائي بسبب فقدان الاتصال"
                )
            }
        }
    }
}
```

### 2. **إعادة المحاولة الذكية**
```kotlin
class SmartRetry {
    private val maxRetries = 3
    private val baseDelay = 1000L // ثانية واحدة
    
    suspend fun <T> executeWithRetry(operation: suspend () -> T): T {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = baseDelay * (attempt + 1) // تأخير متزايد
                    delay(delay)
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error")
    }
}
```

## 📋 **قائمة التحقق للمطورين**

### قبل النشر:
- [ ] اختبار الوضع المحلي فقط
- [ ] اختبار الاتصال بالخادم السحابي
- [ ] التحقق من ملف `google-services.json`
- [ ] اختبار معالجة الأخطاء
- [ ] التحقق من السجلات
- [ ] اختبار إعادة الاتصال التلقائي

### للمستخدمين:
- [ ] تفعيل الوضع المحلي كإعداد افتراضي
- [ ] إضافة رسائل توضيحية للمستخدم
- [ ] توفير خيارات إعدادات سهلة
- [ ] إضافة دليل استخدام مبسط

## 🎯 **التوصيات النهائية**

1. **استخدم الوضع المحلي كافتراضي** لضمان عمل التطبيق دون مشاكل
2. **أضف واجهة إعدادات بسيطة** للمستخدمين المتقدمين
3. **راقب الاتصال باستمرار** وقم بالتبديل التلقائي عند الحاجة
4. **وفر رسائل خطأ واضحة** باللغة العربية
5. **اختبر جميع السيناريوهات** قبل النشر

---

**ملاحظة**: هذا الدليل يوفر حلول شاملة لمشاكل الاتصال بالخادم السحابي. يُنصح بتطبيق الحلول تدريجياً واختبارها قبل النشر النهائي.