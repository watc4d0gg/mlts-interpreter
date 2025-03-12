package primitives

import Expr
import Value
import EvalException
import Interpreter
import PrimFunc
import createEvaluations

// Arithmetic
private val ADD = PrimFunc("+")
private val SUB = PrimFunc("-")
private val MUL = PrimFunc("*")
private val DIV = PrimFunc("/")
private val MOD = PrimFunc("%")

internal val ARITHMETIC_OPERATORS = createEvaluations {
    // ADDITION
    ADD += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Int(v1.value + v2.value)
                is Value.Float -> Value.Float(v1.value + v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }

            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Float(v1.value + v2.value)
                is Value.Float -> Value.Float(v1.value + v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }

            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // NEGATION/SUBTRACTION
    SUB += { arguments ->
        if (arguments.size == 1) {
            val (e) = arguments
            when (val v = e.eval()) {
                is Value.Int -> Value.Int(-v.value)
                is Value.Float -> Value.Float(-v.value)
                else -> throw EvalException("$v is not an int nor a float")
            }
        } else {
            val (e1, e2) = arguments
            when (val v1 = e1.eval()) {
                is Value.Int -> when (val v2 = e2.eval()) {
                    is Value.Int -> Value.Int(v1.value - v2.value)
                    is Value.Float -> Value.Float(v1.value - v2.value)
                    else -> throw EvalException("$v2 is not an int nor a float")
                }
                is Value.Float -> when (val v2 = e2.eval()) {
                    is Value.Int -> Value.Float(v1.value - v2.value)
                    is Value.Float -> Value.Float(v1.value - v2.value)
                    else -> throw EvalException("$v2 is not an int nor a float")
                }
                else -> throw EvalException("$v1 is not an int nor a float")
            }
        }
    }

    // MULTIPLICATION
    MUL += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Int(v1.value * v2.value)
                is Value.Float -> Value.Float(v1.value * v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Float(v1.value * v2.value)
                is Value.Float -> Value.Float(v1.value * v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // DIVISION
    DIV += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Float(v1.value.toDouble() / v2.value.toDouble())
                is Value.Float -> Value.Float(v1.value / v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            is Value.Float -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Float(v1.value / v2.value)
                is Value.Float -> Value.Float(v1.value / v2.value)
                else -> throw EvalException("$v2 is not an int nor a float")
            }
            else -> throw EvalException("$v1 is not an int nor a float")
        }
    }

    // MODULO
    MOD += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Int -> when (val v2 = e2.eval()) {
                is Value.Int -> Value.Int(v1.value % v2.value)
                else -> throw EvalException("$v2 is not an int")
            }
            else -> throw EvalException("$v1 is not an int")
        }
    }
}

internal fun Interpreter.arithmetic(primitive: PrimFunc, arguments: List<Expr>): Value {
    if (primitive == SUB && arguments.size != 1 && arguments.size != 2 || primitive != SUB && arguments.size != 2) {
        throw EvalException("\'${primitive.symbol}\' requires exactly ${if (primitive == SUB) "one or " else ""}two arguments")
    }
    return ARITHMETIC_OPERATORS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported arithmetic operation ${primitive.symbol}")
}