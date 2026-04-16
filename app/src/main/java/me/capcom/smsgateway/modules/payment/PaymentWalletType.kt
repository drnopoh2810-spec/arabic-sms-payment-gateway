package me.capcom.smsgateway.modules.payment

enum class PaymentWalletType(val displayName: String, val senderPatterns: List<String>) {
    INSTAPAY("InstaPay", listOf("InstaPay", "انستاباي", "INSTAPAY")),
    VODAFONE_CASH("Vodafone Cash", listOf("Vodafone", "فودافون", "VODAFONE", "VF-Cash")),
    ORANGE_CASH("Orange Cash", listOf("Orange", "أورانج", "ORANGE")),
    ETISALAT_CASH("Etisalat Cash", listOf("Etisalat", "اتصالات", "ETISALAT")),
    FAWRY("Fawry", listOf("Fawry", "فوري", "FAWRY")),
    WE_PAY("WE Pay", listOf("WE", "وي", "WE-Pay")),
    BANK_MISR("Bank Misr", listOf("BankMisr", "بنك مصر", "BANKMISR")),
    NBE("National Bank of Egypt", listOf("NBE", "الأهلي المصري", "National")),
    CIB("Commercial International Bank", listOf("CIB", "التجاري الدولي", "COMMERCIAL")),
    UNKNOWN("Unknown", emptyList());

    companion object {
        fun detectFromSender(sender: String): PaymentWalletType {
            return values().find { wallet ->
                wallet.senderPatterns.any { pattern ->
                    sender.contains(pattern, ignoreCase = true)
                }
            } ?: UNKNOWN
        }
    }
}