package language.interpreter

import internals.*
import language.Binding
import language.Continuation
import language.Expr
import language.Transformation
import language.Value
import language.transform


/**
 * Interpreters
 */


/**
 * Interpreter with big-step operational semantics
 */
typealias Interpreter<T> = Transformation<Expr, Value, Environment, T>

context(interpreter: Interpreter<T>)
fun <T> Value.strict(continuation: Continuation<Value, T>): Result<T> = result {
    when (this@strict) {
        is Value.Thunk -> interpreter.transform(expression, environment) {
            it.strict(continuation).bind()
        }.bind()

        else -> continuation(this@strict)
    }
}


/**
 * Step interpreter with small-step operational semantics
 */
typealias StepInterpreter<T> = Transformation<Expr, Expr, Environment, T>

context(interpreter: StepInterpreter<T>)
@JvmName("strictStep")
fun <T> Value.strict(continuation: Continuation<Expr, T>): Result<T> = result {
    when (this@strict) {
        is Value.Thunk -> interpreter.transform(expression, environment) {
            when (it) {
                // thunks in thunks?
                is Value -> it.strict(continuation).bind()
                else -> {
                    // update the thunk with a partially evaluated expression
                    this@strict.expression = it
                    continuation(it)
                }
            }
        }.bind()

        else -> continuation(this@strict)
    }
}

inline fun <T> StepInterpreter<T>.toBigStep(crossinline onStep: (Expr) -> Unit = {}): Interpreter<T> =
    Interpreter { expression, environment, continuation ->
        onStep(expression)
        transform(expression, environment, recursive {
            {
                onStep(it)
                when (it) {
                    is Value -> continuation(it)
                    else -> transform(it, environment, this).getOrThrow()
                }
            }
        })
    }


/**
 * Pretty printing
 */
typealias PrettyPrinter<T> = Transformation<Expr, String, Environment, T>

fun <T> prettyPrinter(): PrettyPrinter<T> = PrettyPrinter { expression, environment, continuation ->
    result {
        when (expression) {
            is Value.Literal.Str -> continuation("\"${expression.value}\"")
            is Value.Literal<*> -> continuation(expression.value.toString())
            is Expr.Id -> when (expression.name) {
                in environment -> environment.lookup(expression.name) { value ->
                    when (value) {
                        is Value.Thunk -> value.expression.transform(value.environment, continuation)
                            .bind()
                        else -> continuation(value.toString())
                    }
                }.bind()

                else -> continuation(expression.name)
            }

            is Expr.Define -> {
                val start = "(define "
                val indent = start.length
                val name = expression.name
                val definition = expression.expression

                when (definition) {
                    is Expr.Lambda -> {
                        environment.unbind(definition.parameters) { lambdaEnvironment ->
                            definition.body.transform(lambdaEnvironment) { body ->
                                val parameters = "(${definition.parameters.joinToString(" ")})"
                                    .indentWith(indent, firstLine = true)
                                val body = body.indentWith(indent, firstLine = true)
                                continuation("$start$name\n$parameters\n$body)")
                            }.bind()
                        }.bind()
                    }

                    else -> definition.transform(environment) { definition ->
                        continuation("$start$name ${definition.indentWith(indent)})")
                    }.bind()
                }
            }

            is Expr.Lambda -> {
                val start = "(lambda "
                val indent = start.length
                environment.unbind(expression.parameters) { lambdaEnvironment ->
                    expression.body.transform(lambdaEnvironment) { body ->
                        val parameters = "(${expression.parameters.joinToString(" ")})"
                        val body = body.indentWith(indent, firstLine = true)
                        continuation("$start$parameters\n$body)")
                    }.bind()
                }.bind()
            }

            is Expr.Apply -> expression.function.transform(environment) {
                val start = "(${it.indentWith(1)}"

                if (expression.arguments.size > 2) {
                    val indent = start.split("\n").last().length + 1
                    val (head, tail) = expression.arguments.headTail
                    head.transform(environment) { first ->
                        tail.transform(environment) { rest ->
                            val first = first.indentWith(indent)
                            val rest = rest.joinToString("\n") { value ->
                                value.indentWith(indent, firstLine = true)
                            }
                            continuation("$start $first\n$rest)")
                        }.bind()
                    }.bind()

                } else if (expression.arguments.isNotEmpty()) {
                    var indent = start.split("\n").last().length + 1
                    expression.arguments.transform(environment) { values ->
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

            is Expr.Let -> expression.asPair()
                .pretty("(let ", environment, continuation)
                .bind()
            is Expr.LetRec -> expression.asPair()
                .pretty("(letrec ", environment, continuation)
                .bind()

            is Value -> continuation(toString())
        }
    }
}

fun Expr.pretty(environment: Environment): Result<String> = with(prettyPrinter<String>()) {
    transform(environment) { it }
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
    expressions.transform(environment) { values ->
        val values = values.mapIndexed { index, value -> value.indentWith(names[index].length + 2) }
        body.transform(environment) { body ->
            val bindings = values.mapIndexed { index, binding ->
                "(${names[index]} $binding)".indentWith(indent + 1, firstLine = index != 0)
            }.joinToString("\n")
            val body = body.indentWith(indent, firstLine = true)
            continuation("$start($bindings)\n$body)")
        }.bind()
    }.bind()
}