package me.capcom.smsgateway.modules.payment

import me.capcom.smsgateway.modules.payment.data.PaymentInfo
import me.capcom.smsgateway.modules.payment.data.PaymentPattern
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

class PaymentParser {
    
    private val paymentPatterns = listOf(
        // InstaPay patterns
        PaymentPattern(
            walletType = PaymentWalletType.INSTAPAY,
            amountRegex = Regex("""(?:تم استلام|received|استقبال)\s*(?:مبلغ)?\s*(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP|LE)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from)\s*([^\s]+(?:\s+[^\s]+)?)""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:مرجع|ref|reference)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""(?:رقم العملية|transaction|trans)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
        ),
        
        // Vodafone Cash patterns
        PaymentPattern(
            walletType = PaymentWalletType.VODAFONE_CASH,
            amountRegex = Regex("""(?:تم تحويل|transferred|استقبال)\s*(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP|LE)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from)\s*(\d{11})""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:كود|code|ref)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""VF[\s-]?(\d+)""", RegexOption.IGNORE_CASE)
        ),
        
        // Orange Cash patterns
        PaymentPattern(
            walletType = PaymentWalletType.ORANGE_CASH,
            amountRegex = Regex("""(?:تم استقبال|received)\s*(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from)\s*(\d{11})""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:مرجع|reference)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""OR[\s-]?(\d+)""", RegexOption.IGNORE_CASE)
        ),
        
        // Etisalat Cash patterns
        PaymentPattern(
            walletType = PaymentWalletType.ETISALAT_CASH,
            amountRegex = Regex("""(?:تم تحويل|received)\s*(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from)\s*(\d{11})""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:رقم العملية|ref)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""ET[\s-]?(\d+)""", RegexOption.IGNORE_CASE)
        ),
        
        // Fawry patterns
        PaymentPattern(
            walletType = PaymentWalletType.FAWRY,
            amountRegex = Regex("""(?:تم دفع|paid|استقبال)\s*(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from)\s*([^\s]+)""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:كود الدفع|payment code|ref)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""FW[\s-]?(\d+)""", RegexOption.IGNORE_CASE)
        ),
        
        // Generic bank transfer patterns
        PaymentPattern(
            walletType = PaymentWalletType.UNKNOWN,
            amountRegex = Regex("""(\d+(?:\.\d{2})?)\s*(?:جنيه|EGP|LE|pound)""", RegexOption.IGNORE_CASE),
            senderRegex = Regex("""(?:من|from|sender)[\s:]*([^\n\r]+)""", RegexOption.IGNORE_CASE),
            referenceRegex = Regex("""(?:مرجع|ref|reference|رقم)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            transactionIdRegex = Regex("""(?:رقم العملية|transaction|trans|ID)[\s:]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
        )
    )
    
    fun parsePayment(messageText: String, sender: String, messageId: String): PaymentInfo? {
        // First detect wallet type from sender
        val detectedWallet = PaymentWalletType.detectFromSender(sender)
        
        // Find matching pattern
        val pattern = paymentPatterns.find { it.walletType == detectedWallet }
            ?: paymentPatterns.find { it.walletType == PaymentWalletType.UNKNOWN }
            ?: return null
        
        // Extract amount
        val amountMatch = pattern.amountRegex.find(messageText) ?: return null
        val amount = try {
            BigDecimal(amountMatch.groupValues[1])
        } catch (e: NumberFormatException) {
            return null
        }
        
        // Extract other information
        val senderName = pattern.senderRegex?.find(messageText)?.groupValues?.get(1)
        val reference = pattern.referenceRegex?.find(messageText)?.groupValues?.get(1)
        val transactionId = pattern.transactionIdRegex?.find(messageText)?.groupValues?.get(1)
        
        // Extract phone number if sender name looks like a phone number
        val senderPhone = if (senderName?.matches(Regex("""\d{10,11}""")) == true) {
            senderName
        } else null
        
        return PaymentInfo(
            id = UUID.randomUUID().toString(),
            walletType = if (detectedWallet != PaymentWalletType.UNKNOWN) detectedWallet else pattern.walletType,
            amount = amount,
            senderName = senderName,
            senderPhone = senderPhone,
            reference = reference,
            transactionId = transactionId,
            description = extractDescription(messageText),
            timestamp = Date(),
            rawMessage = messageText,
            messageId = messageId
        )
    }
    
    private fun extractDescription(messageText: String): String? {
        val descriptionPatterns = listOf(
            Regex("""(?:وصف|description|memo)[\s:]*([^\n\r]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:تفاصيل|details)[\s:]*([^\n\r]+)""", RegexOption.IGNORE_CASE)
        )
        
        return descriptionPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(messageText)?.groupValues?.get(1)?.trim()
        }
    }
    
    fun isPaymentMessage(messageText: String, sender: String): Boolean {
        val walletType = PaymentWalletType.detectFromSender(sender)
        if (walletType == PaymentWalletType.UNKNOWN) {
            // Check for generic payment keywords
            val paymentKeywords = listOf(
                "تم استلام", "تم تحويل", "تم دفع", "received", "transferred", "paid",
                "استقبال", "تحويل", "دفع", "payment", "transfer", "جنيه", "EGP", "LE"
            )
            return paymentKeywords.any { keyword ->
                messageText.contains(keyword, ignoreCase = true)
            }
        }
        
        // If wallet is detected, check for amount pattern
        val pattern = paymentPatterns.find { it.walletType == walletType }
        return pattern?.amountRegex?.containsMatchIn(messageText) == true
    }
}