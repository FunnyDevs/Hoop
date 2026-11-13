package app.hoop.data.fake

import app.hoop.domain.FeedRepository
import app.hoop.domain.HoopsRepository
import app.hoop.domain.PostsRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.identity.NotificationKind
import app.hoop.domain.identity.UserId
import app.hoop.domain.post.Author
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import app.hoop.domain.post.Reply
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * In-memory backend for Hoops, feeds and posts, with sample content, used while the server is not available.
 * Sample texts are fixtures, not user interface strings.
 */
internal class FakeHomeStore : HoopsRepository, FeedRepository, PostsRepository {

    private class Seed(
        val id: String, val type: PostType, val author: String, val text: String, val mediaKey: String?,
        val dxMeters: Int, val dyMeters: Int, val ageMinutes: Int, val replies: MutableList<Pair<String, String>>,
    )

    private var hoops: UserHoops = UserHoops(
        home = Hoop.Home(HoopId(HOME_ID), Radius.Default),
        extras = listOf(
            extra("work", "Lavoro", 45.4781, 9.2275, 1_200),
            extra("turin", "Torino", 45.0703, 7.6869, 2_000),
        ),
    )

    private val seeds: MutableMap<String, MutableList<Seed>> = mutableMapOf(
        HOME_ID to mutableListOf(
            Seed("c1", PostType.Question, "Marta", "La farmacia di via Tadino fa il turno di notte?", null, -80, -90, 4,
                mutableListOf("Gianni" to "Sì, ieri sera la luce era accesa.", "Rosa" to "Dipende dal giorno, meglio chiamare.", "Tommaso" to "Ci passo adesso e ti scrivo.")),
            Seed("c2", PostType.Photo, "Luca", "Tramonto dal ponte della ferrovia", "sample:sunset", 250, -180, 20,
                mutableListOf("Sara" to "Che luce!", "Paolo" to "Domani ci torno.")),
            Seed("c3", PostType.Video, "Giulia", "Concerto in piazza Duca d'Aosta, venite!", "sample:concert", 150, 440, 12,
                mutableListOf("Marco" to "Arrivo tra dieci minuti.", "Anna" to "Si sente fino a casa mia.")),
            Seed("c4", PostType.Question, "Paolo", "Qualcuno ha trovato un gatto grigio in via Lecco?", null, -520, 320, 60,
                mutableListOf("Elena" to "Ne ho visto uno vicino al fruttivendolo.")),
            Seed("c5", PostType.Photo, "Sara", "Pesce fresco al mercato di via Marcello", "sample:market", 620, 560, 120,
                mutableListOf("Luca" to "Ci vado subito.")),
        ),
        "work" to mutableListOf(
            Seed("l1", PostType.Question, "Anna", "La mensa di via Cavour è aperta il sabato?", null, -120, -140, 8, mutableListOf("Davide" to "No, solo dal lunedì al venerdì.")),
            Seed("l2", PostType.Photo, "Davide", "Nuova pista ciclabile lungo il Lambro", "sample:sunset", 380, 260, 35, mutableListOf()),
            Seed("l3", PostType.Question, "Elena", "Un caffè tranquillo vicino alla stazione?", null, -480, 300, 60, mutableListOf("Anna" to "Il bar sotto i portici.")),
        ),
        "turin" to mutableListOf(
            Seed("t1", PostType.Question, "Irene", "Dove si vede bene la partita stasera?", null, -200, -240, 3, mutableListOf()),
            Seed("t2", PostType.Question, "Marco", "Parcheggio libero vicino al Politecnico?", null, 700, 500, 25, mutableListOf("Chiara" to "Di solito in corso Duca degli Abruzzi.")),
            Seed("t3", PostType.Question, "Chiara", "Una buona pizzeria in zona Vanchiglia?", null, -650, 700, 120, mutableListOf()),
        ),
    )

    private var nextId = 0
    private val newReplies = mutableMapOf<String, MutableList<Reply>>()

    // ---- HoopsRepository ----
    override suspend fun getUserHoops(): DomainResult<UserHoops> = DomainResult.success(hoops)

    override suspend fun addHoop(name: String, center: GeoPoint, radius: Radius): DomainResult<UserHoops> {
        val id = "hoop-${++nextId}"
        val added = hoops.add(Hoop.Extra(HoopId(id), name, center, radius))
        if (added is DomainResult.Success) {
            hoops = added.value
            seeds[id] = mutableListOf(
                Seed("$id-1", PostType.Question, "Luca", "Cosa c'è di bello da vedere qui vicino?", null, -200, -200, 5, mutableListOf()),
                Seed("$id-2", PostType.Question, "Sofia", "Qualcuno conosce un posto per pranzare?", null, 250, 250, 60, mutableListOf()),
            )
        }
        return added
    }

    override suspend fun removeHoop(id: HoopId): DomainResult<UserHoops> = hoops.remove(id).also {
        if (it is DomainResult.Success) { hoops = it.value; seeds.remove(id.value) }
    }

    override suspend fun resizeHoop(id: HoopId, radius: Radius): DomainResult<UserHoops> = hoops.resize(id, radius).also {
        if (it is DomainResult.Success) hoops = it.value
    }

    // ---- FeedRepository ----
    override suspend fun getFeed(hoop: Hoop, position: GeoPoint): DomainResult<List<FeedPost>> {
        delay(LATENCY_MILLIS)
        val center = (hoop as? Hoop.Extra)?.center ?: position
        val posts = seeds[hoop.id.value].orEmpty().map { seed ->
            val location = offset(center, seed.dxMeters, seed.dyMeters)
            FeedPost(
                id = PostId(seed.id), hoopId = hoop.id, type = seed.type,
                author = Author(UserId(seed.author.lowercase()), seed.author), text = seed.text, mediaKey = seed.mediaKey,
                location = location, distanceMeters = hypot(seed.dxMeters.toDouble(), seed.dyMeters.toDouble()).roundToInt(),
                ageMinutes = seed.ageMinutes,
                replies = seed.replies.mapIndexed { i, r -> Reply("${seed.id}-r$i", Author(UserId(r.first.lowercase()), r.first), r.second, ageMinutes = seed.ageMinutes / 2 + i * 3, fliesCount = (i * 7 + seed.id.length) % 5) } +
                    newReplies[seed.id].orEmpty(),
                street = STREETS[seed.id] ?: DEFAULT_STREET, fliesCount = FLIES[seed.id] ?: 0,
            )
        }
        return DomainResult.success(posts)
    }

    override suspend fun addReply(postId: PostId, text: String): DomainResult<Reply> {
        val reply = Reply("${postId.value}-new${++nextId}", Author(UserId(ME_ID), ME_NAME), text)
        newReplies.getOrPut(postId.value) { mutableListOf() }.add(reply)
        return DomainResult.success(reply)
    }

    override suspend fun getNotifications(): DomainResult<List<AppNotification>> = DomainResult.success(
        listOf(
            AppNotification("n1", NotificationKind.RepliesReceived, "Marta", PostId("c1"), HoopId(HOME_ID), count = 3, distanceMeters = 0),
            AppNotification("n2", NotificationKind.VideoPublished, "Giulia", PostId("c3"), HoopId(HOME_ID), count = 0, distanceMeters = 480),
            AppNotification("n3", NotificationKind.QuestionPublished, "Paolo", PostId("c4"), HoopId(HOME_ID), count = 0, distanceMeters = 700),
        ),
    )

    // ---- PostsRepository ----
    override suspend fun publish(draft: PostDraft): DomainResult<PostId> {
        val list = seeds[draft.hoopId.value] ?: return DomainResult.failure(Violation.HoopNotFound)
        val id = "u${++nextId}"
        val spot = NEW_POST_OFFSETS[list.size % NEW_POST_OFFSETS.size]
        list.add(0, Seed(id, draft.type, ME_NAME, draft.text?.value.orEmpty(), draft.media?.uri, spot.first, spot.second, 0, mutableListOf()))
        return DomainResult.success(PostId(id))
    }

    private fun extra(id: String, name: String, lat: Double, lon: Double, radius: Int): Hoop.Extra {
        val point = (GeoPoint.create(lat, lon) as DomainResult.Success).value
        val r = (Radius.create(radius) as DomainResult.Success).value
        return Hoop.Extra(HoopId(id), name, point, r)
    }

    private fun offset(center: GeoPoint, dxMeters: Int, dyMeters: Int): GeoPoint {
        val lat = center.latitude - dyMeters / METERS_PER_DEGREE
        val lon = center.longitude + dxMeters / (METERS_PER_DEGREE * cos(center.latitude * PI / 180.0))
        return (GeoPoint.create(lat.coerceIn(-90.0, 90.0), lon.coerceIn(-180.0, 180.0)) as DomainResult.Success).value
    }

    private companion object {
        const val HOME_ID = "home"
        const val DEFAULT_STREET = "Via Tadino"
        val STREETS = mapOf("c2" to "Via Sammartini", "c3" to "Piazza Duca d'Aosta", "c4" to "Via Lecco", "c5" to "Via Marcello")
        val FLIES = mapOf("c1" to 2, "c2" to 14, "c3" to 37, "c5" to 1)
        const val ME_ID = "me"
        const val ME_NAME = "Tu"
        const val LATENCY_MILLIS = 250L
        const val METERS_PER_DEGREE = 111_320.0
        val NEW_POST_OFFSETS = listOf(-120 to 200, 180 to -220, -330 to -60, 90 to 380, 400 to 100, -220 to 330)
    }
}
