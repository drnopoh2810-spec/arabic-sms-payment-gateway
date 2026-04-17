package me.capcom.smsgateway.modules.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * معالج أخطاء الشبكة مع رسائل باللغة العربية
 */
class NetworkErrorHandler(private val context: Context) {
    
    /**
     * معالجة أخطاء الشبكة وإرجاع رسالة مفهومة باللغة العربية
     */
    fun handleNetworkError(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> {
                "لا يمكن الوصول إلى الخادم. تحقق من اتصال الإنترنت أو عنوان الخادم."
            }
            is ConnectException -> {
                "فشل في الاتصال بالخادم. قد يكون الخادم غير متاح حالياً."
            }
            is SocketTimeoutException, is HttpRequestTimeoutException -> {
                "انتهت مهلة الاتصال. تحقق من سرعة الإنترنت وحاول مرة أخرى."
            }
            is SSLException -> {
                "خطأ في الأمان (SSL). تحقق من شهادة الخادم."
            }
            is ClientRequestException -> {
                when (throwable.response.status.value) {
                    400 -> "طلب غير صحيح. تحقق من البيانات المرسلة."
                    401 -> "غير مصرح. تحقق من بيانات تسجيل الدخول."
                    403 -> "ممنوع. ليس لديك صلاحية للوصول."
                    404 -> "الخادم غير موجود. تحقق من عنوان الخادم."
                    429 -> "تم تجاوز حد الطلبات. حاول مرة أخرى لاحقاً."
                    else -> "خطأ من العميل (${throwable.response.status.value}): ${throwable.message}"
                }
            }
            is ServerResponseException -> {
                when (throwable.response.status.value) {
                    500 -> "خطأ داخلي في الخادم. حاول مرة أخرى لاحقاً."
                    502 -> "خادم البوابة غير متاح."
                    503 -> "الخدمة غير متاحة مؤقتاً."
                    504 -> "انتهت مهلة البوابة."
                    else -> "خطأ في الخادم (${throwable.response.status.value}): ${throwable.message}"
                }
            }
            else -> {
                "خطأ غير متوقع: ${throwable.localizedMessage ?: throwable.message ?: throwable.toString()}"
            }
        }
    }
    
    /**
     * التحقق من توفر اتصال الإنترنت
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * الحصول على نوع الشبكة الحالية
     */
    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "غير متصل"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "غير معروف"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "بيانات الجوال"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "إيثرنت"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "بلوتوث"
                else -> "غير معروف"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "بيانات الجوال"
                ConnectivityManager.TYPE_ETHERNET -> "إيثرنت"
                ConnectivityManager.TYPE_BLUETOOTH -> "بلوتوث"
                else -> "غير معروف"
            }
        }
    }
    
    /**
     * التحقق من قوة الإشارة
     */
    fun getSignalStrength(): String {
        // يمكن تطوير هذه الوظيفة لاحقاً لقياس قوة الإشارة
        return if (isNetworkAvailable()) "جيد" else "ضعيف"
    }
}