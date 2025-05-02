package language.interpreter

import internals.*
import language.Binding
import language.Expr
import language.Value


/**
 * Interpreters
 */
fun interface Interpreter<T, R> {

    context(interpreter: Interpreter<T, R>)
    fun interpret(expression: Expr, environment: Environment, continuation: Continuation<T, R>): Result<R>
}

context(interpreter: Interpreter<T, R>)
fun <T, R> Expr.interpret(environment: Environment, continuation: Continuation<T, R>): Result<R> =
    interpreter.interpret(this, environment, continuation)

context(interpreter: Interpreter<T, R>)
fun <T, R> List<Expr>.interpret(environment: Environment, continuation: Continuation<List<T>, R>): Result<R> = result {
    if (isEmpty()) {
        continuation(emptyList())
    } else {
        fun nextContinuation(index: Int = 0, values: MutableList<T> = mutableListOf()): Continuation<T, R> =
            when (index) {
                size - 1 -> { value ->
                    values.add(value)
                    continuation(values)
                }

                else -> { value ->
                    values.add(value)
                    val nextContinuation = nextContinuation(index + 1, values)
                    this@interpret[index + 1].interpret(environment, nextContinuation).bind()
                }
            }

        first().interpret(environment, nextContinuation()).bind()
    }
}

operator fun <T, R> Interpreter<T, R>.plus(other: Interpreter<T, R>): Interpreter<T, R> =
    Interpreter { expression, environment, continuation ->
        interpret(expression, environment, continuation) or other.interpret(
            expression,
            environment,
            continuation
        )
    }

typealias BigStepInterpreter<T> = Interpreter<Value, T>

context(interpreter: BigStepInterpreter<T>)
@JvmName("strictBigStep")
fun <T> Value.strict(continuation: Continuation<Value, T>): Result<T> = result {
    when (this@strict) {
        is Value.Thunk -> interpreter.interpret(expression, environment) {
            it.strict(continuation).bind()
        }.bind()

        else -> continuation(this@strict)
    }
}

typealias SmallStepInterpreter<T> = Interpreter<Expr, T>

context(interpreter: SmallStepInterpreter<T>)
@JvmName("strictSmallStep")
fun <T> Value.strict(continuation: Continuation<Expr, T>): Result<T> = result {
    when (this@strict) {
        is Value.Thunk -> interpreter.interpret(expression, environment) {
            when (it) {
                // thunks in thunks?
                is Value -> it.strict(continuation).bind()
                else -> {
                    this@strict.expression = it
                    continuation(it)
                }
            }
        }.bind()

        else -> continuation(this@strict)
    }
}

fun <T> SmallStepInterpreter<T>.toBigStep(): BigStepInterpreter<T> =
    BigStepInterpreter { expression, environment, continuation ->
        interpret(expression, environment, recursive {
            {
                when (it) {
                    is Value -> continuation(it)
                    else -> interpret(expression, environment, this).getOrThrow()
                }
            }
        })
    }

typealias PrettyPrinter<T> = Interpreter<String, T>

fun <T> prettyPrinter(): PrettyPrinter<T> = PrettyPrinter { expression, environment, continuation ->
    result {
        when (expression) {
            is Value.Literal.Str -> continuation("\"${expression.value}\"")
            is Value.Literal<*> -> continuation(expression.value.toString())
            is Expr.Id -> when (expression.name) {
                in environment -> environment.lookup(expression.name) { value ->
                    when (value) {
                        is Value.Thunk -> value.result?.let { continuation(it.toString()) }
                            ?: value.expression.interpret(value.environment, continuation).bind()

                        else -> continuation(value.toString())
                    }
                }.bind()

                else -> continuation(expression.name)
            }

            is Expr.Define -> {
                val start = "(define "
                val indent = start.length
                val definition = expression.expression

                when (definition) {
                    is Expr.Lambda -> {
                        environment.unbind(definition.parameters) { lambdaEnvironment ->
                            definition.body.interpret(lambdaEnvironment) {
                                val parameters = "(${definition.parameters.joinToString(" ")})"
                                    .indentWith(indent, firstLine = true)
                                val body = it.indentWith(indent, firstLine = true)
                                continuation("$start${expression.name}\n$parameters\n$body)")
                            }.bind()
                        }.bind()
                    }

                    else -> expression.interpret(environment) {
                        continuation("$start${expression.name} ${it.indentWith(indent)})")
                    }.bind()
                }
            }

            is Expr.Lambda -> {
                val start = "(lambda "
                val indent = start.length
                environment.unbind(expression.parameters) { lambdaEnvironment ->
                    expression.body.interpret(lambdaEnvironment) {
                        val parameters = "(${expression.parameters.joinToString(" ")})"
                        val body = it.indentWith(indent, firstLine = true)
                        continuation("$start$parameters\n$body)")
                    }.bind()
                }.bind()
            }

            is Expr.Apply -> expression.function.interpret(environment) {
                val start = "(${it.indentWith(1)}"

                if (expression.arguments.size > 2) {
                    val indent = start.split("\n").last().length + 1
                    val (head, tail) = expression.arguments.headTail
                    head.interpret(environment) { value ->
                        tail.interpret(environment) { values ->
                            val first = value.indentWith(indent)
                            val rest = values.joinToString("\n") { value ->
                                value.indentWith(indent, firstLine = true)
                            }
                            continuation("$start $first\n$rest)")
                        }.bind()
                    }.bind()

                } else if (expression.arguments.isNotEmpty()) {
                    var indent = start.split("\n").last().length + 1
                    expression.arguments.interpret(environment) { values ->
                        val arguments = values.joinToString(" ") { value ->
                            val indented = value.indentWith(indent)
                            indent += indented.split("\n").last().length + 1
                            indented
                        }
                        continuation("$start $arguments)")
                    }.bind()

                } else {
                    continuation("$start)")
                }
            }.bind()

            is Expr.Let -> (expression.bindings to expression.body).pretty("(let ", environment, continuation).bind()
            is Expr.LetRec -> (expression.bindings to expression.body).pretty("(letrec ", environment, continuation)
                .bind()

            is Value -> continuation(toString())
        }
    }
}

fun Expr.pretty(environment: Environment): Result<String> = with(prettyPrinter<String>()) {
    interpret(environment) { it }
}

context(interpreter: PrettyPrinter<T>)
private fun <T> Pair<List<Binding>, Expr>.pretty(
    start: String,
    environment: Environment,
    continuation: Continuation<String, T>
): Result<T> = result {
    val indent = start.length
    val (bindings, body) = this@pretty
    val (names, expressions) = bindings.map(Binding::asPair).unzip()
    expressions.interpret(environment) {
        val values = it.mapIndexed { index, value -> value.indentWith(names[index].length + 2) }
        body.interpret(environment) { value ->
            val bindings = values.mapIndexed { index, binding ->
                "(${names[index]} $binding)".indentWith(indent + 1, firstLine = index != 0)
            }.joinToString("\n")
            val body = value.indentWith(indent, firstLine = true)
            continuation("$start($bindings)\n$body)")
        }.bind()
    }.bind()
}