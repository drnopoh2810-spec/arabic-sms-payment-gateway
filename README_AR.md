# بوابة الرسائل النصية مع نظام الدفع الإلكتروني

## نظرة عامة

تطبيق Android SMS Gateway محسّن مع دعم كامل للغة العربية ونظام متقدم لاستخراج ومعالجة معلومات الدفع من رسائل المحافظ الإلكترونية المصرية.

## الميزات الجديدة

### 🌍 التعريب الكامل
- واجهة مستخدم باللغة العربية
- دعم النصوص من اليمين إلى اليسار (RTL)
- ترجمة جميع النصوص والرسائل

### 💳 نظام الدفع الإلكتروني

#### المحافظ المدعومة
- **InstaPay** - إنستاباي
- **Vodafone Cash** - فودافون كاش  
- **Orange Cash** - أورانج كاش
- **Etisalat Cash** - اتصالات كاش
- **Fawry** - فوري
- **WE Pay** - وي باي
- **Bank Misr** - بنك مصر
- **NBE** - البنك الأهلي المصري
- **CIB** - البنك التجاري الدولي

#### الوظائف الأساسية
- **استخراج تلقائي** لمعلومات الدفع من الرسائل النصية
- **تحليل ذكي** للمبالغ والمرسلين والمراجع
- **تأكيد تلقائي** أو يدوي للمعاملات
- **إشعارات فورية** عند اكتشاف عمليات دفع
- **API متكامل** للتكامل مع أنظمة الدفع الخارجية

## API Endpoints الجديدة

### معاملات الدفع

```http
GET /payments
```
استرجاع قائمة معاملات الدفع

**المعاملات:**
- `limit` (اختياري): عدد المعاملات (افتراضي: 50)
- `wallet_type` (اختياري): نوع المحفظة للتصفية

```http
GET /payments/{id}
```
استرجاع معاملة محددة

```http
POST /payments/{id}/confirm
```
تأكيد معاملة دفع

```http
GET /payments/stats
```
إحصائيات الدفع

**المعاملات:**
- `hours` (اختياري): فترة الإحصائيات بالساعات (افتراضي: 24)

```http
GET /payments/pending
```
المعاملات المعلقة (غير المؤكدة)

### إعدادات الدفع

```http
GET /payments/settings
```
استرجاع إعدادات الدفع

```http
PATCH /payments/settings
```
تحديث إعدادات الدفع

**مثال على البيانات:**
```json
{
  "enabled": true,
  "auto_confirm": false,
  "show_notifications": true,
  "webhook_url": "https://your-api.com/webhook",
  "enabled_wallets": ["INSTAPAY", "VODAFONE_CASH"],
  "minimum_amount": "1.00",
  "maximum_amount": "10000.00",
  "retention_days": 30,
  "require_confirmation": true,
  "webhook_timeout": 30,
  "webhook_retries": 3
}
```

## Webhook Events الجديدة

### payment:detected
يتم إرساله عند اكتشاف عملية دفع جديدة

```json
{
  "event": "payment:detected",
  "transaction_id": "uuid",
  "wallet_type": "INSTAPAY",
  "amount": "100.00",
  "currency": "EGP",
  "sender_name": "أحمد محمد",
  "sender_phone": "01234567890",
  "reference": "REF123456",
  "transaction_ref": "TXN789012",
  "description": "تحويل فوري",
  "created_at": 1713276000000,
  "raw_message": "تم استلام مبلغ 100.00 جنيه من أحمد محمد..."
}
```

### payment:confirmed
يتم إرساله عند تأكيد عملية دفع

```json
{
  "event": "payment:confirmed",
  "transaction_id": "uuid",
  "wallet_type": "INSTAPAY",
  "amount": "100.00",
  "currency": "EGP",
  "sender_name": "أحمد محمد",
  "confirmed_at": 1713276300000,
  "created_at": 1713276000000
}
```

## أمثلة على الاستخدام

### تكامل مع نظام دفع

```javascript
// استقبال webhook عند اكتشاف دفعة
app.post('/webhook/payment', (req, res) => {
  const { event, transaction_id, amount, wallet_type } = req.body;
  
  if (event === 'payment:detected') {
    // معالجة الدفعة المكتشفة
    console.log(`دفعة جديدة: ${amount} EGP من ${wallet_type}`);
    
    // تأكيد الدفعة تلقائياً
    fetch(`http://gateway-ip:port/payments/${transaction_id}/confirm`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer your-token' }
    });
  }
  
  res.status(200).send('OK');
});
```

### استعلام عن المعاملات

```javascript
// الحصول على آخر المعاملات
const response = await fetch('http://gateway-ip:port/payments?limit=10', {
  headers: { 'Authorization': 'Bearer your-token' }
});

const { transactions } = await response.json();
console.log('آخر المعاملات:', transactions);
```

## التثبيت والإعداد

### 1. تحديث قاعدة البيانات
سيتم إنشاء جدول `payment_transactions` تلقائياً عند تشغيل التطبيق.

### 2. تفعيل نظام الدفع
```http
PATCH /payments/settings
Content-Type: application/json

{
  "enabled": true,
  "webhook_url": "https://your-api.com/webhook"
}
```

### 3. تكوين المحافظ المدعومة
```http
PATCH /payments/settings
Content-Type: application/json

{
  "enabled_wallets": [
    "INSTAPAY",
    "VODAFONE_CASH", 
    "ORANGE_CASH",
    "FAWRY"
  ]
}
```

## الأمان والخصوصية

- جميع معلومات الدفع مشفرة في قاعدة البيانات
- دعم HTTPS للـ webhooks
- إمكانية تحديد مهلة زمنية لحذف المعاملات القديمة
- مصادقة JWT للـ API endpoints

## المتطلبات

- Android 7.0+ (API level 24)
- إذن قراءة الرسائل النصية
- اتصال بالإنترنت (للـ webhooks)

## الدعم الفني

للحصول على الدعم الفني أو الإبلاغ عن مشاكل، يرجى إنشاء issue في المستودع.

## الترخيص

هذا المشروع مرخص تحت رخصة MIT - راجع ملف LICENSE للتفاصيل.