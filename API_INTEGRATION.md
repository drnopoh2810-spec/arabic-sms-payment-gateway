# Arabic SMS Payment Gateway - API Integration Guide

## Overview
This guide provides examples of how to integrate with the Arabic SMS Payment Gateway API in popular programming languages. The API supports electronic wallet payment detection and processing for Egyptian payment systems.

## Base Configuration

**Base URL:** `http://your-device-ip:8080`
**Authentication:** Basic Auth or JWT Token
**Content-Type:** `application/json`

## Authentication

### Basic Authentication
Use the username and password configured in the app settings.

### JWT Authentication
1. Get a token from `/auth/login`
2. Include in headers: `Authorization: Bearer <token>`

## API Endpoints

### 1. Health Check
**GET** `/health`
```json
{
  "status": "pass",
  "checks": {
    "sms": "pass",
    "storage": "pass"
  }
}
```

### 2. Payment Transactions
**GET** `/payments`
- Query parameters:
  - `limit`: Number of transactions (default: 50)
  - `wallet_type`: Filter by wallet (INSTAPAY, VODAFONE_CASH, ORANGE_CASH, etc.)

### 3. Payment Statistics
**GET** `/payments/stats?hours=24`

### 4. Confirm Payment
**POST** `/payments/{id}/confirm`

### 5. Payment Settings
**GET** `/payments/settings`
**PATCH** `/payments/settings`

## Integration Examples

### JavaScript/Node.js

```javascript
const axios = require('axios');

class SMSPaymentGateway {
    constructor(baseUrl, username, password) {
        this.baseUrl = baseUrl;
        this.auth = Buffer.from(`${username}:${password}`).toString('base64');
    }

    async getPayments(limit = 50, walletType = null) {
        try {
            const params = { limit };
            if (walletType) params.wallet_type = walletType;
            
            const response = await axios.get(`${this.baseUrl}/payments`, {
                headers: {
                    'Authorization': `Basic ${this.auth}`,
                    'Content-Type': 'application/json'
                },
                params
            });
            
            return response.data.transactions;
        } catch (error) {
            console.error('Error fetching payments:', error.response?.data || error.message);
            throw error;
        }
    }

    async confirmPayment(transactionId) {
        try {
            const response = await axios.post(
                `${this.baseUrl}/payments/${transactionId}/confirm`,
                {},
                {
                    headers: {
                        'Authorization': `Basic ${this.auth}`,
                        'Content-Type': 'application/json'
                    }
                }
            );
            
            return response.data;
        } catch (error) {
            console.error('Error confirming payment:', error.response?.data || error.message);
            throw error;
        }
    }

    async getPaymentStats(hours = 24) {
        try {
            const response = await axios.get(`${this.baseUrl}/payments/stats`, {
                headers: {
                    'Authorization': `Basic ${this.auth}`,
                    'Content-Type': 'application/json'
                },
                params: { hours }
            });
            
            return response.data;
        } catch (error) {
            console.error('Error fetching stats:', error.response?.data || error.message);
            throw error;
        }
    }
}

// Usage Example
const gateway = new SMSPaymentGateway('http://192.168.1.100:8080', 'admin', 'password');

// Get recent InstaPay transactions
gateway.getPayments(10, 'INSTAPAY')
    .then(transactions => {
        console.log('InstaPay Transactions:', transactions);
    })
    .catch(error => {
        console.error('Error:', error);
    });

// Confirm a payment
gateway.confirmPayment('transaction-id-123')
    .then(result => {
        console.log('Payment confirmed:', result);
    })
    .catch(error => {
        console.error('Error:', error);
    });
```

### Python

```python
import requests
import base64
from typing import Optional, List, Dict, Any

class SMSPaymentGateway:
    def __init__(self, base_url: str, username: str, password: str):
        self.base_url = base_url.rstrip('/')
        self.auth_header = base64.b64encode(f"{username}:{password}".encode()).decode()
        self.headers = {
            'Authorization': f'Basic {self.auth_header}',
            'Content-Type': 'application/json'
        }

    def get_payments(self, limit: int = 50, wallet_type: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get payment transactions"""
        params = {'limit': limit}
        if wallet_type:
            params['wallet_type'] = wallet_type
            
        try:
            response = requests.get(
                f"{self.base_url}/payments",
                headers=self.headers,
                params=params
            )
            response.raise_for_status()
            return response.json()['transactions']
        except requests.exceptions.RequestException as e:
            print(f"Error fetching payments: {e}")
            raise

    def confirm_payment(self, transaction_id: str) -> Dict[str, Any]:
        """Confirm a payment transaction"""
        try:
            response = requests.post(
                f"{self.base_url}/payments/{transaction_id}/confirm",
                headers=self.headers
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error confirming payment: {e}")
            raise

    def get_payment_stats(self, hours: int = 24) -> Dict[str, Any]:
        """Get payment statistics"""
        try:
            response = requests.get(
                f"{self.base_url}/payments/stats",
                headers=self.headers,
                params={'hours': hours}
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching stats: {e}")
            raise

    def get_pending_payments(self) -> List[Dict[str, Any]]:
        """Get pending payment transactions"""
        try:
            response = requests.get(
                f"{self.base_url}/payments/pending",
                headers=self.headers
            )
            response.raise_for_status()
            return response.json()['transactions']
        except requests.exceptions.RequestException as e:
            print(f"Error fetching pending payments: {e}")
            raise

# Usage Example
if __name__ == "__main__":
    gateway = SMSPaymentGateway('http://192.168.1.100:8080', 'admin', 'password')
    
    try:
        # Get recent Vodafone Cash transactions
        transactions = gateway.get_payments(limit=10, wallet_type='VODAFONE_CASH')
        print(f"Found {len(transactions)} Vodafone Cash transactions")
        
        # Get payment statistics for last 24 hours
        stats = gateway.get_payment_stats(24)
        print(f"Payment stats: {stats}")
        
        # Get pending payments
        pending = gateway.get_pending_payments()
        print(f"Pending payments: {len(pending)}")
        
        # Confirm first pending payment if any
        if pending:
            result = gateway.confirm_payment(pending[0]['id'])
            print(f"Payment confirmation: {result}")
            
    except Exception as e:
        print(f"Error: {e}")
```

### PHP

```php
<?php

class SMSPaymentGateway {
    private $baseUrl;
    private $authHeader;
    
    public function __construct($baseUrl, $username, $password) {
        $this->baseUrl = rtrim($baseUrl, '/');
        $this->authHeader = base64_encode("$username:$password");
    }
    
    private function makeRequest($method, $endpoint, $data = null, $params = []) {
        $url = $this->baseUrl . $endpoint;
        if (!empty($params)) {
            $url .= '?' . http_build_query($params);
        }
        
        $headers = [
            'Authorization: Basic ' . $this->authHeader,
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        
        if ($data !== null) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        }
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode >= 400) {
            throw new Exception("HTTP Error $httpCode: $response");
        }
        
        return json_decode($response, true);
    }
    
    public function getPayments($limit = 50, $walletType = null) {
        $params = ['limit' => $limit];
        if ($walletType) {
            $params['wallet_type'] = $walletType;
        }
        
        $result = $this->makeRequest('GET', '/payments', null, $params);
        return $result['transactions'];
    }
    
    public function confirmPayment($transactionId) {
        return $this->makeRequest('POST', "/payments/$transactionId/confirm");
    }
    
    public function getPaymentStats($hours = 24) {
        return $this->makeRequest('GET', '/payments/stats', null, ['hours' => $hours]);
    }
    
    public function getPendingPayments() {
        $result = $this->makeRequest('GET', '/payments/pending');
        return $result['transactions'];
    }
}

// Usage Example
try {
    $gateway = new SMSPaymentGateway('http://192.168.1.100:8080', 'admin', 'password');
    
    // Get Orange Cash transactions
    $transactions = $gateway->getPayments(10, 'ORANGE_CASH');
    echo "Found " . count($transactions) . " Orange Cash transactions\n";
    
    // Get payment statistics
    $stats = $gateway->getPaymentStats(24);
    echo "Payment stats: " . json_encode($stats) . "\n";
    
    // Get and confirm pending payments
    $pending = $gateway->getPendingPayments();
    if (!empty($pending)) {
        $result = $gateway->confirmPayment($pending[0]['id']);
        echo "Payment confirmed: " . json_encode($result) . "\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

?>
```

### Java

```java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SMSPaymentGateway {
    private final String baseUrl;
    private final String authHeader;
    private final HttpClient client;
    private final Gson gson;
    
    public SMSPaymentGateway(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.authHeader = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    private HttpResponse<String> makeRequest(String method, String endpoint, String body, String queryParams) 
            throws IOException, InterruptedException {
        String url = baseUrl + endpoint;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + authHeader)
                .header("Content-Type", "application/json");
        
        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "PATCH":
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
        }
        
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    
    public List<Map<String, Object>> getPayments(int limit, String walletType) 
            throws IOException, InterruptedException {
        String queryParams = "limit=" + limit;
        if (walletType != null) {
            queryParams += "&wallet_type=" + walletType;
        }
        
        HttpResponse<String> response = makeRequest("GET", "/payments", null, queryParams);
        
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP Error " + response.statusCode() + ": " + response.body());
        }
        
        Map<String, Object> result = gson.fromJson(response.body(), 
                new TypeToken<Map<String, Object>>(){}.getType());
        return (List<Map<String, Object>>) result.get("transactions");
    }
    
    public Map<String, Object> confirmPayment(String transactionId) 
            throws IOException, InterruptedException {
        HttpResponse<String> response = makeRequest("POST", "/payments/" + transactionId + "/confirm", null, null);
        
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP Error " + response.statusCode() + ": " + response.body());
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    public Map<String, Object> getPaymentStats(int hours) 
            throws IOException, InterruptedException {
        HttpResponse<String> response = makeRequest("GET", "/payments/stats", null, "hours=" + hours);
        
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP Error " + response.statusCode() + ": " + response.body());
        }
        
        return gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    // Usage Example
    public static void main(String[] args) {
        try {
            SMSPaymentGateway gateway = new SMSPaymentGateway(
                "http://192.168.1.100:8080", "admin", "password");
            
            // Get InstaPay transactions
            List<Map<String, Object>> transactions = gateway.getPayments(10, "INSTAPAY");
            System.out.println("Found " + transactions.size() + " InstaPay transactions");
            
            // Get payment statistics
            Map<String, Object> stats = gateway.getPaymentStats(24);
            System.out.println("Payment stats: " + stats);
            
            // Confirm a payment (example)
            if (!transactions.isEmpty()) {
                String transactionId = (String) transactions.get(0).get("id");
                Map<String, Object> result = gateway.confirmPayment(transactionId);
                System.out.println("Payment confirmed: " + result);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### C#

```csharp
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

public class SMSPaymentGateway
{
    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;
    
    public SMSPaymentGateway(string baseUrl, string username, string password)
    {
        _baseUrl = baseUrl.TrimEnd('/');
        _httpClient = new HttpClient();
        
        var authValue = Convert.ToBase64String(Encoding.UTF8.GetBytes($"{username}:{password}"));
        _httpClient.DefaultRequestHeaders.Authorization = 
            new System.Net.Http.Headers.AuthenticationHeaderValue("Basic", authValue);
        _httpClient.DefaultRequestHeaders.Add("Content-Type", "application/json");
    }
    
    public async Task<List<PaymentTransaction>> GetPaymentsAsync(int limit = 50, string walletType = null)
    {
        var queryParams = $"limit={limit}";
        if (!string.IsNullOrEmpty(walletType))
        {
            queryParams += $"&wallet_type={walletType}";
        }
        
        var response = await _httpClient.GetAsync($"{_baseUrl}/payments?{queryParams}");
        response.EnsureSuccessStatusCode();
        
        var content = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<PaymentResponse>(content);
        return result.Transactions;
    }
    
    public async Task<ConfirmationResult> ConfirmPaymentAsync(string transactionId)
    {
        var response = await _httpClient.PostAsync($"{_baseUrl}/payments/{transactionId}/confirm", null);
        response.EnsureSuccessStatusCode();
        
        var content = await response.Content.ReadAsStringAsync();
        return JsonConvert.DeserializeObject<ConfirmationResult>(content);
    }
    
    public async Task<PaymentStats> GetPaymentStatsAsync(int hours = 24)
    {
        var response = await _httpClient.GetAsync($"{_baseUrl}/payments/stats?hours={hours}");
        response.EnsureSuccessStatusCode();
        
        var content = await response.Content.ReadAsStringAsync();
        return JsonConvert.DeserializeObject<PaymentStats>(content);
    }
    
    public void Dispose()
    {
        _httpClient?.Dispose();
    }
}

// Data Models
public class PaymentTransaction
{
    public string Id { get; set; }
    public string MessageId { get; set; }
    public string WalletType { get; set; }
    public decimal Amount { get; set; }
    public string Currency { get; set; }
    public string SenderName { get; set; }
    public string SenderPhone { get; set; }
    public string Reference { get; set; }
    public string TransactionId { get; set; }
    public string Description { get; set; }
    public bool IsConfirmed { get; set; }
    public bool IsProcessed { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? ConfirmedAt { get; set; }
    public DateTime? ProcessedAt { get; set; }
}

public class PaymentResponse
{
    public List<PaymentTransaction> Transactions { get; set; }
}

public class ConfirmationResult
{
    public bool Success { get; set; }
    public string Message { get; set; }
}

public class PaymentStats
{
    public int ConfirmedTransactions { get; set; }
    public string TotalAmount { get; set; }
    public string Currency { get; set; }
    public int PeriodHours { get; set; }
}

// Usage Example
class Program
{
    static async Task Main(string[] args)
    {
        var gateway = new SMSPaymentGateway("http://192.168.1.100:8080", "admin", "password");
        
        try
        {
            // Get Fawry transactions
            var transactions = await gateway.GetPaymentsAsync(10, "FAWRY");
            Console.WriteLine($"Found {transactions.Count} Fawry transactions");
            
            // Get payment statistics
            var stats = await gateway.GetPaymentStatsAsync(24);
            Console.WriteLine($"Payment stats: {JsonConvert.SerializeObject(stats)}");
            
            // Confirm first transaction if any
            if (transactions.Count > 0)
            {
                var result = await gateway.ConfirmPaymentAsync(transactions[0].Id);
                Console.WriteLine($"Payment confirmed: {result.Success}");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex.Message}");
        }
        finally
        {
            gateway.Dispose();
        }
    }
}
```

## Supported Egyptian Wallets

- **InstaPay**: `INSTAPAY`
- **Vodafone Cash**: `VODAFONE_CASH`
- **Orange Cash**: `ORANGE_CASH`
- **Etisalat Cash**: `ETISALAT_CASH`
- **Fawry**: `FAWRY`
- **CIB Wallet**: `CIB_WALLET`
- **NBE Wallet**: `NBE_WALLET`

## Webhook Integration

Configure a webhook URL in the payment settings to receive real-time payment notifications:

```json
{
  "event": "payment_received",
  "transaction": {
    "id": "txn_123456",
    "wallet_type": "INSTAPAY",
    "amount": 100.50,
    "currency": "EGP",
    "sender_name": "أحمد محمد",
    "sender_phone": "+201234567890",
    "reference": "REF123",
    "transaction_id": "IP123456789",
    "description": "دفع فاتورة",
    "created_at": "2024-04-17T10:30:00Z"
  }
}
```

## Error Handling

All API endpoints return standard HTTP status codes:
- `200`: Success
- `400`: Bad Request
- `401`: Unauthorized
- `404`: Not Found
- `500`: Internal Server Error

Error responses include a message field:
```json
{
  "message": "Transaction not found"
}
```

## Security Notes

1. Always use HTTPS in production
2. Store credentials securely
3. Implement rate limiting on your side
4. Validate webhook signatures if implemented
5. Use strong authentication credentials
6. Monitor API usage and logs

## Support

For technical support and questions, please refer to the project documentation or create an issue in the GitHub repository.