package primitives

import EvalException
import Expr
import Interpreter
import PrimType
import Value
import createEvaluations

// Logical operators
private val NOT = PrimType("not")
private val AND = PrimType("and")
private val OR = PrimType("or")
private val IF = PrimType("if")

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
                    shortCircuit(e1)
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
                    shortCircuit(e1)
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
                    shortCircuit(e2)
                    e2.eval()
                }
                false -> {
                    shortCircuit(e3)
                    e3.eval()
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }
}

internal fun Interpreter.logical(primitive: PrimType, arguments: List<Expr>): Value {
    when (primitive) {
        NOT -> if (arguments.size != 1) throw EvalException("\'not\' requires exactly one argument")
        IF  -> if (arguments.size != 3) throw EvalException("\'if\' requires exactly three arguments")
        else -> if (arguments.size != 2) throw EvalException("\'${primitive.symbol}\' requires exactly two arguments")
    }
    return LOGICAL_OPERATORS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported logical operation ${primitive.symbol}")
}