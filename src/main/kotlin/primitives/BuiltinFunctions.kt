package primitives

import internals.EvalException

// Builtins
private val ID = PrimType("id")
private val PRINT = PrimType("print")
private val INPUT = PrimType("input")

internal val BUILTIN_FUNCTIONS = createEvaluations {
    // IDENTITY
    ID += { arguments, environment ->
        if (arguments.size != 1) {
            throw EvalException("\'id\' requires exactly one argument")
        }
        arguments.first().eval(environment)
    }

    // PRINT
    PRINT += { arguments, environment ->
        if (arguments.size != 1) {
            throw EvalException("\'print\' requires exactly one argument")
        }
        when (val result = arguments.first().eval(environment)) {
            is Value.Int -> println(result.value)
            is Value.Float -> println(result.value)
            is Value.Bool -> println(result.value)
            is Value.Str -> println(result.value)
            else -> throw EvalException("$result is not a literal")
        }
        Value.Void
    }

    // INPUT
    INPUT += { arguments, _ ->
        if (arguments.isNotEmpty()) {
            throw EvalException("\'input\' requires no arguments")
        }
        Value.Str(readlnOrNull() ?: "")
    }
}