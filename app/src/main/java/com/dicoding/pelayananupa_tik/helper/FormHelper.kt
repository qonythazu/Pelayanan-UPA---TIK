package com.dicoding.pelayananupa_tik.helper

fun isValidPhoneNumber(phoneNumber: String): Boolean {

    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
    if (digitsOnly.length < 10 || digitsOnly.length > 15) {
        return false
    }
    return when {
        digitsOnly.startsWith("62") -> {
            val localNumber = digitsOnly.substring(2)
            isValidIndonesianLocalNumber(localNumber)
        }
        digitsOnly.startsWith("0") -> {
            val localNumber = digitsOnly.substring(1)
            isValidIndonesianLocalNumber(localNumber)
        }
        else -> {
            isValidIndonesianLocalNumber(digitsOnly)
        }
    }
}

fun isValidIndonesianLocalNumber(localNumber: String): Boolean {
    if (localNumber.length < 9 || localNumber.length > 13) {
        return false
    }
    val validPrefixes = listOf(
        // Telkomsel
        "811", "812", "813", "821", "822", "823", "851", "852", "853",
        // Indosat
        "814", "815", "816", "855", "856", "857", "858",
        // XL
        "817", "818", "819", "859", "877", "878",
        // Tri (3)
        "895", "896", "897", "898", "899",
        // Smartfren
        "881", "882", "883", "884", "885", "886", "887", "888", "889",
        // Axis
        "831", "832", "833", "838",
        // Telkom (PSTN)
        "21", "22", "24", "31", "341", "343", "361", "370", "380", "401", "411", "421", "431", "451", "471", "481", "511", "541", "561", "571", "601", "620", "651", "717", "721", "741", "751", "761", "771", "778"
    )
    return validPrefixes.any { prefix -> localNumber.startsWith(prefix) }
}