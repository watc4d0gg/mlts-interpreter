package primitives

import internals.EvalException

// String operations
private val CONCAT = Expr.LSym("++")
private val SUBSTRING = Expr.LSym("substr")

internal val STRING_OPERATIONS = createEvaluations {
    // CONCATENATION
    CONCAT += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'++\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Str -> when (val v2 = e2.eval(environment)) {
                is Value.Str -> Value.Str(v1.value + v2.value)
                else -> throw EvalException("$v2 is not a str")
            }
            else -> throw EvalException("$v1 is not a str")
        }
    }

    // SUBSTRING
    SUBSTRING += { arguments, environment ->
        if (arguments.size != 2 && arguments.size != 3) {
            throw EvalException("\'substr\' requires exactly two or three arguments")
        }
        if (arguments.size == 2) {
            val (e1, e2) = arguments
            when (val v1 = e1.eval(environment)) {
                is Value.Str -> when (val v2 = e2.eval(environment)) {
                    is Value.Int -> when (v2.value) {
                        in 0 until v1.value.length -> Value.Str(v1.value.substring(v2.value))
                        else -> throw EvalException("Invalid start index $v2")
                    }
                    else -> throw EvalException("$v2 is not an int")
                }
                else -> throw EvalException("$v1 is not a str")
            }
        } else {
            val (e1, e2, e3) = arguments
            when (val v1 = e1.eval(environment)) {
                is Value.Str -> when (val v2 = e2.eval(environment)) {
                    is Value.Int -> when (v2.value) {
                        in 0 until v1.value.length -> when (val v3 = e3.eval(environment)) {
                            is Value.Int -> when (v3.value) {
                                in 0..v1.value.length -> Value.Str(v1.value.substring(v2.value, v3.value))
                                else -> throw EvalException("Invalid end index $v3")
                            }
                            else -> throw EvalException("$v3 is not an int")
                        }
                        else -> throw EvalException("Invalid start index $v2")
                    }
                    else -> throw EvalException("$v2 is not an int")
                }
                else -> throw EvalException("$v1 is not a str")
            }
        }
    }
}