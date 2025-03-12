package primitives

import Expr
import EvalException
import Interpreter
import PrimFunc
import Value
import createEvaluations

// String operations
private val CONCAT = Expr.LSym("++")
private val SUBSTRING = Expr.LSym("substr")

internal val STRING_OPERATIONS = createEvaluations {
    // CONCATENATION
    CONCAT += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Str -> when (val v2 = e2.eval()) {
                is Value.Str -> Value.Str(v1.value + v2.value)
                else -> throw EvalException("$v2 is not a str")
            }
            else -> throw EvalException("$v1 is not a str")
        }
    }

    // SUBSTRING
    SUBSTRING += { arguments ->
        if (arguments.size == 2) {
            val (e1, e2) = arguments
            when (val v1 = e1.eval()) {
                is Value.Str -> when (val v2 = e2.eval()) {
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
            when (val v1 = e1.eval()) {
                is Value.Str -> when (val v2 = e2.eval()) {
                    is Value.Int -> when (v2.value) {
                        in 0 until v1.value.length -> when (val v3 = e3.eval()) {
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

internal fun Interpreter.string(primitive: PrimFunc, arguments: List<Expr>): Value {
    if (primitive != SUBSTRING && arguments.size != 2 || primitive == SUBSTRING && arguments.size != 2 && arguments.size != 3) {
        throw EvalException("\'${primitive.symbol}\' requires exactly two${if (primitive == SUBSTRING) " or three" else ""} arguments")
    }
    return STRING_OPERATIONS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported string operation ${primitive.symbol}")
}