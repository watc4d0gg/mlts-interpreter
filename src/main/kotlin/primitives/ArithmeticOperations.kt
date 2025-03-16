package primitives

import internals.EvalException

// Arithmetic
private val ADD = PrimType("+")
private val SUB = PrimType("-")
private val MUL = PrimType("*")
private val DIV = PrimType("/")
private val MOD = PrimType("%")

internal val ARITHMETIC_OPERATORS = createEvaluations {
    // ADDITION
    ADD += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'+\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Int(v1.value + v2.value)
                is Value.Float -> Value.Float(v1.value + v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }

            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Float(v1.value + v2.value)
                is Value.Float -> Value.Float(v1.value + v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }

            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // NEGATION/SUBTRACTION
    SUB += { arguments, environment ->
        if (arguments.size != 1 && arguments.size != 2) {
            throw EvalException("\'-\' requires exactly one or two arguments")
        }
        if (arguments.size == 1) {
            val (e) = arguments
            when (val v = e.eval(environment)) {
                is Value.Int -> Value.Int(-v.value)
                is Value.Float -> Value.Float(-v.value)
                else -> throw EvalException("$v is not an int nor a float")
            }
        } else {
            val (e1, e2) = arguments
            when (val v1 = e1.eval(environment)) {
                is Value.Int -> when (val v2 = e2.eval(environment)) {
                    is Value.Int -> Value.Int(v1.value - v2.value)
                    is Value.Float -> Value.Float(v1.value - v2.value)
                    else -> throw EvalException("$v2 is not an int nor a float")
                }
                is Value.Float -> when (val v2 = e2.eval(environment)) {
                    is Value.Int -> Value.Float(v1.value - v2.value)
                    is Value.Float -> Value.Float(v1.value - v2.value)
                    else -> throw EvalException("$v2 is not an int nor a float")
                }
                else -> throw EvalException("$v1 is not an int nor a float")
            }
        }
    }

    // MULTIPLICATION
    MUL += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'*\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Int(v1.value * v2.value)
                is Value.Float -> Value.Float(v1.value * v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Float(v1.value * v2.value)
                is Value.Float -> Value.Float(v1.value * v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // DIVISION
    DIV += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'/\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Float(v1.value.toDouble() / v2.value.toDouble())
                is Value.Float -> Value.Float(v1.value / v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Float(v1.value / v2.value)
                is Value.Float -> Value.Float(v1.value / v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // MODULO
    MOD += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'%\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Int -> when (val v2 = e2.eval(environment)) {
                is Value.Int -> Value.Int(v1.value % v2.value)
                else -> throw EvalException("$v2 is not an int")
            }
            else -> throw EvalException("$v1 is not an int")
        }
    }
}