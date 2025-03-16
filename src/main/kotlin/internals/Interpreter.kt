package internals

import Binding
import Environment
import Expr
import Thunk
import Value
import copy
import extend
import plusAssign
import prettyString
import primitives.*
import java.util.*

private val PRIMITIVE_FUNCTIONS: Evaluator = ARITHMETIC_OPERATORS + COMPARISON_OPERATORS + LOGICAL_OPERATORS + STRING_OPERATIONS + BUILTIN_FUNCTIONS

internal fun Sequence<Expr>.eval(environment: Environment): Sequence<Value> = sequence {
    for (expression in this@eval) {
        expression.parseDeclaration()?.let { environment += it } ?: yield(EvalContext(expression)(environment))
    }
}

internal fun Sequence<Expr>.debug(environment: Environment): Sequence<Value> = sequence {
    for (expression in this@debug) {
        expression.parseDeclaration()?.let { environment += it } ?: yield(DebugContext(expression)(environment))
    }
}

private fun Expr.parseDeclaration(): Binding? {
    if (this !is Expr.SExpr || expressions.size != 3 && expressions.size != 4
        || expressions.first() != Expr.LSym("define")) {
        return null
    }
    val (_, arguments) = expressions.headTail
    return if (arguments.size == 2) {
        val (name, body) = arguments
        if (name !is Expr.LSym) {
            throw EvalException("\'${name.prettyString}\' is an invalid name symbol")
        }
        name.symbol to body
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
                throw EvalException("\'${it.prettyString}\' is not a valid parameter name")
            }
        }
        name.symbol to Expr.SExpr(listOf(Expr.LSym("lambda"), parameters, body))
    }
}

/**
 * Default interpreter with no instrumentation
 */
internal open class EvalContext(protected var expression: Expr) {

    open fun Expr.pushScope(environment: Environment) = Unit

    open fun Expr.popScope(value: Value?) = Unit

    open fun shortCircuit(target: Expr) = Unit

    operator fun invoke(environment: Environment = mutableMapOf()): Value = expression.eval(environment)

    fun Expr.eval(environment: Environment): Value {
        pushScope(environment)
        val result = when(this) {
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
                                Value.Closure(names, body, environment)
                            }
                            in environment -> when (val result = environment.getValue(expr.symbol).force()) {
                                is Value.Prim -> PRIMITIVE_FUNCTIONS(PrimType(result.name), arguments, environment)
                                else -> result
                            }
                            in PRIMITIVE_FUNCTIONS -> PRIMITIVE_FUNCTIONS(expr, arguments, environment)
                            else -> throw EvalException("\'${expr.symbol}\' is an unknown symbol")
                        }
                    }
                    is Expr.SExpr -> when (val func = expr.eval(environment)) {
                        is Value.Closure -> {
                            if (func.parameters.size != arguments.size) {
                                throw EvalException("Expected ${func.parameters.size} parameters, got ${arguments.size}")
                            }
                            val funcEnvironment = func.environment.extend(func.parameters.zip(arguments), environment)
                            shortCircuit(func.body)
                            func.body.eval(funcEnvironment)
                        }
                        else -> throw EvalException("\'$func\' is not a function")
                    }
                    else -> throw EvalException("\'${expr.prettyString}\' doesn't evaluate to a function or primitive")
                }
            }
        }
        popScope(if (this is Expr.LSym && symbol in environment || this is Expr.SExpr && result !is Value.Closure) result else null)
        return result
    }

    fun Thunk.force() = let { (expression, environment) -> expression.eval(environment) }

    operator fun Evaluator.invoke(primitive: PrimType, arguments: List<Expr>, environment: Environment): Value =
        this[primitive](arguments, environment)
}

/**
 * Debugging interpreter allowing for step debug mode
 */
internal class DebugContext(expression: Expr): EvalContext(expression) {
    private val scopes = Stack<ExprScope>()
    private val scopeMap: MutableMap<Expr, ExprScope> = IdentityHashMap()
    private val substitutions: MutableMap<Expr, Value> = IdentityHashMap()

    override fun Expr.pushScope(environment: Environment) {
        val shouldShortCircuit = scopes.empty() || scopes.peek().shortCircuit === this
        ExprScope(this, environment).let {
            scopes.push(it)
            scopeMap[this] = it
        }
        if (shouldShortCircuit) {
            step()
        }
    }

    override fun Expr.popScope(value: Value?) {
        val scope = scopes.pop()
        scopeMap.remove(this)
        value?.let {
            substitutions[this] = it
            if (scope.shortCircuit == null && scopes.size > 1) {
                step()
            }
        }
    }

    override fun shortCircuit(target: Expr) {
        scopes.peek().shortCircuit = target
    }

    private fun step() {
        println(expression.prettyEvaluation())
        while (true) {
            print("Step? ")
            when (readlnOrNull()) {
                null, "exit", "quit" -> throw DebugStopException()
                ":env" -> println(scopes.peek().environment.prettyString)
                else -> break
            }
        }
    }

    private fun Expr.prettyEvaluation(indent: String = ""): String {
        if (this in substitutions) {
            return "${substitutions[this]}"
        }
        if (scopeMap[this]?.shortCircuit != null) {
            return "${scopeMap[this]?.shortCircuit?.prettyEvaluation()}"
        }
        return when (this) {
            is Expr.SExpr -> {
                if (expressions.size > 3) {
                    val nextIndent = "$indent${" ".repeat(expressions[0].prettyEvaluation().length + 2)}"
                    val firstLine = "${expressions[0].prettyEvaluation()} ${expressions[1].prettyEvaluation()}"
                    val nextLines = expressions.subList(2, expressions.size).joinToString("\n") {
                        when (it) {
                            is Expr.SExpr -> "$nextIndent${it.prettyEvaluation(indent = nextIndent)}"
                            else -> "$nextIndent${it.prettyEvaluation()}"
                        }
                    }
                    "($firstLine\n$nextLines)"
                } else {
                    expressions.joinToString(" ", "(", ")") { it.prettyEvaluation() }
                }
            }
            else -> prettyString
        }
    }
}

private data class ExprScope(val expression: Expr, val environment: Environment, var shortCircuit: Expr? = null)

internal class EvalException(message: String) : Exception("EvalError: $message")

internal class DebugStopException: Exception()