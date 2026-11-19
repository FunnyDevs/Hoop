package app.hoop.data.remote

import app.hoop.data.remote.dto.CommentDto
import app.hoop.data.remote.dto.CreateHoopBody
import app.hoop.data.remote.dto.DeleteHoopsBody
import app.hoop.data.remote.dto.DeletedHoopsDto
import app.hoop.data.remote.dto.FeedPostDto
import app.hoop.data.remote.dto.FunctionResponse
import app.hoop.data.remote.dto.HoopDto
import app.hoop.data.remote.dto.LikeResultDto
import app.hoop.data.remote.dto.LoadCommentsBody
import app.hoop.data.remote.dto.NoParams
import app.hoop.data.remote.dto.NotificationDto
import app.hoop.data.remote.dto.SaveCommentBody
import app.hoop.data.remote.dto.SearchPostsBody
import app.hoop.data.remote.dto.ToggleCommentLikeBody
import app.hoop.data.remote.dto.ToggleLikeBody
import app.hoop.data.remote.dto.UpdateHoopBody
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Cloud Functions behind the home: the Hoops of the user, the posts around them, comments and notifications. */
internal class ParseHomeApi(private val client: HttpClient) {

    suspend fun getUserHoops(): List<HoopDto> = call<List<HoopDto>>(ParseProtocol.FUNCTION_GET_USER_HOOPS)

    suspend fun createHoop(body: CreateHoopBody): HoopDto = call(ParseProtocol.FUNCTION_CREATE_HOOP, body)

    suspend fun updateHoop(body: UpdateHoopBody): HoopDto = call(ParseProtocol.FUNCTION_UPDATE_HOOP, body)

    suspend fun deleteHoops(body: DeleteHoopsBody) {
        call<DeleteHoopsBody, DeletedHoopsDto>(ParseProtocol.FUNCTION_DELETE_HOOPS, body)
    }

    suspend fun searchPosts(body: SearchPostsBody): List<FeedPostDto> = call(ParseProtocol.FUNCTION_SEARCH_POSTS, body)

    suspend fun loadComments(body: LoadCommentsBody): List<CommentDto> = call(ParseProtocol.FUNCTION_LOAD_COMMENTS, body)

    suspend fun saveComment(body: SaveCommentBody): CommentDto = call(ParseProtocol.FUNCTION_SAVE_COMMENT, body)

    suspend fun toggleLike(body: ToggleLikeBody): LikeResultDto = call(ParseProtocol.FUNCTION_TOGGLE_LIKE, body)

    suspend fun toggleCommentLike(body: ToggleCommentLikeBody): LikeResultDto = call(ParseProtocol.FUNCTION_TOGGLE_COMMENT_LIKE, body)

    suspend fun getNotifications(): List<NotificationDto> = call(ParseProtocol.FUNCTION_GET_NOTIFICATIONS)

    private suspend inline fun <reified T> call(function: String): T =
        client.post(ParseProtocol.PATH_FUNCTIONS + function) { setBody(NoParams()) }.parse<FunctionResponse<T>>().result

    private suspend inline fun <reified B : Any, reified T> call(function: String, body: B): T =
        client.post(ParseProtocol.PATH_FUNCTIONS + function) { setBody(body) }.parse<FunctionResponse<T>>().result
}
