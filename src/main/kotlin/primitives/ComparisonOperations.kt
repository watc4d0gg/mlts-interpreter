package primitives

import internals.EvalException

// Comparisons
private val EQ = PrimType("=")
private val NE = PrimType("!=")
private val LT = PrimType("<")
private val GT = PrimType(">")
private val LEQ = PrimType("<=")
private val GEQ = PrimType(">=")

internal val COMPARISON_OPERATORS = createEvaluations {
    // EQUALITY
    EQ += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'=\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value == v2.value)
                is Value.Float -> Value.Bool(v1.value.toDouble() == v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value == v2.value.toDouble())
                is Value.Float -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Bool -> when (val v2 = e2.eval(environment)) {
                is Value.Bool -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not a bool")
            }
            is Value.Str -> when (val v2 = e2.eval(environment)) {
                is Value.Str -> Value.Bool(v1.value == v2.value)
                else -> throw EvalException("$v2 is not a str")
            }
            else -> throw EvalException("$v1 is not an int, float, bool nor str")
        }
    }

    // NEGATION -- "desugared" into equality
    NE += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'!=\' requires exactly two arguments")
        }
        when (val result = invoke(EQ, arguments, environment)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }

    // LESS THAN
    LT += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'<\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value < v2.value)
                is Value.Float -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value < v2.value)
                is Value.Float -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Str -> when (val v2 = e2.eval(environment)) {
                is Value.Str -> Value.Bool(v1.value < v2.value)
                else -> throw EvalException("$v2 is not an str")
            }
            else -> throw EvalException("$v1 is not an int, float nor str")
        }
    }

    // LESS THAN OR EQUAL TO
    LEQ += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'<=\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value <= v2.value)
                is Value.Float -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Bool(v1.value <= v2.value)
                is Value.Float -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Str -> when (val v2 = e2.eval(environment)) {
                is Value.Str -> Value.Bool(v1.value <= v2.value)
                else -> throw EvalException("$v2 is not an str")
            }
            else -> throw EvalException("$v1 is not an int, float nor str")
        }
    }

    // GREATER THAN -- "desugared" in to less than or equal to
    GT += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'>\' requires exactly two arguments")
        }
        when (val result = invoke(LEQ, arguments, environment)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }

    // GREATER THAN OR EQUAL TO -- "desugared" in to less than
    GEQ += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'>=\' requires exactly two arguments")
        }
        when (val result = invoke(LT, arguments, environment)) {
            is Value.Bool -> Value.Bool(!result.value)
            else -> throw EvalException("$result is not a bool")
        }
    }
}

