# دليل التكامل مع نظام الدفع الإلكتروني

## نظرة عامة

هذا الدليل يوضح كيفية تكامل تطبيق SMS Gateway مع أنظمة الدفع الإلكترونية المختلفة لمعالجة المدفوعات تلقائياً.

## سيناريوهات الاستخدام

### 1. متجر إلكتروني مع الدفع عند الاستلام
```javascript
// عند إنشاء طلب جديد
const order = {
  id: "ORD-12345",
  amount: 250.00,
  customer_phone: "01234567890",
  payment_method: "cash_on_delivery"
};

// تسجيل webhook لاستقبال تأكيد الدفع
app.post('/webhook/payment-confirmation', (req, res) => {
  const { transaction_id, amount, sender_phone } = req.body;
  
  // البحث عن الطلب المطابق
  const order = findOrderByPhone(sender_phone);
  
  if (order && parseFloat(amount) === order.amount) {
    // تأكيد الطلب وتحديث الحالة
    updateOrderStatus(order.id, 'paid');
    
    // إرسال إشعار للعميل
    sendConfirmationSMS(order.customer_phone, order.id);
  }
  
  res.status(200).send('OK');
});
```

### 2. نظام اشتراكات شهرية
```javascript
// مراقبة المدفوعات الشهرية
app.post('/webhook/subscription-payment', (req, res) => {
  const { amount, sender_phone, wallet_type } = req.body;
  
  const subscription = findActiveSubscription(sender_phone);
  
  if (subscription && amount >= subscription.monthly_fee) {
    // تجديد الاشتراك
    extendSubscription(subscription.id, 30); // 30 يوم
    
    // إرسال تأكيد
    sendSubscriptionConfirmation(sender_phone, subscription.end_date);
  }
  
  res.status(200).send('OK');
});
```

### 3. نظام محفظة رقمية
```javascript
// إضافة رصيد للمحفظة
app.post('/webhook/wallet-topup', (req, res) => {
  const { transaction_id, amount, sender_phone } = req.body;
  
  // إضافة الرصيد
  addWalletBalance(sender_phone, parseFloat(amount));
  
  // تسجيل المعاملة
  logTransaction({
    user_phone: sender_phone,
    type: 'topup',
    amount: amount,
    transaction_id: transaction_id,
    timestamp: new Date()
  });
  
  res.status(200).send('OK');
});
```

## أمثلة متقدمة

### تكامل مع قاعدة بيانات MySQL
```javascript
const mysql = require('mysql2/promise');

const connection = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'password',
  database: 'payments'
});

app.post('/webhook/payment', async (req, res) => {
  const { transaction_id, amount, sender_phone, wallet_type } = req.body;
  
  try {
    // حفظ المعاملة في قاعدة البيانات
    await connection.execute(
      'INSERT INTO payments (transaction_id, amount, sender_phone, wallet_type, status, created_at) VALUES (?, ?, ?, ?, ?, NOW())',
      [transaction_id, amount, sender_phone, wallet_type, 'confirmed']
    );
    
    // معالجة الطلب المرتبط
    const [orders] = await connection.execute(
      'SELECT * FROM orders WHERE customer_phone = ? AND amount = ? AND status = "pending"',
      [sender_phone, amount]
    );
    
    if (orders.length > 0) {
      const order = orders[0];
      
      // تحديث حالة الطلب
      await connection.execute(
        'UPDATE orders SET status = "paid", payment_transaction_id = ? WHERE id = ?',
        [transaction_id, order.id]
      );
      
      // إرسال إشعار
      console.log(`تم تأكيد الدفع للطلب ${order.id}`);
    }
    
    res.status(200).send('OK');
  } catch (error) {
    console.error('خطأ في معالجة الدفع:', error);
    res.status(500).send('Error');
  }
});
```

### تكامل مع Redis للتخزين المؤقت
```javascript
const redis = require('redis');
const client = redis.createClient();

app.post('/webhook/payment', async (req, res) => {
  const { transaction_id, amount, sender_phone } = req.body;
  
  // التحقق من عدم تكرار المعاملة
  const exists = await client.exists(`payment:${transaction_id}`);
  if (exists) {
    return res.status(200).send('Already processed');
  }
  
  // حفظ المعاملة مؤقتاً
  await client.setex(`payment:${transaction_id}`, 3600, JSON.stringify(req.body));
  
  // معالجة الدفع
  processPayment(req.body);
  
  res.status(200).send('OK');
});
```

## إعدادات الأمان

### 1. التحقق من صحة الـ Webhook
```javascript
const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');
    
  return crypto.timingSafeEqual(
    Buffer.from(signature, 'hex'),
    Buffer.from(expectedSignature, 'hex')
  );
}

app.post('/webhook/payment', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);
  
  if (!verifyWebhook(payload, signature, process.env.WEBHOOK_SECRET)) {
    return res.status(401).send('Unauthorized');
  }
  
  // معالجة الدفع
  processPayment(req.body);
  res.status(200).send('OK');
});
```

### 2. تحديد معدل الطلبات
```javascript
const rateLimit = require('express-rate-limit');

const paymentLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 دقيقة
  max: 100, // حد أقصى 100 طلب لكل IP
  message: 'تم تجاوز الحد المسموح من الطلبات'
});

app.use('/webhook', paymentLimiter);
```

## مراقبة ومتابعة الأداء

### 1. تسجيل العمليات
```javascript
const winston = require('winston');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  transports: [
    new winston.transports.File({ filename: 'payments.log' })
  ]
});

app.post('/webhook/payment', (req, res) => {
  logger.info('Payment received', {
    transaction_id: req.body.transaction_id,
    amount: req.body.amount,
    wallet_type: req.body.wallet_type,
    timestamp: new Date().toISOString()
  });
  
  // معالجة الدفع
  processPayment(req.body);
  res.status(200).send('OK');
});
```

### 2. إحصائيات الدفع
```javascript
// الحصول على إحصائيات يومية
app.get('/api/payment-stats', async (req, res) => {
  try {
    const response = await fetch('http://gateway-ip:port/payments/stats?hours=24', {
      headers: { 'Authorization': 'Bearer your-token' }
    });
    
    const stats = await response.json();
    
    res.json({
      daily_transactions: stats.confirmed_transactions,
      daily_amount: stats.total_amount,
      currency: stats.currency
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch stats' });
  }
});
```

## استكشاف الأخطاء وإصلاحها

### مشاكل شائعة وحلولها

1. **عدم استقبال الـ webhooks**
   - تأكد من أن الـ URL صحيح ومتاح
   - تحقق من إعدادات الجدار الناري
   - استخدم أدوات مثل ngrok للاختبار المحلي

2. **تكرار المعاملات**
   - استخدم `transaction_id` للتحقق من التكرار
   - احفظ المعاملات المعالجة في قاعدة البيانات

3. **فشل في تحليل الرسائل**
   - تحقق من أنماط الرسائل المدعومة
   - أضف أنماط جديدة حسب الحاجة

### أدوات الاختبار
```bash
# اختبار webhook محلياً
curl -X POST http://localhost:3000/webhook/payment \
  -H "Content-Type: application/json" \
  -d '{
    "event": "payment:detected",
    "transaction_id": "test-123",
    "amount": "100.00",
    "sender_phone": "01234567890",
    "wallet_type": "INSTAPAY"
  }'
```

## الخلاصة

نظام الدفع الإلكتروني المدمج يوفر حلاً شاملاً لمعالجة المدفوعات تلقائياً، مما يقلل من التدخل اليدوي ويحسن تجربة العملاء. باتباع هذا الدليل، يمكنك تكامل النظام مع تطبيقك بسهولة وأمان.