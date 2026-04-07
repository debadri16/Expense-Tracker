package com.expensetracker.app.data.sms

import com.expensetracker.app.data.db.Classification
import com.expensetracker.app.data.db.TransactionEntity

data class RawSms(
    val address: String,
    val body: String,
    val dateMillis: Long
)

data class ParseResult(
    val transactions: List<TransactionEntity>,
    val matched: Int,
    val skipped: Int
)

object SmsParser {

    // Indian bank SMS senders: prefix[-_]BANKCODE[-suffix]
    // e.g. VM-HDFCBK, JD_HDFCBK-S, AD-ICICIT-S, AX-SBIBNK
    private val BANK_SENDER_PATTERN = Regex(
        """^[A-Z]{2}[-_][A-Z0-9]{4,9}(?:-[A-Z0-9]{1,3})?$""", RegexOption.IGNORE_CASE
    )

    private val BANK_SENDER_HINTS = listOf(
        "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOI", "CANARA",
        "YESBNK", "IDFC", "RBL", "CITI", "AMEX", "INDUS", "FEDER", "AUBANK",
        "BOB", "UNION", "IDBI", "PAYTM", "JIOFI", "HSBC", "SC", "BARB",
        "MAHB", "SBIN", "IDFCFB", "HDFCBK", "ICICIB", "ICICIT", "AXISBK", "KOTAKB"
    )

    fun isBankSender(address: String): Boolean {
        val cleaned = address.trim()
        if (!BANK_SENDER_PATTERN.matches(cleaned)) return false
        val code = cleaned.substringAfter("-").substringAfter("_")
            .removeSuffix("-S").removeSuffix("-s").uppercase()
        return BANK_SENDER_HINTS.any { code.contains(it) }
    }

    private val AMOUNT_PATTERN = Regex(
        """(?:Rs\.?|INR\.?|₹)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE
    )
    private val CARD_PATTERN = Regex(
        """(?:card|a/c|ac|acct|account|xx|ending)\s*(?:no\.?\s*)?[xX*]*(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "withdrawn", "paid", "purchase",
        "transferred", "sent", "charged", "deducted"
    )
    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "refund", "cashback", "deposited"
    )

    private val CATEGORY_KEYWORDS = mapOf(
        "food" to listOf("swiggy", "zomato", "restaurant", "food", "cafe", "pizza", "burger", "dining"),
        "grocery" to listOf("bigbasket", "grofers", "blinkit", "dmart", "grocery", "supermarket", "zepto"),
        "shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "mall", "store", "shop"),
        "travel" to listOf("uber", "ola", "rapido", "irctc", "railway", "flight", "makemytrip", "goibibo"),
        "fuel" to listOf("petrol", "diesel", "fuel", "hp ", "iocl", "bpcl", "shell"),
        "bills" to listOf("electricity", "water", "gas", "broadband", "jio", "airtel", "vi ", "bsnl", "recharge"),
        "entertainment" to listOf("netflix", "hotstar", "prime", "spotify", "movie", "pvr", "inox"),
        "medical" to listOf("pharmacy", "medical", "hospital", "doctor", "apollo", "medplus", "1mg"),
        "transfer" to listOf("neft", "imps", "transfer", "sent to"),
        "emi" to listOf("emi", "loan", "instalment"),
        "atm" to listOf("atm", "cash withdrawal"),
    )

    private val CC_BILL_KEYWORDS = listOf(
        "credit card bill", "cc bill", "card outstanding", "credit card payment",
        "towards credit card", "towards your card", "card bill", "bill payment"
    )
    private val CC_RECEIVED_KEYWORDS = listOf(
        "payment received", "received towards", "thank you for payment"
    )
    private val REFUND_KEYWORDS = listOf("refund", "reversed", "reversal", "cashback", "cash back")
    private val EMI_KEYWORDS = listOf("emi", "instalment", "installment", "loan emi")
    private val INCOME_KEYWORDS = listOf("salary", "bonus", "wages")
    private val CC_SENDER_HINTS = listOf("CRD", "CARD", "AMEX", "CC")

    private val BANK_PATTERNS = mapOf(
        "HDFC" to Regex("""hdfc""", RegexOption.IGNORE_CASE),
        "SBI" to Regex("""\bsbi\b|state bank""", RegexOption.IGNORE_CASE),
        "ICICI" to Regex("""icici""", RegexOption.IGNORE_CASE),
        "Axis" to Regex("""\baxis\b""", RegexOption.IGNORE_CASE),
        "Kotak" to Regex("""kotak""", RegexOption.IGNORE_CASE),
        "PNB" to Regex("""\bpnb\b|punjab national""", RegexOption.IGNORE_CASE),
        "BOI" to Regex("""\bboi\b|bank of india""", RegexOption.IGNORE_CASE),
        "Canara" to Regex("""canara""", RegexOption.IGNORE_CASE),
        "Yes Bank" to Regex("""yes bank""", RegexOption.IGNORE_CASE),
        "IDFC" to Regex("""idfc""", RegexOption.IGNORE_CASE),
        "RBL" to Regex("""\brbl\b""", RegexOption.IGNORE_CASE),
        "Citi" to Regex("""\bciti\b""", RegexOption.IGNORE_CASE),
        "Amex" to Regex("""amex|american express""", RegexOption.IGNORE_CASE),
        "IndusInd" to Regex("""indusind""", RegexOption.IGNORE_CASE),
        "Federal" to Regex("""federal bank""", RegexOption.IGNORE_CASE),
        "AU" to Regex("""\bau bank\b|au small""", RegexOption.IGNORE_CASE),
        "BoB" to Regex("""bank of baroda|\bbob\b""", RegexOption.IGNORE_CASE),
    )

    fun processBatch(smsList: List<RawSms>): ParseResult {
        val transactions = mutableListOf<TransactionEntity>()
        val seenKeys = mutableSetOf<String>()
        var matched = 0

        for (sms in smsList) {
            val txn = parse(sms) ?: continue
            if (txn.dedupKey in seenKeys) continue
            seenKeys.add(txn.dedupKey)
            transactions.add(txn)
            matched++
        }

        return ParseResult(
            transactions = transactions,
            matched = matched,
            skipped = smsList.size - matched
        )
    }

    private val OTP_PATTERN = Regex(
        """\b(?:otp|one.?time.?password|verification.?code|security.?code|pin)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(sms: RawSms): TransactionEntity? {
        if (!isBankSender(sms.address)) return null

        val body = sms.body
        val bodyLower = body.lowercase()

        if (OTP_PATTERN.containsMatchIn(body)) return null

        val amount: Double = AMOUNT_PATTERN.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        val hasDebit = DEBIT_KEYWORDS.any { bodyLower.contains(it) }
        val hasCredit = CREDIT_KEYWORDS.any { bodyLower.contains(it) }
        if (!hasDebit && !hasCredit) return null

        val type = when {
            hasDebit && !hasCredit -> "debit"
            hasCredit && !hasDebit -> "credit"
            else -> {
                val firstDebit = DEBIT_KEYWORDS.mapNotNull { bodyLower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                val firstCredit = CREDIT_KEYWORDS.mapNotNull { bodyLower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                if (firstDebit < firstCredit) "debit" else "credit"
            }
        }

        val cardLast4 = CARD_PATTERN.find(body)?.groupValues?.get(1) ?: ""
        val bank = detectBank(bodyLower, sms.address)
        val merchant = extractMerchant(bodyLower)
        val category = detectCategory(bodyLower)
        val classification = classify(bodyLower, type, sms.address.uppercase())
        val dedupKey = TransactionEntity.buildDedupKey(bank, cardLast4, amount, sms.dateMillis, type)

        return TransactionEntity(
            amount = amount,
            type = type,
            classification = classification,
            bank = bank,
            cardLast4 = cardLast4,
            category = category,
            merchant = merchant,
            date = sms.dateMillis,
            smsBody = body,
            smsAddress = sms.address,
            templateId = "",
            needsReview = false,
            dedupKey = dedupKey
        )
    }

    private fun detectBank(bodyLower: String, senderAddress: String): String {
        for ((bank, pattern) in BANK_PATTERNS) {
            if (pattern.containsMatchIn(bodyLower)) return bank
        }
        val upper = senderAddress.uppercase().replace(Regex("[^A-Z]"), "")
        if (upper.length >= 3) return upper
        return ""
    }

    private fun classify(bodyLower: String, type: String, senderUpper: String): String {
        val isCcSender = CC_SENDER_HINTS.any { senderUpper.contains(it) }

        if (type == "debit" && CC_BILL_KEYWORDS.any { bodyLower.contains(it) })
            return Classification.CC_PAYMENT
        if (type == "credit" && CC_RECEIVED_KEYWORDS.any { bodyLower.contains(it) })
            return Classification.CC_RECEIVED
        if (type == "credit" && isCcSender && bodyLower.contains("payment"))
            return Classification.CC_RECEIVED
        if (type == "credit" && REFUND_KEYWORDS.any { bodyLower.contains(it) })
            return Classification.REFUND
        if (type == "debit" && EMI_KEYWORDS.any { bodyLower.contains(it) })
            return Classification.EMI
        if (type == "credit" && INCOME_KEYWORDS.any { bodyLower.contains(it) })
            return Classification.INCOME

        return if (type == "debit") Classification.EXPENSE else Classification.INCOME
    }

    private fun detectCategory(bodyLower: String): String {
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (keywords.any { bodyLower.contains(it) }) return category
        }
        return "other"
    }

    private val MERCHANT_END = """(?=\s+on\s+\d|\s+on\s+[a-zA-Z]|\s+ref\b|\s+txn\b|\s+utr\b|\s+dated\b|\s*\.\s*[A-Z]|\s+avl\b|\s+bal\b|\s*,\s*[A-Z]|\s*$)"""

    private val DATE_LIKE = Regex("""^\d{1,2}[-/\s]""")

    private val MERCHANT_PATTERNS = listOf(
        Regex("""on\s+\d{1,2}[-/\s]?\w{3,9}[-/\s]?\d{0,4}\s+(?:on|at)\s+(.+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:at|via)\s+(.+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""on\s+([a-zA-Z].+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:spent|purchase[d]?|charged|used)\s+.*?(?:on|at)\s+([a-zA-Z].+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|paid|sent)\s+.*?(?:for|to)\s+(.+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:to|for)\s+(.+?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""(?:vpa|upi)\s*:?\s*([a-zA-Z0-9._]+)@""", RegexOption.IGNORE_CASE),
        Regex("""(?:at|to|for|via|on)\s+([a-zA-Z][a-zA-Z0-9\s&'.\-]{1,29})""", RegexOption.IGNORE_CASE),
    )

    private val MERCHANT_NOISE = setOf(
        "on", "for", "at", "the", "your", "a", "an", "is", "was", "with", "from",
        "rs", "inr", "and", "of", "to", "in", "by", "has", "been"
    )

    private fun extractMerchant(bodyLower: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            for (match in pattern.findAll(bodyLower)) {
                var merchant = match.groupValues[1].trim()
                if (DATE_LIKE.containsMatchIn(merchant)) continue
                merchant = merchant.replace(Regex("""\s+"""), " ")
                val cleaned = merchant.split(" ")
                    .dropLastWhile { it.lowercase() in MERCHANT_NOISE || it.length <= 1 }
                    .joinToString(" ")
                    .trim()
                if (cleaned.length >= 2) {
                    return cleaned.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                }
            }
        }
        return ""
    }
}
