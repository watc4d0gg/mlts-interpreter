import primitives.*
import java.util.*

internal typealias PrimType = Expr.LSym
internal typealias Evaluator = Map<PrimType, Interpreter.(List<Expr>) -> Value>

internal class EvaluatorBuilder(initial: Evaluator = mapOf())
    : MutableMap<PrimType, Interpreter.(List<Expr>) -> Value> by initial.toMutableMap() {
    operator fun PrimType.plusAssign(evaluation: Interpreter.(List<Expr>) -> Value) {
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

    data class Prim(val name: String) : Value() {
        override fun toString() = name
    }

    data class Thunk(val expression: Expr): Value() {
        override fun toString() = expression.prettyString
    }
}

// TODO: type checking
typealias Environment = MutableMap<String, Value>

fun Program.eval(environment: Environment = mutableMapOf()) = Interpreter.Default.eval(this, environment)

fun Program.debug(environment: Environment = mutableMapOf()) = Interpreter.Debugger().eval(this, environment)

internal sealed interface Interpreter {

    fun shortCircuit(target: Expr)

    fun substitute(target: Expr, value: Value)

    fun eval(program: Program, environment: Environment = mutableMapOf()): Sequence<Value> = program.asSequence().map { it.eval() }

    fun Expr.eval(): Value = when(this) {
        is Expr.LInt -> Value.Int(value)
        is Expr.LFloat -> Value.Float(value)
        is Expr.LBool -> Value.Bool(value)
        is Expr.LStr -> Value.Str(value)
        is Expr.LSym -> when (this) {
            in PRIMITIVE_FUNCTIONS -> Value.Prim(symbol)
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
            substitute(this, result)
            result
        }
    }

    /**
     * Default interpreter with no instrumentation
     */
    data object Default : Interpreter {
        override fun shortCircuit(target: Expr) {
        }

        override fun substitute(target: Expr, value: Value) {
        }
    }

    /**
     * Debugging interpreter allowing for step debug mode
     */
    class Debugger : Interpreter {
        private var currentContext: DebugContext? = null

        override fun eval(program: Program, environment: Environment): Sequence<Value> = program
            .asSequence()
            .map {
                currentContext = DebugContext(it)
                step()
                it.eval()
            }

        override fun shortCircuit(target: Expr) {
            currentContext!!.registerShortCircuit(target)
            step()
        }

        override fun substitute(target: Expr, value: Value) {
            if (currentContext!!.registerSubstitution(target, value)) {
                step()
            }
        }

        private fun step() {
            println(currentContext!!.currentEvaluation())
            print("Step? ")
            when (readlnOrNull()) {
                null, "exit", "quit" -> throw DebugStopException()
                else -> return
            }
        }
    }
}

private data class DebugContext(var mainExpression: Expr) {
    private val substitutions: MutableMap<Expr, Value> = IdentityHashMap()
    private val previousExpressions: MutableSet<Expr> = Collections.newSetFromMap(IdentityHashMap())

    fun registerShortCircuit(target: Expr) {
        previousExpressions.add(mainExpression)
        mainExpression = target
        substitutions.clear()
    }

    fun registerSubstitution(target: Expr, value: Value): Boolean {
        if (target === mainExpression || target in previousExpressions) {
            return false
        }
        return substitutions.put(target, value) == null
    }

    fun currentEvaluation(): String = mainExpression.prettyEvaluation()

    private fun Expr.prettyEvaluation(indent: String = ""): String {
        if (substitutions[this] != null) {
            return "${substitutions[this]}"
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

internal class DebugStopException: Exception()