package app.hoop.di

import app.hoop.domain.identity.EmailTypoDetector
import app.hoop.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Use cases are registered in the composition root (the app), so the domain
 * stays free of any dependency injection framework.
 */
val domainModule = module {
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::RequestPasswordResetUseCase)
    factoryOf(::ChangePasswordUseCase)
    factoryOf(::RestoreSessionUseCase)
    factoryOf(::RequestLocationUseCase)
    factoryOf(::CheckEmailAvailabilityUseCase)
    factoryOf(::CheckUsernameAvailabilityUseCase)
    factoryOf(::PublishPostUseCase)
    factoryOf(::SendPendingPostsUseCase)
    factoryOf(::GetPendingPostsUseCase)
    factoryOf(::CancelPendingPostUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::GetUserHoopsUseCase)
    factoryOf(::GetHoopFeedUseCase)
    factoryOf(::AddReplyUseCase)
    factoryOf(::GetRepliesUseCase)
    factoryOf(::SetFlyUseCase)
    factoryOf(::SetReplyFlyUseCase)
    factoryOf(::GetNotificationsUseCase)
    factoryOf(::AddHoopUseCase)
    factoryOf(::RemoveHoopUseCase)
    factoryOf(::ResizeHoopUseCase)
    single { EmailTypoDetector() }
}
