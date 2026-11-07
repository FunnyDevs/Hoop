package app.hoop.domain.post

import app.hoop.domain.assertSuccess
import app.hoop.domain.assertViolation
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.hoop.testExtra
import app.hoop.domain.hoop.testHome
import kotlin.test.Test

class PostDraftTest {
    private val media = MediaRef("file://x", "image/jpeg")

    @Test fun homeAcceptsEveryType() {
        PostDraft.create(testHome(), PostType.Question, "Chi apre?").assertSuccess()
        PostDraft.create(testHome(), PostType.Photo, "", media).assertSuccess()
        PostDraft.create(testHome(), PostType.Video, "didascalia", media).assertSuccess()
    }

    @Test fun extraHoopAcceptsOnlyQuestions() {
        PostDraft.create(testExtra("a"), PostType.Question, "Chi apre?").assertSuccess()
        PostDraft.create(testExtra("a"), PostType.Photo, "", media).assertViolation(Violation.PostTypeNotAllowedInHoop)
        PostDraft.create(testExtra("a"), PostType.Video, "", media).assertViolation(Violation.PostTypeNotAllowedInHoop)
    }

    @Test fun questionNeedsText() = PostDraft.create(testHome(), PostType.Question, "  ").assertViolation(Violation.PostTextBlank)
    @Test fun photoNeedsMedia() = PostDraft.create(testHome(), PostType.Photo, "x").assertViolation(Violation.PostMediaMissing)
    @Test fun textIsLimited() =
        PostDraft.create(testHome(), PostType.Question, "a".repeat(PostPolicy.TEXT_MAX_LENGTH + 1)).assertViolation(Violation.PostTextTooLong)
}
