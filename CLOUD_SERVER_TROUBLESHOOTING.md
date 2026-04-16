# دليل حل مشاكل الاتصال بالخادم السحابي

## 🔧 **الحلول المقترحة**

### 1. **تحديث إعدادات أمان الشبكة**

قم بتعديل ملف `app/src/main/res/xml/network_security_config.xml` لإضافة دعم أفضل للخادم السحابي:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="user" />
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- إعدادات خاصة للخادم السحابي -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.sms-gate.app</domain>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </domain-config>
    
    <!-- إعدادات للخادم المحلي -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="true">192.168.0.0/16</domain>
        <domain includeSubdomains="true">10.0.0.0/8</domain>
    </domain-config>
</network-security-config>
```

### 2. **إضافة معالجة أخطاء محسنة**

إنشاء فئة لمعالجة أخطاء الاتصال:

```kotlin
// app/src/main/java/me/capcom/smsgateway/modules/gateway/NetworkErrorHandler.kt
package me.capcom.smsgateway.modules.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NetworkErrorHandler(private val context: Context) {
    
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    fun handleNetworkError(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "لا يمكن الوصول إلى الخادم السحابي. تحقق من اتصال الإنترنت."
            is ConnectException -> "فشل في الاتصال بالخادم السحابي. تحقق من إعدادات الشبكة."
            is SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم السحابي. حاول مرة أخرى."
            is SSLException -> "مشكلة في شهادة الأمان للخادم السحابي."
            is ClientRequestException -> {
                when (error.response.status) {
                    HttpStatusCode.Unauthorized -> "خطأ في المصادقة. تحقق من بيانات الدخول."
                    HttpStatusCode.Forbidden -> "ليس لديك صلاحية للوصول إلى هذا المورد."
                    HttpStatusCode.NotFound -> "المورد المطلوب غير موجود على الخادم."
                    else -> "خطأ في الطلب: ${error.response.status}"
                }
            }
            is ServerResponseException -> "خطأ في الخادم السحابي: ${error.response.status}"
            else -> "خطأ غير معروف في الاتصال: ${error.localizedMessage ?: error.message}"
        }
    }
}
```

### 3. **تحسين GatewayApi مع معالجة أخطاء أفضل**

```kotlin
// إضافة إلى GatewayApi.kt
private val client = HttpClient(OkHttp) {
    install(UserAgent) {
        agent = "me.capcom.smsgateway/" + BuildConfig.VERSION_NAME
    }
    install(ContentNegotiation) {
        gson {
            configure()
        }
    }
    
    // إعدادات المهلة الزمنية
    engine {
        config {
            connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
    
    expectSuccess = false // تغيير لمعالجة الأخطاء يدوياً
}
```

### 4. **إضافة فحص الاتصال قبل العمليات**

```kotlin
// إضافة إلى GatewayService.kt
private val networkErrorHandler = NetworkErrorHandler(context)

suspend fun testConnection(): Boolean {
    return try {
        if (!networkErrorHandler.isNetworkAvailable()) {
            throw Exception("لا يوجد اتصال بالإنترنت")
        }
        
        val response = api.getDevice(null)
        true
    } catch (e: Exception) {
        logsService.insert(
            LogEntry.Priority.ERROR,
            "NetworkTest",
            networkErrorHandler.handleNetworkError(e),
            mapOf("error" to e.stackTraceToString())
        )
        false
    }
}
```

### 5. **إعدادات DNS البديلة**

إضافة إعدادات DNS في `GatewayApi.kt`:

```kotlin
private val client = HttpClient(OkHttp) {
    // ... إعدادات أخرى
    
    engine {
        config {
            // استخدام DNS عام لتجنب مشاكل DNS المحلي
            dns(object : okhttp3.Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    return try {
                        okhttp3.Dns.SYSTEM.lookup(hostname)
                    } catch (e: Exception) {
                        // استخدام Google DNS كبديل
                        java.net.InetAddress.getAllByName(hostname).toList()
                    }
                }
            })
        }
    }
}
```

## 🔍 **خطوات التشخيص**

### 1. **فحص الاتصال بالإنترنت**
```kotlin
fun checkInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
```

### 2. **فحص الوصول للخادم السحابي**
```bash
# اختبار من سطر الأوامر
curl -v https://api.sms-gate.app/mobile/v1/device
```

### 3. **فحص إعدادات البروكسي**
تأكد من أن التطبيق لا يستخدم إعدادات بروكسي خاطئة.

## 📱 **إعدادات التطبيق**

### 1. **تحديث عنوان الخادم السحابي**
في إعدادات التطبيق، تأكد من أن عنوان الخادم السحابي صحيح:
- العنوان الافتراضي: `https://api.sms-gate.app/mobile/v1`

### 2. **إعادة تسجيل الجهاز**
إذا استمرت المشكلة، قم بإعادة تسجيل الجهاز:
1. احذف بيانات التسجيل الحالية
2. أعد تسجيل الجهاز مع الخادم السحابي

### 3. **فحص الشهادات**
تأكد من أن شهادات SSL محدثة على الجهاز.

## 🛠️ **حلول إضافية**

### 1. **استخدام HTTP بدلاً من HTTPS (للاختبار فقط)**
```xml
<!-- في network_security_config_insecure.xml -->
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">api.sms-gate.app</domain>
</domain-config>
```

### 2. **إضافة إعادة المحاولة التلقائية**
```kotlin
suspend fun <T> retryOperation(
    maxRetries: Int = 3,
    delayMs: Long = 1000,
    operation: suspend () -> T
): T {
    repeat(maxRetries - 1) { attempt ->
        try {
            return operation()
        } catch (e: Exception) {
            delay(delayMs * (attempt + 1))
        }
    }
    return operation() // المحاولة الأخيرة
}
```

### 3. **تسجيل مفصل للأخطاء**
```kotlin
// إضافة تسجيل مفصل في GatewayService.kt
private fun logNetworkError(operation: String, error: Throwable) {
    logsService.insert(
        LogEntry.Priority.ERROR,
        "CloudServer",
        "فشل في $operation: ${networkErrorHandler.handleNetworkError(error)}",
        mapOf(
            "operation" to operation,
            "error_type" to error.javaClass.simpleName,
            "error_message" to error.message,
            "stack_trace" to error.stackTraceToString(),
            "server_url" to settings.serverUrl,
            "network_available" to networkErrorHandler.isNetworkAvailable()
        )
    )
}
```

## 📞 **الدعم الفني**

إذا استمرت المشكلة بعد تطبيق هذه الحلول:

1. **تحقق من سجلات التطبيق** للحصول على تفاصيل الخطأ
2. **اختبر الاتصال من متصفح الويب** للتأكد من الوصول للخادم
3. **تحقق من إعدادات الجدار الناري** على الشبكة
4. **جرب شبكة مختلفة** (مثل بيانات الهاتف المحمول)

---

**ملاحظة**: تأكد من تحديث التطبيق إلى أحدث إصدار للحصول على أحدث إصلاحات الأخطاء.