package primitives

import EvalException
import Expr
import Interpreter
import PrimFunc
import Value
import createEvaluations

// Logical operators
private val NOT = PrimFunc("not")
private val AND = PrimFunc("and")
private val OR = PrimFunc("or")
private val IF = PrimFunc("if")

internal val LOGICAL_OPERATORS = createEvaluations {
    // NOT
    NOT += { (e) ->
        when (val v = e.eval()) {
            is Value.Bool -> Value.Bool(!v.value)
            else -> throw EvalException("$v is not a bool")
        }
    }

    // AND -- short-circuiting
    AND += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Bool -> when (v1.value) {
                true -> when (val v2 = e2.eval()) {
                    is Value.Bool -> v2
                    else -> throw EvalException("$v2 is not a bool")
                }
                false -> {
                    debugger?.shortCircuit(e1)
                    v1
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }

    // OR -- short-circuiting
    OR += { (e1, e2) ->
        when (val v1 = e1.eval()) {
            is Value.Bool -> when (v1.value) {
                true -> {
                    debugger?.shortCircuit(e1)
                    v1
                }
                false -> when (val v2 = e2.eval()) {
                    is Value.Bool -> v2
                    else -> throw EvalException("$v2 is not a bool")
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }

    // IF -- short-circuiting
    IF += { (e1, e2, e3) ->
        when (val v1 = e1.eval()) {
            is Value.Bool -> when (v1.value) {
                true -> {
                    debugger?.shortCircuit(e2)
                    e2.eval()
                }
                false -> {
                    debugger?.shortCircuit(e3)
                    e3.eval()
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }
}

internal fun Interpreter.logical(primitive: PrimFunc, arguments: List<Expr>): Value {
    if (primitive == NOT && arguments.size != 1) {
        throw EvalException("\'not\' requires exactly one argument")
    }
    if (primitive != IF && arguments.size != 2 || primitive == IF && arguments.size != 3) {
        throw EvalException("\'${primitive.symbol}\' requires exactly two${if (primitive == IF) " or three" else ""} arguments")
    }
    return LOGICAL_OPERATORS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported logical operation ${primitive.symbol}")
}