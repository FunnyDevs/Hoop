package app.hoop.presentation.home

/** Sample photos used while there is no real media upload. Keys are what the fake backend stores in a post. */
internal object SampleMedia {
    const val SUNSET = "sample:sunset"
    const val CONCERT = "sample:concert"
    const val MARKET = "sample:market"
    const val MIME_TYPE = "image/jpeg"

    val pickerKeys = listOf(SUNSET, MARKET, CONCERT)

    fun keyAt(index: Int): String = pickerKeys[index.coerceIn(0, pickerKeys.lastIndex)]
}
