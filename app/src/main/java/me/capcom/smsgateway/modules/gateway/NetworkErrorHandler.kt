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
                    HttpStatusCode.BadRequest -> "طلب غير صحيح. تحقق من البيانات المرسلة."
                    HttpStatusCode.TooManyRequests -> "تم تجاوز حد الطلبات المسموح. حاول لاحقاً."
                    else -> "خطأ في الطلب: ${error.response.status.value} - ${error.response.status.description}"
                }
            }
            is ServerResponseException -> "خطأ في الخادم السحابي: ${error.response.status.value} - ${error.response.status.description}"
            else -> "خطأ غير معروف في الاتصال: ${error.localizedMessage ?: error.message ?: error.toString()}"
        }
    }
    
    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "غير متصل"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "غير معروف"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "بيانات الهاتف"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "إيثرنت"
            else -> "غير معروف"
        }
    }
}