import primitives.*
import java.util.*

internal typealias PrimFunc = Expr.LSym
internal typealias Evaluator = Map<PrimFunc, Interpreter.(List<Expr>) -> Value>

internal class EvaluatorBuilder(initial: Evaluator = mapOf())
    : MutableMap<PrimFunc, Interpreter.(List<Expr>) -> Value> by initial.toMutableMap() {
    operator fun PrimFunc.plusAssign(evaluation: Interpreter.(List<Expr>) -> Value) {
        this@EvaluatorBuilder[this] = evaluation
    }
}

internal fun createEvaluations(builder: EvaluatorBuilder.() -> Unit): Evaluator = EvaluatorBuilder().apply(builder).toMap()

private val PRIMITIVE_FUNCTIONS = ARITHMETIC_OPERATORS.keys +
        COMPARISON_OPERATORS.keys +
        LOGICAL_OPERATORS.keys +
        STRING_OPERATIONS.keys +
        BUILTIN_FUNCTIONS.keys

internal class EvalException(message: String) : Exception("EvalError: $message")

sealed class Value {

    // TODO: algebraic effects
    data object Void : Value() {
        override fun toString() = "<void>"
    }

    data class Int(val value: kotlin.Int) : Value() {
        override fun toString(): String = value.toString()
    }

    data class Float(val value: Double) : Value() {
        override fun toString(): String = value.toString()
    }

    data class Bool(val value: Boolean) : Value() {
        override fun toString(): String = value.toString()
    }

    data class Str(val value: String) : Value() {
        override fun toString(): String = "\"$value\""
    }

    data class Lambda(val parameters: List<String>, val expression: Expr) : Value() {
        override fun toString(): String = expression.prettyString
    }

    data class Primitive(val name: String) : Value() {
        override fun toString() = name
    }

    data class Thunk(val expression: Expr): Value() {
        override fun toString() = expression.prettyString
    }
}

// TODO: type checking
typealias Environment = MutableMap<String, Value>

fun Program.eval(environment: Environment = mutableMapOf(), debug: Boolean = false) = Interpreter(environment, debug).evaluate(this)

internal data class Interpreter(
    val environment: Environment = mutableMapOf(),
    private val debug: Boolean = false) {

    var debugger: Debugger? = null
        private set

    fun evaluate(program: Program): Sequence<Value> = program
        .asSequence()
        .map {
            if (debug && it is Expr.SExpr) debugger = Debugger(it)
            // TODO: declarations
            it.eval()
        }

    fun Expr.eval(): Value = when(this) {
        is Expr.LInt -> Value.Int(value)
        is Expr.LFloat -> Value.Float(value)
        is Expr.LBool -> Value.Bool(value)
        is Expr.LStr -> Value.Str(value)
        is Expr.LSym -> when (this) {
            in PRIMITIVE_FUNCTIONS -> Value.Primitive(symbol)
            else -> throw EvalException("\'$symbol\' is an unknown symbol")
        }
        is Expr.SExpr -> {
            if (expressions.isEmpty()) {
                throw EvalException("Cannot evaluate an empty s-expression")
            }
            val result = when (val expr = expressions.first()) {
                is Expr.LSym -> {
                    val arguments = expressions.drop(1)
                    when (expr) {
                        // TODO: function definitions
                        in ARITHMETIC_OPERATORS -> arithmetic(expr, arguments)
                        in COMPARISON_OPERATORS -> comparisons(expr, arguments)
                        in LOGICAL_OPERATORS    -> logical(expr, arguments)
                        in STRING_OPERATIONS    -> string(expr, arguments)
                        in BUILTIN_FUNCTIONS    -> builtin(expr, arguments)
                        else -> throw EvalException("\'${expr.symbol}\' is an unknown symbol")
                    }
                }
                else -> throw EvalException("\'${expr.prettyString}\' doesn't evaluate to a function or primitive")
            }
            debugger?.substitute(this, result)
            result
        }
    }
}

internal class DebugStopException: Exception()

internal data class Debugger(private var mainExpression: Expr) {
    private val substitutions: MutableMap<Expr, Value> = IdentityHashMap()
    private val previousExpressions: MutableSet<Expr> = Collections.newSetFromMap(IdentityHashMap())

    init {
        step()
    }

    private fun step() {
        println(mainExpression.currentEvaluation())
        print("Step? ")
        when (readlnOrNull()) {
            null, "exit", "quit" -> throw DebugStopException()
            else -> return
        }
    }

    fun shortCircuit(target: Expr) {
        previousExpressions.add(mainExpression)
        mainExpression = target
        substitutions.clear()
        step()
    }

    fun substitute(target: Expr, value: Value) {
        if (target === mainExpression || target in previousExpressions) {
            return
        }
        substitutions[target] = value
        step()
    }

    private fun Expr.currentEvaluation(indent: String = ""): String {
        if (substitutions[this] != null) {
            return "${substitutions[this]}"
        }
        return when (this) {
            is Expr.SExpr -> {
                if (expressions.size > 3) {
                    val nextIndent = "$indent${" ".repeat(expressions[0].currentEvaluation().length + 2)}"
                    val firstLine = "${expressions[0].currentEvaluation()} ${expressions[1].currentEvaluation()}"
                    val nextLines = expressions.subList(2, expressions.size).joinToString("\n") {
                        when (it) {
                            is Expr.SExpr -> "$nextIndent${it.currentEvaluation(indent = nextIndent)}"
                            else -> "$nextIndent${it.currentEvaluation()}"
                        }
                    }
                    "($firstLine\n$nextLines)"
                } else {
                    expressions.joinToString(" ", "(", ")") { it.currentEvaluation() }
                }
            }
            else -> prettyString
        }
    }
}