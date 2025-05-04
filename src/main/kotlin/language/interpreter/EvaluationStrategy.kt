package language.interpreter

import internals.result
import language.Continuation
import language.Expr
import language.Transformation
import language.Value
import language.transform

/**
 * Evaluation strategies
 */
sealed interface EvaluationStrategy {

    fun <T> onLookup(value: Value, continuation: Continuation<Value, T>): Result<T> = result { continuation(value) }

    context(interpreter: T)
    fun <V : Expr, R, T : Transformation<Expr, V, Environment, R>> bindAs(expression: Expr, environment: Environment, continuation: Continuation<V, R>): Result<R>

    data object CallByValue : EvaluationStrategy {

        context(interpreter: T)
        override fun <V : Expr, R, T : Transformation<Expr, V, Environment, R>> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<V, R>
        ): Result<R> = result {
            expression.transform(environment, continuation).bind()
        }
    }

    data object CallByName : EvaluationStrategy {

        context(_: T)
        override fun <V : Expr, R, T : Transformation<Expr, V, Environment, R>> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<V, R>
        ): Result<R> = result {
            // Value is the only subtype of Expr (both are sealed)
            @Suppress("UNCHECKED_CAST")
            continuation(Value.Thunk(expression, environment) as V)
        }
    }

    data object Lazy : EvaluationStrategy {

        override fun <T> onLookup(value: Value, continuation: Continuation<Value, T>): Result<T> = result {
            when (value) {
                is Value.Thunk -> continuation(value.result ?: value)
                else -> continuation(value)
            }
        }

        context(_: T)
        override fun <V : Expr, R, T : Transformation<Expr, V, Environment, R>> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<V, R>
        ): Result<R> = result {
            // Value is the only subtype of Expr (both are sealed)
            @Suppress("UNCHECKED_CAST")
            continuation(Value.Thunk(expression, environment) as V)
        }
    }
}

context(interpreter: T)
fun <V : Expr, R, T : Transformation<Expr, V, Environment, R>> EvaluationStrategy.bindAllAs(
    expressions: List<Expr>,
    environment: Environment,
    nextEnvironment: context(T) Environment.(
        index: Int,
        expression: Expr,
        value: V
    ) -> Result<Environment> = { _, _, _ -> result { environment } },
    continuation: Continuation<List<V>, R>
): Result<R> = expressions.transform(
    state = environment,
    nextState = nextEnvironment,
    transform = { environment, continuation -> bindAs(this, environment, continuation) },
    continuation = continuation
)

context(interpreter: T)
@JvmName("bindAllAsStep")
fun <R, T : StepInterpreter<R>> EvaluationStrategy.bindAllAs(
    expressions: List<Expr>,
    environment: Environment,
    nextEnvironment: context(T) Environment.(
        index: Int,
        expression: Expr,
        value: Expr
    ) -> Result<Environment> = { _, _, _ -> result { environment } },
    continuation: Continuation<List<Expr>, R>
): Result<R> = expressions.transform(
    state = environment,
    nextState = nextEnvironment,
    transform = { environment, continuation -> bindAs(this, environment, continuation) },
    onValue = { index, value, values, continuation, default ->
        when (value) {
            is Value -> default()
            else -> continuation(values + subList(index + 1, size))
        }
    },
    continuation = continuation
)