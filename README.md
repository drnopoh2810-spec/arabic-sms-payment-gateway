# بوابة الرسائل النصية العربية مع نظام الدفع الإلكتروني

## نظرة عامة

تطبيق Android متطور لإدارة الرسائل النصية مع دعم كامل للغة العربية ونظام متقدم لمعالجة المدفوعات الإلكترونية من المحافظ المصرية.

## الميزات الرئيسية

### 🌍 دعم اللغة العربية
- واجهة مستخدم باللغة العربية بالكامل
- دعم اتجاه النص من اليمين إلى اليسار (RTL)
- ترجمة شاملة لجميع النصوص والرسائل

### 💳 نظام الدفع الإلكتروني المتطور
- **استخراج تلقائي** لمعلومات الدفع من رسائل SMS
- **دعم المحافظ المصرية**: InstaPay، Vodafone Cash، Orange Cash، Etisalat Cash، Fawry
- **تأكيد تلقائي** للمعاملات مع إشعارات فورية
- **API متكامل** للتكامل مع أنظمة الدفع الخارجية
- **Webhooks** لمعالجة المدفوعات في الوقت الفعلي

### 📱 إدارة الرسائل النصية
- إرسال واستقبال الرسائل النصية
- دعم الرسائل المتعددة الأجزاء
- إدارة SIM متعددة
- تشفير الرسائل
- سجل شامل للعمليات

## التثبيت والإعداد

### المتطلبات
- Android 7.0+ (API level 24)
- إذن قراءة وإرسال الرسائل النصية
- اتصال بالإنترنت (للـ webhooks)

### خطوات التثبيت
1. قم بتحميل أحدث إصدار من [Releases](../../releases)
2. قم بتثبيت التطبيق على جهاز Android
3. امنح الأذونات المطلوبة
4. قم بتكوين الإعدادات حسب احتياجاتك

## API Documentation

### إرسال رسالة نصية
```http
POST /message
Content-Type: application/json
Authorization: Bearer your-jwt-token

{
  "message": "مرحباً بك",
  "phoneNumbers": ["+201234567890"],
  "withDeliveryReport": true
}
```

### الحصول على معاملات الدفع
```http
GET /payments?limit=10
Authorization: Bearer your-jwt-token
```

### تأكيد معاملة دفع
```http
POST /payments/{transaction_id}/confirm
Authorization: Bearer your-jwt-token
```

## أمثلة التكامل

### JavaScript/Node.js
```javascript
const axios = require('axios');

// إرسال رسالة
const sendSMS = async () => {
  const response = await axios.post('http://gateway-ip:port/message', {
    message: 'مرحباً بك في خدمتنا',
    phoneNumbers: ['+201234567890']
  }, {
    headers: {
      'Authorization': 'Bearer your-token',
      'Content-Type': 'application/json'
    }
  });
  
  console.log('تم إرسال الرسالة:', response.data);
};

// استقبال webhook للدفع
app.post('/webhook/payment', (req, res) => {
  const { event, transaction_id, amount, wallet_type } = req.body;
  
  if (event === 'payment:detected') {
    console.log(`دفعة جديدة: ${amount} EGP من ${wallet_type}`);
    // معالجة الدفعة
  }
  
  res.status(200).send('OK');
});
```

### Python
```python
import requests

# إرسال رسالة
def send_sms():
    url = "http://gateway-ip:port/message"
    headers = {
        "Authorization": "Bearer your-token",
        "Content-Type": "application/json"
    }
    data = {
        "message": "مرحباً بك في خدمتنا",
        "phoneNumbers": ["+201234567890"]
    }
    
    response = requests.post(url, json=data, headers=headers)
    print("تم إرسال الرسالة:", response.json())

# Flask webhook للدفع
from flask import Flask, request

app = Flask(__name__)

@app.route('/webhook/payment', methods=['POST'])
def payment_webhook():
    data = request.json
    if data['event'] == 'payment:detected':
        print(f"دفعة جديدة: {data['amount']} EGP من {data['wallet_type']}")
        # معالجة الدفعة
    
    return 'OK', 200
```

### PHP
```php
<?php
// إرسال رسالة
function sendSMS() {
    $url = "http://gateway-ip:port/message";
    $headers = [
        "Authorization: Bearer your-token",
        "Content-Type: application/json"
    ];
    $data = [
        "message" => "مرحباً بك في خدمتنا",
        "phoneNumbers" => ["+201234567890"]
    ];
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    
    $response = curl_exec($ch);
    curl_close($ch);
    
    echo "تم إرسال الرسالة: " . $response;
}

// Webhook للدفع
if ($_POST['event'] === 'payment:detected') {
    $amount = $_POST['amount'];
    $wallet_type = $_POST['wallet_type'];
    echo "دفعة جديدة: {$amount} EGP من {$wallet_type}";
    // معالجة الدفعة
}
?>
```

## المساهمة

نرحب بالمساهمات! يرجى:
1. عمل Fork للمشروع
2. إنشاء branch جديد للميزة
3. إجراء التغييرات المطلوبة
4. إرسال Pull Request

## الترخيص

هذا المشروع مرخص تحت رخصة MIT - راجع ملف [LICENSE](LICENSE) للتفاصيل.

## الدعم الفني

- إنشاء [Issue](../../issues) للإبلاغ عن مشاكل
- مراجعة [الوثائق](docs/) للمزيد من التفاصيل
- التواصل عبر [Discussions](../../discussions) للأسئلة العامة

## الشكر والتقدير

شكر خاص لجميع المساهمين في تطوير هذا المشروع وجعله أفضل للمجتمع العربي.