package language.interpreter

import internals.Continuation
import internals.result
import language.Expr
import language.Value

/**
 * Evaluation strategies
 */
sealed interface EvaluationStrategy {

    fun <T> onLookup(value: Value, continuation: Continuation<Value, T>): Result<T> = result { continuation(value) }

    context(interpreter: Interpreter<T, R>)
    fun <T : Expr, R> bindAs(expression: Expr, environment: Environment, continuation: Continuation<T, R>): Result<R>

    data object CallByValue : EvaluationStrategy {

        context(interpreter: Interpreter<T, R>)
        override fun <T : Expr, R> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<T, R>
        ): Result<R> = result {
            expression.interpret(environment, continuation).bind()
        }
    }

    data object CallByName : EvaluationStrategy {

        context(_: Interpreter<T, R>)
        override fun <T : Expr, R> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<T, R>
        ): Result<R> = result {
            // Value is the only subtype of Expr (both are sealed)
            @Suppress("UNCHECKED_CAST")
            continuation(Value.Thunk(expression, environment) as T)
        }
    }

    data object Lazy : EvaluationStrategy {

        override fun <T> onLookup(value: Value, continuation: Continuation<Value, T>): Result<T> = result {
            when (value) {
                is Value.Thunk -> continuation(value.result ?: value)
                else -> continuation(value)
            }
        }

        context(_: Interpreter<T, R>)
        override fun <T : Expr, R> bindAs(
            expression: Expr,
            environment: Environment,
            continuation: Continuation<T, R>
        ): Result<R> = result {
            // Value is the only subtype of Expr (both are sealed)
            @Suppress("UNCHECKED_CAST")
            continuation(Value.Thunk(expression, environment) as T)
        }
    }
}


context(interpreter: Interpreter<T, R>)
fun <T : Expr, R> EvaluationStrategy.bindAllAs(
    expressions: List<Expr>,
    environment: Environment,
    nextEnvironment: Environment.(Int, T) -> Environment = { _, _ -> this },
    continuation: Continuation<List<T>, R>
): Result<R> = result {
    if (expressions.isEmpty()) {
        continuation(emptyList())
    } else {
        var currentEnvironment = environment

        fun nextContinuation(index: Int = 1, values: MutableList<T> = mutableListOf()): Continuation<T, R> =
            when (index) {
                expressions.size - 1 -> { value ->
                    values.add(value)
                    currentEnvironment = currentEnvironment.nextEnvironment(index, value)
                    continuation(values)
                }

                else -> { value ->
                    values.add(value)
                    currentEnvironment = currentEnvironment.nextEnvironment(index, value)
                    // no hope of inlining...
                    val nextContinuation = nextContinuation(index + 1, values)
                    bindAs(expressions[index + 1], currentEnvironment, nextContinuation).bind()
                }
            }

        bindAs(expressions.first(), currentEnvironment, nextContinuation()).bind()
    }
}

context(interpreter: SmallStepInterpreter<T>)
@JvmName("bindAllAsSmallStep")
fun <T> EvaluationStrategy.bindAllAs(
    expressions: List<Expr>,
    environment: Environment,
    nextEnvironment: Environment.(Int, Expr) -> Environment = { _, _ -> this },
    continuation: Continuation<List<Expr>, T>
): Result<T> = result {
    if (expressions.isEmpty()) {
        continuation(emptyList())
    } else {
        var currentEnvironment = environment

        fun nextContinuation(index: Int = 1, values: MutableList<Expr> = mutableListOf()): Continuation<Expr, T> =
            when (index) {
                expressions.size - 1 -> { value ->
                    values.add(value)
                    currentEnvironment = currentEnvironment.nextEnvironment(index, value)
                    continuation(values)
                }

                else -> { value ->
                    values.add(value)
                    currentEnvironment = currentEnvironment.nextEnvironment(index, value)
                    when (value) {
                        is Value -> {
                            val nextContinuation = nextContinuation(index + 1, values)
                            // no hope of inlining...
                            bindAs(expressions[index + 1], currentEnvironment, nextContinuation).bind()
                        }

                        else -> {
                            continuation(values + expressions.subList(index + 1, expressions.size))
                        }
                    }

                }
            }

        bindAs(expressions.first(), currentEnvironment, nextContinuation()).bind()
    }
}