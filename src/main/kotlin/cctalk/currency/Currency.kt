package cctalk.currency

import kotlin.math.pow

data class CoinValue(
  val id: String,
  val value: Double,
  val euroCent: Int
) {
  val intValue: Int
    get() = (value * 10.0.pow(euroCent.toDouble())).toInt()
}

data class ValueFactor(val factor: Char, val face: Double) {
  companion object Search {
    fun fromChar(factor: Char): ValueFactor? = VALUE_FACTORS.find { it.factor == factor }
  }
}

val VALUE_FACTORS = listOf(
  ValueFactor('m', 10.0.pow(-3.0)),
  ValueFactor(' ', 10.0.pow(0.0)),
  ValueFactor('.', 10.0.pow(0.0)),
  ValueFactor('K', 10.0.pow(3.0)),
  ValueFactor('M', 10.0.pow(6.0)),
  ValueFactor('G', 10.0.pow(9.0))
)

data class Currency(
  val description: String,
  val isoCode: String,
  val ccTalkID: String,
  val mdbCode: Int,
  val decimals: Int,
  val jcmCode: Int,
  val dialingCode: Int
) {
  companion object Search {
    fun byIsoCode(isoCode: String): Currency? = CURRENCIES.find { it.isoCode == isoCode }
    fun byCcTalkID(ccTalkID: String): Currency? = CURRENCIES.find { it.ccTalkID == ccTalkID }
    fun byMdbCode(mdbCode: Int): Currency? = CURRENCIES.find { it.mdbCode == mdbCode }
    fun byJcmCode(jcmCode: Int): Currency? = CURRENCIES.find { it.jcmCode == jcmCode }
  }
}

val CURRENCIES = listOf<Currency>(
  Currency("Albanian Lek", "ALL", "AL", 0x1008, 2, 0x77, 0x0355),
  Currency("Algerian Dinar", "DZD", "DZ", 0x1012, 2, 0x00, 0x0213),
  Currency("Antillian Guilder", "ANG", "AN", 0x1532, 2, 0x00, 0x0000),
  Currency("Argentine Peso", "ARS", "AR", 0x1032, 2, 0x0e, 0x0054),
  Currency("Australian Dollar", "AUD", "AU", 0x1036, 2, 0x02, 0x0061),
  Currency("Azerbaijanian Manat - New", "AZN", "AZ", 0x1944, 2, 0x00, 0x0994),
  Currency("Bahraini Dinar", "BHD", "BH", 0x1048, 3, 0x00, 0x0973),
  Currency("Bermudian Dollar", "BMD", "BM", 0x1060, 2, 0x00, 0x0000),
  Currency("Bolivian Boliviano", "BOB", "BO", 0x1068, 2, 0x00, 0x0591),
  Currency("Bosnia-Herzegovina Convert. Marks", "BAM", "BA", 0x1977, 2, 0x75, 0x0387),
  Currency("Brazilian Real", "BRL", "BR", 0x1986, 2, 0x00, 0x0055),
  Currency("Bulgarian Lev", "BGL", "BG", 0x1100, 2, 0x5d, 0x0359),
  Currency("Canadian Dollar", "CAD", "CA", 0x1124, 2, 0x08, 0x0000),
  Currency("Chilean Peso", "CLP", "CL", 0x1152, 2, 0x4e, 0x0056),
  Currency("Chinese Yuan Renminbi", "CNY", "CN", 0x1156, 2, 0x2d, 0x0086),
  Currency("Colombian Peso", "COP", "CO", 0x1170, 2, 0x19, 0x0057),
  Currency("Costa Rican Colon", "CRC", "CR", 0x1188, 2, 0x4d, 0x0506),
  Currency("Croatian Kuna", "HRK", "HR", 0x1191, 2, 0x6e, 0x0000),
  Currency("Cyprus Pound", "CYP", "CY", 0x1196, 2, 0x62, 0x0357),
  Currency("Czech Koruna", "CZK", "CZ", 0x1203, 2, 0x2c, 0x0420),
  Currency("Danish Krone", "DKK", "DK", 0x1208, 2, 0x3a, 0x0045),
  Currency("Dominican Peso", "DOP", "DO", 0x1214, 2, 0x00, 0x0000),
  Currency("Egyptian Pound", "EGP", "EG", 0x1818, 2, 0x00, 0x0020),
  Currency("Estonian Kroon", "EEK", "EE", 0x1233, 2, 0x24, 0x0372),
  Currency("Euro", "EUR", "EU", 0x1978, 2, 0xe0, 0x0049),
  Currency("Fiji Dollar", "FJD", "FJ", 0x1242, 2, 0x00, 0x0679),
  Currency("Georgian Lari", "GEL", "GE", 0x1981, 2, 0x76, 0x0995),
  Currency("Ghanaian Cedi - New", "GHS", "GH", 0x1936, 2, 0x97, 0x0233),
  Currency("Ghanaian Cedi - Old", "GHC", "GH", 0x1288, 2, 0x97, 0x0000),
  Currency("Hong Kong Dollar", "HKD", "HK", 0x1344, 2, 0x59, 0x0852),
  Currency("Hungarian Forint", "HUF", "HU", 0x1348, 2, 0x30, 0x0036),
  Currency("Iceland Krona", "ISK", "IS", 0x1352, 2, 0x49, 0x0354),
  Currency("Indian Rupee", "INR", "IN", 0x1356, 2, 0x63, 0x0091),
  Currency("Israeli Sheqel", "ILS", "IL", 0x1376, 2, 0x58, 0x0972),
  Currency("Jamaican Dollar", "JMD", "JM", 0x1388, 2, 0x00, 0x0000),
  Currency("Japanese Yen", "JPY", "JP", 0x1392, 0, 0x0a, 0x0081),
  Currency("Jordanian Dinar", "JOD", "JO", 0x1400, 2, 0x00, 0x0962),
  Currency("Kazakhstan Tenge", "KZT", "KZ", 0x1398, 2, 0x51, 0x0007),
  Currency("Kenyan Shilling", "KES", "KE", 0x1404, 2, 0x00, 0x0254),
  Currency("Latvian Lats", "LVL", "LV", 0x1428, 2, 0x46, 0x0371),
  Currency("Lebanese Pound", "LBP", "LB", 0x1422, 2, 0x00, 0x0961),
  Currency("Lithuanian Litas", "LTL", "LT", 0x1440, 2, 0x4f, 0x0370),
  Currency("Macau Pataca", "MOP", "MO", 0x1446, 2, 0x00, 0x0853),
  Currency("Macedonian Denar", "MKD", "MK", 0x1807, 2, 0x73, 0x0389),
  Currency("Malaysian Ringgit", "MYR", "MY", 0x1458, 2, 0x21, 0x0060),
  Currency("Maltese Lira", "MTL", "MT", 0x1470, 2, 0x61, 0x0356),
  Currency("Mauritius Rupee", "MUR", "MU", 0x1480, 2, 0x47, 0x0230),
  Currency("Mexican Peso", "MXN", "MX", 0x1484, 2, 0x09, 0x0052),
  Currency("Moroccan Dirham", "MAD", "MA", 0x1504, 2, 0x6c, 0x0212),
  Currency("Namibia Dollar", "NAD", "NA", 0x1516, 2, 0x3b, 0x0000),
  Currency("New Caledonian Franc", "XPF", "XP", 0x1953, 0, 0x00, 0x0687),
  Currency("New Zealand Dollar", "NZD", "NZ", 0x1554, 2, 0x0d, 0x0064),
  Currency("Norvegian Crown", "NOK", "NO", 0x1578, 2, 0x07, 0x0000),
  Currency("Pacific Franc", "XPF", "PF", 0x1953, 2, 0x00, 0x0000),
  Currency("Panama Balboa", "PAB", "PA", 0x1590, 2, 0x00, 0x0507),
  Currency("Peruvian Nuevo Sol", "PEN", "PE", 0x1604, 2, 0x2f, 0x0051),
  Currency("Philippine Peso", "PHP", "PH", 0x1608, 2, 0x4a, 0x0063),
  Currency("Polish Zloty", "PLN", "PL", 0x1985, 2, 0x1a, 0x0048),
  Currency("Pound Sterling", "GBP", "GB", 0x1826, 2, 0x17, 0x0044),
  Currency("Qatari Rial", "QAR", "QA", 0x1634, 2, 0x42, 0x0974),
  Currency("Rial Omani", "OMR", "OM", 0x1512, 3, 0x7d, 0x0968),
  Currency("Romanian Leu - New", "RON", "RO", 0x1946, 2, 0x4c, 0x0040),
  Currency("Romanian Leu - Old", "ROL", "RO", 0x1642, 2, 0x4c, 0x0000),
  Currency("Russian Ruble", "RUB", "RU", 0x1810, 2, 0x27, 0x0007),
  Currency("Serbia and Montenegro New Dinar", "YUM", "YU", 0x1891, 2, 0x74, 0x0381),
  Currency("Singapore Dollar", "SGD", "SG", 0x1702, 2, 0x22, 0x0065),
  Currency("Slovakian Crown", "SKK", "SK", 0x1703, 2, 0x41, 0x0421),
  Currency("Slovenian Tolar", "SIT", "SI", 0x1705, 2, 0x53, 0x0000),
  Currency("South African Rand", "ZAR", "ZA", 0x1710, 2, 0x06, 0x0027),
  Currency("Swedish Crown", "SEK", "SE", 0x1752, 2, 0x05, 0x0046),
  Currency("Swiss Franc", "CHF", "CH", 0x1756, 2, 0x16, 0x0041),
  Currency("Taiwan Dollar", "TWD", "TW", 0x1901, 2, 0x1d, 0x0886),
  Currency("Tajikistani Somoni", "TJS", "TJ", 0x1972, 2, 0x00, 0x0992),
  Currency("Thailandian Baht", "THB", "TH", 0x1764, 2, 0x12, 0x0066),
  Currency("Tunisian Dinar", "TND", "TN", 0x1788, 3, 0x00, 0x0216),
  Currency("Turkish Lira - New", "YTL", "TY", 0x1949, 2, 0x7b, 0x0090),
  Currency("Turkish Lira - Old", "TRL", "TR", 0x1792, 2, 0x7b, 0x0000),
  Currency("Ukrainian Hryvnia", "UAH", "UA", 0x1980, 2, 0x5c, 0x0380),
  Currency("United Arab Emirates Dirham", "AED", "AE", 0x1784, 2, 0x1c, 0x0971),
  Currency("US Dollar", "USD", "US", 0x1840, 2, 0x01, 0x0001),
  Currency("Venezuelan Bolivar", "VEB", "VE", 0x1862, 2, 0x1f, 0x0058)
)