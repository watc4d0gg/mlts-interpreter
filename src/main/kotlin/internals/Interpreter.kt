package internals

import Binding
import Environment
import Expr
import Thunk
import Value
import copy
import extend
import extendRecursive
import prettyString
import primitives.*
import java.util.*

private val PRIMITIVE_FUNCTIONS: Evaluator =
    ARITHMETIC_OPERATORS + COMPARISON_OPERATORS + LOGICAL_OPERATORS + STRING_OPERATIONS + BUILTIN_FUNCTIONS

internal fun Sequence<Expr>.eval(initial: Environment): Sequence<Value> = sequence {
    var environment = initial
    for (expression in this@eval) {
        expression.parseDeclaration()
            ?.let {
                environment[it.first] = Thunk(it.second, environment)
                environment = environment.copy()
            } ?: yield(EvalContext(expression)(environment))
    }
}

internal fun Sequence<Expr>.debug(initial: Environment): Sequence<Value> = sequence {
    var environment = initial
    for (expression in this@debug) {
        expression.parseDeclaration()
            ?.let {
                environment[it.first] = Thunk(it.second, environment)
                environment = environment.copy()
            } ?: yield(DebugContext(expression)(environment))
    }
}

/**
 * Default interpreter with no instrumentation
 */
internal open class EvalContext(private var expression: Expr) {

    open fun Expr.pushScope(environment: Environment) = Unit

    open fun Expr.popScope(value: Value?) = Unit

    open fun shortCircuit() = Unit

    operator fun invoke(environment: Environment = mutableMapOf()): Value = expression.eval(environment)

    fun Expr.eval(environment: Environment): Value {
        pushScope(environment)
        val result = when (this) {
            is Expr.LInt -> Value.Int(value)
            is Expr.LFloat -> Value.Float(value)
            is Expr.LBool -> Value.Bool(value)
            is Expr.LStr -> Value.Str(value)
            is Expr.LSym -> when (symbol) {
                in environment -> environment.getValue(symbol).force()
                in PRIMITIVE_FUNCTIONS -> Value.Prim(symbol)
                else -> throw EvalException("\'$symbol\' is an unknown symbol")
            }

            is Expr.SExpr -> {
                if (expressions.isEmpty()) {
                    throw EvalException("Cannot evaluate an empty s-expression")
                }
                val (expr, arguments) = expressions.headTail
                when (expr) {
                    is Expr.LSym -> {
                        when (expr.symbol) {
                            "define" -> throw EvalException("Cannot evaluate a top-level declaration inside an expression")
                            "lambda" -> {
                                if (arguments.size != 2) {
                                    throw EvalException("\'lambda\' requires exactly two arguments")
                                }
                                val (parameters, body) = arguments
                                if (parameters !is Expr.SExpr) {
                                    throw EvalException("\'lambda\' parameters need to defined as an s-expression")
                                }
                                val names = parameters.expressions.map {
                                    if (it !is Expr.LSym) {
                                        throw EvalException("\'${it.prettyString}\' is not a valid parameter name")
                                    }
                                    it.symbol
                                }
                                shortCircuit()
                                Value.Closure(names, body, environment)
                            }

                            "let" -> {
                                if (arguments.size != 2 && arguments.size != 3) {
                                    throw EvalException("\'let\' requires exactly two or three arguments")
                                }
                                val (bindings, body) = if (arguments.size == 2) {
                                    val (variables, body) = arguments
                                    if (variables !is Expr.SExpr) {
                                        throw EvalException("\'let\' variables need to defined as an s-expression")
                                    }
                                    variables.expressions.map { it.parseBinding() } to body
                                } else {
                                    val (name, parameters, body) = arguments
                                    if (name !is Expr.LSym) {
                                        throw EvalException("\'${name.prettyString}\' is an invalid name symbol")
                                    }
                                    if (parameters !is Expr.SExpr) {
                                        throw EvalException("\'let\' parameters need to defined as an s-expression")
                                    }
                                    val bindings = parameters.expressions.map { it.parseBinding() }.toMutableList()
                                    bindings += name.symbol to Expr.SExpr(
                                        listOf(
                                            Expr.LSym("lambda"),
                                            Expr.SExpr(bindings.map { Expr.LSym(it.first) }),
                                            body
                                        )
                                    )
                                    bindings to body
                                }
                                if (bindings.distinctBy { it.first }.size < bindings.size) {
                                    throw EvalException(
                                        "\'let\' bindings require to be distinct (${
                                            bindings
                                                .joinToString(" ") { it.first }
                                        }")
                                }
                                shortCircuit()
                                return body.eval(environment.extend(bindings))
                            }

                            "letrec" -> {
                                if (arguments.size != 2) {
                                    throw EvalException("\'letrec\' requires exactly two arguments")
                                }
                                val (variables, body) = arguments
                                if (variables !is Expr.SExpr) {
                                    throw EvalException("\'let\' variables need to defined as an s-expression")
                                }
                                val bindings = variables.expressions.map { it.parseBinding() }
                                if (bindings.distinctBy { it.first }.size < bindings.size) {
                                    throw EvalException(
                                        "\'let\' bindings require to be distinct (${
                                            bindings
                                                .joinToString(" ") { it.first }
                                        }")
                                }
                                shortCircuit()
                                return body.eval(environment.extendRecursive(bindings))
                            }

                            in environment -> when (val result = expr.eval(environment)) {
                                is Value.Prim -> PRIMITIVE_FUNCTIONS(PrimType(result.name), arguments, environment)
                                is Value.Closure -> result.evalClosure(arguments, environment)
                                else -> result
                            }

                            in PRIMITIVE_FUNCTIONS -> PRIMITIVE_FUNCTIONS(expr, arguments, environment)
                            else -> throw EvalException("\'${expr.symbol}\' is an unknown symbol")
                        }
                    }

                    is Expr.SExpr -> when (val result = expr.eval(environment)) {
                        is Value.Closure -> result.evalClosure(arguments, environment)
                        else -> throw EvalException("\'$result\' is not a function")
                    }

                    else -> throw EvalException("\'${expr.prettyString}\' doesn't evaluate to a function or primitive")
                }
            }
        }
        popScope(if (this is Expr.LSym && symbol in environment || this is Expr.SExpr && result !is Value.Closure) result else null)
        return result
    }

    private fun Thunk.force() = let { (expression, environment) -> expression.eval(environment) }

    private operator fun Evaluator.invoke(primitive: PrimType, arguments: List<Expr>, environment: Environment): Value =
        this[primitive](arguments, environment)

    private fun Value.Closure.evalClosure(arguments: List<Expr>, environment: Environment): Value {
        if (parameters.size != arguments.size) {
            throw EvalException("Expected ${parameters.size} parameters, got ${arguments.size}")
        }
        shortCircuit()
        return body.eval(environment.extend(parameters.zip(arguments), environment))
    }
}

private fun Expr.parseDeclaration(): Binding? {
    if (this !is Expr.SExpr || expressions.size != 3 && expressions.size != 4
        || expressions.first() != Expr.LSym("define")
    ) {
        return null
    }
    val (_, arguments) = expressions.headTail
    return if (arguments.size == 2) {
        val (id, body) = arguments
        if (id !is Expr.LSym) {
            throw EvalException("\'${id.prettyString}\' is an invalid name symbol")
        }
        id.symbol to body
    } else {
        val (name, parameters, body) = arguments
        if (name !is Expr.LSym) {
            throw EvalException("\'${name.prettyString}\' is an invalid name symbol")
        }
        if (parameters !is Expr.SExpr) {
            throw EvalException("\'${name.prettyString}\' parameters need to defined as an s-expression")
        }
        parameters.expressions.forEach {
            if (it !is Expr.LSym) {
                throw EvalException("\'${it.prettyString}\' is an invalid parameter name")
            }
        }
        name.symbol to Expr.SExpr(listOf(Expr.LSym("lambda"), parameters, body))
    }
}

private fun Expr.parseBinding(): Binding {
    if (this !is Expr.SExpr) {
        throw EvalException("\'$prettyString\' is an invalid binding expression")
    }
    if (expressions.size != 2) {
        throw EvalException("Bindings require exactly two arguments")
    }
    val (name, value) = expressions
    if (name !is Expr.LSym) {
        throw EvalException("\'${name.prettyString}\' is an invalid name symbol")
    }
    return name.symbol to value
}

/**
 * Debugging interpreter allowing for step debug mode
 */
internal class DebugContext(expression: Expr) : EvalContext(expression) {
    private val scopes = LinkedList<ExprScope>()
    private val scopeMap: MutableMap<Thunk, ExprScope> = mutableMapOf()
    private val substitutions: MutableMap<Thunk, Value> = mutableMapOf()
    private var shortCircuit: Boolean = false

    override fun Expr.pushScope(environment: Environment) {
        val prevScope = if (scopes.isNotEmpty()) scopes.peek() else null
        val shouldShortCircuit = scopes.isEmpty() || shortCircuit
        ExprScope(this, environment).let {
            scopes.push(it)
            scopeMap[it.thunk] = it
            if (shouldShortCircuit) {
                prevScope?.shadow = it.thunk
                shortCircuit = false
                step()
            }
        }
    }

    override fun Expr.popScope(value: Value?) {
        val scope = scopes.pop()
        scopeMap.remove(scope.thunk)
        value?.let {
            if (substitutions.put(scope.thunk, it) == null && scope.shadow == null && !startingThunk.reachesValue()) {
                step()
            }
        }
    }

    override fun shortCircuit() {
        shortCircuit = true
    }

    private fun step() {
        println(startingThunk.prettyEvaluation())
        while (true) {
            print("Step? ")
            when (readlnOrNull()) {
                null, "exit", "quit" -> throw DebugStopException()
                ":env" -> {
                    println(scopes.peek().environment.prettyString)
                    System.out.flush()
                }

                else -> {
                    System.out.flush()
                    break
                }
            }
        }
    }

    private val startingThunk: Thunk get() = scopes.last.thunk

    private tailrec fun Thunk.reachesValue(): Boolean {
        if (this in substitutions) {
            return true
        }
        val scope = scopeMap[this]
        if (scope?.shadow == null) {
            return false
        }
        return scope.shadow!!.reachesValue()
    }

    private fun Thunk.prettyEvaluation(indent: String = ""): String {
        if (this in substitutions) {
            return "${substitutions[this]}"
        }
        if (this in scopeMap) {
            val shadow = scopeMap[this]?.shadow
            if (shadow != null) {
                return shadow.prettyEvaluation(indent)
            }
        }
        val (expression, environment) = when (expression) {
            is Expr.LSym -> {
                val thunk = environment[expression.symbol]
                if (thunk != null && thunk in substitutions) {
                    thunk
                } else {
                    this
                }
            }

            else -> this
        }
        return when (expression) {
            is Expr.SExpr -> {
                val expressions = expression.expressions
                if (expressions.size > 3) {
                    val nextIndent =
                        "$indent${" ".repeat(Thunk(expressions[0], environment).prettyEvaluation().length + 2)}"
                    val firstLine = "${Thunk(expressions[0], environment).prettyEvaluation()} ${
                        Thunk(
                            expressions[1],
                            environment
                        ).prettyEvaluation()
                    }"
                    val nextLines = expressions.subList(2, expressions.size).joinToString("\n") {
                        when (it) {
                            is Expr.SExpr -> "$nextIndent${
                                Thunk(
                                    it,
                                    environment
                                ).prettyEvaluation(indent = nextIndent)
                            }"

                            else -> "$nextIndent${Thunk(it, environment).prettyEvaluation()}"
                        }
                    }
                    "($firstLine\n$nextLines)"
                } else {
                    expressions.joinToString(" ", "(", ")") { Thunk(it, environment).prettyEvaluation() }
                }
            }

            else -> expression.prettyString
        }
    }
}

private data class ExprScope(val expression: Expr, val environment: Environment, var shadow: Thunk? = null) {
    val thunk: Thunk = Thunk(expression, environment)

    override fun toString(): String = "[${expression.prettyString} ${environment.prettyString} ${shadow}]"
}

internal class EvalException(message: String) : Exception("EvalError: $message")

internal class DebugStopException : Exception()