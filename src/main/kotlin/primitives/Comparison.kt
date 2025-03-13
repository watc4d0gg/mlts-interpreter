package primitives

import Expr
import Value
import EvalException
import Interpreter
import PrimType
import createEvaluations

// Comparisons
private val EQ = PrimType("=")
private val NE = PrimType("!=")
private val LT = PrimType("<")
private val GT = PrimType(">")
private val LEQ = PrimType("<=")
private val GEQ = PrimType(">=")

internal val COMPARISON_OPERATORS = createEvaluations {
    // EQUALITY
    EQ += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value == v2.value)
                is Value.Float -> Value.Bool(v1.value.toDouble() == v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value == v2.value.toDouble())
                is Value.Float -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Bool -> when (val v2 = e2.eval()) {
                is Value.Bool -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not a bool")
            }
            is Value.Str -> when (val v2 = e2.eval()) {
                is Value.Str -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not a str")
            }
            else -> throw EvalException("$v1 is not an int, float, bool nor str")
        }
    }

    // NEGATION -- "desugared" into equality
    NE += {
        when (val result = comparisons(EQ, it)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }

    // LESS THAN
    LT += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value < v2.value)
                is Value.Float -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value < v2.value)
                is Value.Float -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Str -> when (val v2 = e2.eval()) {
                is Value.Str -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an str")
            }
            else -> throw EvalException("$v1 is not an int, float nor str")
        }
    }

    // LESS THAN OR EQUAL TO
    LEQ += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value <= v2.value)
                is Value.Float -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Bool(v1.value <= v2.value)
                is Value.Float -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Str -> when (val v2 = e2.eval()) {
                is Value.Str -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an str")
            }
            else -> throw EvalException("$v1 is not an int, float nor str")
        }
    }

    // GREATER THAN -- "desugared" in to less than or equal to
    GT += {
        when (val result = comparisons(LEQ, it)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }

    // GREATER THAN OR EQUAL TO -- "desugared" in to less than
    GEQ += {
        when (val result = comparisons(LT, it)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }
}

internal fun Interpreter.comparisons(primitive: PrimType, arguments: List<Expr>): Value {
    if (arguments.size != 2) {
        throw EvalException("\'${primitive.symbol}\' requires exactly two arguments")
    }
    return COMPARISON_OPERATORS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported comparison operation ${primitive.symbol}")
}

