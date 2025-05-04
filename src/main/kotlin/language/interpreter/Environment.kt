package language.interpreter

import internals.Either
import internals.result
import language.Binding
import language.Continuation
import language.Expr
import language.Value

data class Environment(
    val evaluationStrategy: EvaluationStrategy,
    private val values: MutableMap<String, Value> = mutableMapOf()
) {
    operator fun contains(name: String): Boolean = name in values

    fun <R> lookup(name: String, continuation: Continuation<Value, R>): Result<R> = result {
        evaluationStrategy.onLookup(values.getValue(name), continuation).bind()
    }

    context(interpreter: Interpreter<T>)
    fun <T> bind(bindings: List<Binding>, continuation: Continuation<Environment, T>): Result<T> = result {
        val (names, expressions) = bindings.map(Binding::asPair).unzip()
        evaluationStrategy.bindAllAs(expressions, this@Environment) {
            val environment = Environment(evaluationStrategy, (values + names.zip(it).toMap()).toMutableMap())
            continuation(environment)
        }.bind()
    }

    context(interpreter: StepInterpreter<T>)
    @JvmName("bindStep")
    fun <T> bind(bindings: List<Binding>, continuation: Continuation<Either<List<Expr>, Environment>, T>): Result<T> =
        result {
            val (names, expressions) = bindings.map(Binding::asPair).unzip()
            evaluationStrategy.bindAllAs(expressions, this@Environment) { results ->
                val resultValues = results.filterIsInstance<Value>()
                if (resultValues.size != results.size) {
                    continuation(Either.Left(results))
                } else {
                    val environment =
                        Environment(evaluationStrategy, (values + names.zip(resultValues).toMap()).toMutableMap())
                    continuation(Either.Right(environment))
                }
            }.bind()
        }

    context(interpreter: Interpreter<T>)
    fun <T> bindLocal(
        bindings: List<Binding>,
        isRecursive: Boolean = false,
        continuation: Continuation<Environment, T>
    ): Result<T> = result {
        val (names, expressions) = bindings.map(Binding::asPair).unzip()
        var environment = copy(values = values.toMutableMap())
        evaluationStrategy.bindAllAs(expressions, environment, { index, _, value ->
            result {
                if (isRecursive) {
                    environment.values[names[index]] = value
                } else {
                    environment = Environment(evaluationStrategy, environment.values.toMutableMap().also {
                        it[names[index]] = value
                    })
                }
                environment
            }
        }) {
            continuation(environment)
        }.bind()
    }

    context(interpreter: StepInterpreter<T>)
    @JvmName("bindLocalStep")
    fun <T> bindLocal(
        bindings: List<Binding>,
        isRecursive: Boolean = false,
        continuation: Continuation<Either<List<Expr>, Environment>, T>
    ): Result<T> = result {
        val (names, expressions) = bindings.map(Binding::asPair).unzip()
        var environment = copy(values = values.toMutableMap())
        evaluationStrategy.bindAllAs(expressions, environment, { index, _, value ->
            result {
                if (value is Value) {
                    if (isRecursive) {
                        environment.values[names[index]] = value
                    } else {
                        environment = Environment(evaluationStrategy, environment.values.toMutableMap().also {
                            it[names[index]] = value
                        })
                    }
                }
                environment
            }
        }) { results ->
            val resultValues = results.filterIsInstance<Value>()
            if (resultValues.size != results.size) {
                continuation(Either.Left(results))
            } else {
                continuation(Either.Right(environment))
            }
        }.bind()
    }

    fun <T> unbind(names: List<String>, continuation: Continuation<Environment, T>): Result<T> = result {
        continuation(Environment(evaluationStrategy, (values - names).toMutableMap()))
    }
}