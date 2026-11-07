package app.hoop.domain.usecase

import app.hoop.domain.PostsRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.flatMap
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType

class PublishPostUseCase(private val posts: PostsRepository) {
    suspend operator fun invoke(hoop: Hoop, type: PostType, text: String, media: MediaRef? = null): DomainResult<PostId> =
        PostDraft.create(hoop, type, text, media).flatMap { posts.publish(it) }
}
