package app.hoop.domain.common

/** Outcome of a domain operation: either a value or a [DomainError], never exceptions for expected cases. */
sealed interface DomainResult<out T> {
    data class Success<out T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>

    companion object {
        fun <T> success(value: T): DomainResult<T> = Success(value)
        fun failure(error: DomainError): DomainResult<Nothing> = Failure(error)
        fun failure(violation: Violation): DomainResult<Nothing> = Failure(DomainError.Validation(violation))
    }
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}

inline fun <T, R> DomainResult<T>.flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> = when (this) {
    is DomainResult.Success -> transform(value)
    is DomainResult.Failure -> this
}

inline fun <T, R> DomainResult<T>.fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R = when (this) {
    is DomainResult.Success -> onSuccess(value)
    is DomainResult.Failure -> onFailure(error)
}

fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.value
fun <T> DomainResult<T>.errorOrNull(): DomainError? = (this as? DomainResult.Failure)?.error
