package language

import internals.or
import internals.result

sealed interface Syntax

/**
 * Continuations
 */
typealias Continuation<T, R> = (value: T) -> R

/**
 * Transformations
 */
fun interface Transformation<E : Syntax, V, S, R> {

    context(transformation: Transformation<E, V, S, R>)
    fun transform(element: E, state: S, continuation: Continuation<V, R>): Result<R>
}

open class TransformationException(message: String, cause: Throwable? = null) : Exception(message, cause)

operator fun <E : Syntax, V, S, R> Transformation<E, V, S, R>.plus(
    other: Transformation<E, V, S, R>
): Transformation<E, V, S, R> = Transformation { element, state, continuation ->
    transform(element, state, continuation) or other.transform(element, state, continuation)
}

context(transformation: T)
fun <E : Syntax, V, S, R, T : Transformation<E, V, S, R>> E.transform(state: S, continuation: Continuation<V, R>): Result<R> =
    transformation.transform(this, state, continuation)

context(transformation: T)
fun <E : Syntax, V, U, S, R, T : Transformation<E, V, S, R>> List<E>.transform(
    state: S,
    nextState: context(T) S.(
        index: Int,
        element: E,
        value: U
    ) -> Result<S> = { _, _, _ -> result { state } },
    transform: context(T) E.(
        state: S,
        continuation: Continuation<U, R>
    ) -> Result<R>,
    onValue: context(T) List<E>.(
        index: Int,
        value: U,
        values: List<U>,
        continuation: Continuation<List<U>, R>,
        default: context(T) () -> R
    ) -> R = { _, _, _, _, default -> default() },
    continuation: Continuation<List<U>, R>
): Result<R> = result {
    if (isEmpty()) {
        continuation(emptyList())
    } else {
        var currentState = state

        fun nextContinuation(index: Int = 0, values: MutableList<U> = mutableListOf()): Continuation<U, R> =
            when (index) {
                size - 1 -> { value ->
                    values.add(value)
                    currentState = currentState.nextState(index, this@transform[index], value).bind()
                    continuation(values)
                }

                else -> { value ->
                    values.add(value)
                    currentState = currentState.nextState(index, this@transform[index], value).bind()
                    onValue(index, value, values, continuation) {
                        val continuation = nextContinuation(index + 1, values)
                        this@transform[index + 1].transform(currentState, continuation).bind()
                    }
                }
            }

        first().transform(currentState, nextContinuation()).bind()
    }
}

context(transformation: T)
fun <E : Syntax, V, S, R, T : Transformation<E, V, S, R>> List<E>.transform(
    state: S,
    nextState: context(T) S.(
        index: Int,
        element: E,
        value: V
    ) -> Result<S> = { _, _, _ -> result { state } },
    onValue: context(T) List<E>.(
        index: Int,
        value: V,
        values: List<V>,
        continuation: Continuation<List<V>, R>,
        default: context(T) () -> R
    ) -> R = { _, _, _, _, default -> default() },
    continuation: Continuation<List<V>, R>
): Result<R> = transform(
    state = state,
    nextState = nextState,
    transform = { state, continuation -> transform(state, continuation) },
    onValue = onValue,
    continuation = continuation
)


typealias TransformationWithoutState<E, V, R> = Transformation<E, V, Nothing?, R>

context(transformation: T)
fun <E : Syntax, V, R, T : TransformationWithoutState<E, V, R>> E.transform(continuation: Continuation<V, R>): Result<R> =
    transform(null, continuation)

context(transformation: T)
fun <E : Syntax, V, U, R, T : TransformationWithoutState<E, V, R>> List<E>.transform(
    transform: context(T) E.(Continuation<U, R>) -> Result<R>,
    onValue: context(T) List<E>.(
        index: Int,
        value: U,
        values: List<U>,
        continuation: Continuation<List<U>, R>,
        default: context(T) () -> R
    ) -> R = { _, _, _, _, default -> default() },
    continuation: Continuation<List<U>, R>
): Result<R> = transform(
    state = null,
    transform = { _, continuation -> transform(continuation) },
    onValue = onValue,
    continuation = continuation
)

context(transformation: T)
fun <E : Syntax, V, R, T : TransformationWithoutState<E, V, R>> List<E>.transform(
    continuation: Continuation<List<V>, R>
): Result<R> = transform(
    transform = { transform(it) },
    continuation = continuation,
)