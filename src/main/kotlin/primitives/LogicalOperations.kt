package primitives

import internals.EvalException

// Logical operators
private val NOT = PrimType("not")
private val AND = PrimType("and")
private val OR = PrimType("or")
private val IF = PrimType("if")

internal val LOGICAL_OPERATORS = createEvaluations {
    // NOT
    NOT += { arguments, environment ->
        if (arguments.size != 1) {
            throw EvalException("\'not\' requires exactly one argument")
        }
        val (e) = arguments
        when (val v = e.eval(environment)) {
            is Value.Bool -> Value.Bool(!v.value)
            else -> throw EvalException("$v is not a bool")
        }
    }

    // AND -- short-circuiting
    AND += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'and\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Bool -> when (v1.value) {
                true -> when (val v2 = e2.eval(environment)) {
                    is Value.Bool -> {
                        shortCircuit(e1)
                        v2
                    }
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
    OR += { arguments, environment ->
        if (arguments.size != 2) {
            throw EvalException("\'or\' requires exactly two arguments")
        }
        val (e1, e2) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Bool -> when (v1.value) {
                true -> {
                    shortCircuit(e1)
                    v1
                }
                false -> when (val v2 = e2.eval(environment)) {
                    is Value.Bool -> {
                        shortCircuit(e2)
                        v2
                    }
                    else -> throw EvalException("$v2 is not a bool")
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }

    // IF -- short-circuiting
    IF += { arguments, environment ->
        if (arguments.size != 3) {
            throw EvalException("\'if\' requires exactly three arguments")
        }
        val (e1, e2, e3) = arguments
        when (val v1 = e1.eval(environment)) {
            is Value.Bool -> when (v1.value) {
                true -> {
                    shortCircuit(e2)
                    e2.eval(environment)
                }
                false -> {
                    shortCircuit(e3)
                    e3.eval(environment)
                }
            }
            else -> throw EvalException("$v1 is not a bool")
        }
    }
}