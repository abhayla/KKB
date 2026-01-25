package com.rasoiai.app.presentation.common

/**
 * Base interface for UI state classes.
 * All screen-specific UiState classes should implement common patterns:
 * - isLoading: Boolean for loading indicator
 * - error: String? for error message display
 *
 * Example:
 * ```
 * data class HomeUiState(
 *     override val isLoading: Boolean = false,
 *     override val error: String? = null,
 *     val mealPlan: MealPlan? = null,
 *     val festivals: List<Festival> = emptyList()
 * ) : BaseUiState
 * ```
 */
interface BaseUiState {
    val isLoading: Boolean
    val error: String?
}

/**
 * Sealed class for handling async operations result.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): String? = (this as? Error)?.message

    inline fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, throwable)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String) -> Unit): Resource<T> {
        if (this is Error) action(message)
        return this
    }

    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) action()
        return this
    }
}

/**
 * Extension to convert Flow<T> to Flow<Resource<T>>
 */
// Usage in ViewModel:
// repository.getData()
//     .asResource()
//     .collect { resource ->
//         _uiState.update { it.copy(data = resource) }
//     }
