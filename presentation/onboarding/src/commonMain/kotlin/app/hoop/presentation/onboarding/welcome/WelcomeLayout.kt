package app.hoop.presentation.onboarding.welcome

/**
 * Collage measurements in the design reference system (390 x 844 screen).
 * They are scaled to the real screen: no scattered numbers in the screen code.
 */
internal object WelcomeLayout {
    const val REFERENCE_WIDTH = 390f
    const val COLLAGE_TOP = 92f
    const val COLLAGE_HEIGHT = 368f
    const val FADE_START = 330f
    const val FADE_HEIGHT = 270f
    const val FADE_SOLID_START = 600f
    const val FADE_SOLID_STOP = 0.62f
    const val BOTTOM_BLOCK_RESERVE = 420f
    const val MIN_SCALE = 0.5f

    enum class CardKind { SunsetPhoto, Question, Video, MarketPhoto }

    class Card(
        val kind: CardKind,
        val x: Float, val y: Float, val width: Float, val height: Float,
        val rotation: Float, val floatDelayMillis: Int,
    )

    val cards = listOf(
        Card(CardKind.SunsetPhoto, x = 22f, y = 92f, width = 170f, height = 216f, rotation = -7f, floatDelayMillis = 0),
        Card(CardKind.Question, x = 196f, y = 70f, width = 174f, height = 132f, rotation = 5f, floatDelayMillis = 600),
        Card(CardKind.Video, x = 216f, y = 214f, width = 150f, height = 200f, rotation = 4f, floatDelayMillis = 1200),
        Card(CardKind.MarketPhoto, x = 30f, y = 330f, width = 150f, height = 130f, rotation = -4f, floatDelayMillis = 1800),
    )
}
