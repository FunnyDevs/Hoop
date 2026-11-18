package app.hoop.domain.post

import app.hoop.domain.assertSuccess
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.assertViolation
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.hoop.testExtra
import app.hoop.domain.hoop.testHome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostDraftTest {
    private val position = GeoPoint.create(45.0, 9.0).assertSuccess()
    private val media = MediaRef("file://x", "image/jpeg", capturedAtEpochMillis = 1_700_000_000_000L)

    @Test fun homeAcceptsEveryType() {
        PostDraft.create(testHome(), PostType.Question, "Chi apre?", null, position, "id-1").assertSuccess()
        PostDraft.create(testHome(), PostType.Photo, "Che luce", media, position, "id-1").assertSuccess()
        PostDraft.create(testHome(), PostType.Video, "didascalia", media, position, "id-1").assertSuccess()
    }

    @Test fun extraHoopAcceptsOnlyQuestions() {
        PostDraft.create(testExtra("a"), PostType.Question, "Chi apre?", null, position, "id-1").assertSuccess()
        PostDraft.create(testExtra("a"), PostType.Photo, "x", media, position, "id-1").assertViolation(Violation.PostTypeNotAllowedInHoop)
        PostDraft.create(testExtra("a"), PostType.Video, "x", media, position, "id-1").assertViolation(Violation.PostTypeNotAllowedInHoop)
    }

    @Test fun photoAndVideoNeedACaption() {
        PostDraft.create(testHome(), PostType.Photo, " ", media, position, "id-1").assertViolation(Violation.PostTextBlank)
        PostDraft.create(testHome(), PostType.Video, "", media, position, "id-1").assertViolation(Violation.PostTextBlank)
    }

    @Test fun addressIsTrimmedCappedAndOptional() {
        val long = PostDraft.create(testHome(), PostType.Question, "x", null, position, "id-1", " " + "a".repeat(300)).assertSuccess()
        assertEquals(PostPolicy.ADDRESS_MAX_LENGTH, long.address?.length)
        assertNull(PostDraft.create(testHome(), PostType.Question, "x", null, position, "id-1", "  ").assertSuccess().address)
        assertNull(PostDraft.create(testHome(), PostType.Question, "x", null, position, "id-1").assertSuccess().address)
    }

    @Test fun mediaOrientationFollowsItsSize() {
        assertEquals(MediaOrientation.Landscape, media.copy(width = 1920, height = 1080).orientation)
        assertEquals(MediaOrientation.Portrait, media.copy(width = 1080, height = 1920).orientation)
        assertEquals(MediaOrientation.Square, media.copy(width = 1000, height = 1000).orientation)
        assertNull(media.orientation)
        assertNull(media.copy(width = 0, height = 100).orientation)
    }

    @Test fun questionNeedsText() = PostDraft.create(testHome(), PostType.Question, "  ", null, position, "id-1").assertViolation(Violation.PostTextBlank)
    @Test fun photoNeedsMedia() = PostDraft.create(testHome(), PostType.Photo, "x", null, position, "id-1").assertViolation(Violation.PostMediaMissing)
    @Test fun mediaFromTheGalleryIsRefused() =
        PostDraft.create(testHome(), PostType.Photo, "x", MediaRef("file://x", "image/jpeg"), position, "id-1").assertViolation(Violation.PostMediaNotLive)
    @Test fun textIsLimited() =
        PostDraft.create(testHome(), PostType.Question, "a".repeat(PostPolicy.TEXT_MAX_LENGTH + 1), null, position, "id-1").assertViolation(Violation.PostTextTooLong)
}
