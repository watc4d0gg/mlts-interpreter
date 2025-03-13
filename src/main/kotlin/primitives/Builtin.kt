package primitives

import EvalException
import Expr
import Interpreter
import PrimType
import Value
import createEvaluations

// Builtins
private val ID = PrimType("id")
private val PRINT = PrimType("print")
private val INPUT = PrimType("input")

internal val BUILTIN_FUNCTIONS = createEvaluations {
    // IDENTITY
    ID += { arguments ->
        if (arguments.size != 1) {
            throw EvalException("\'id\' requires exactly one argument")
        }
        arguments.first().eval()
    }

    // PRINT
    PRINT += { arguments ->
        if (arguments.size != 1) {
            throw EvalException("\'print\' requires exactly one argument")
        }
        when (val result = arguments.first().eval()) {
            is Value.Int -> println(result.value)
            is Value.Float -> println(result.value)
            is Value.Bool -> println(result.value)
            is Value.Str -> println(result.value)
            else -> throw EvalException("$result is not a literal")
        }
        Value.Void
    }

    // INPUT
    INPUT += { argument ->
        if (argument.isNotEmpty()) {
            throw EvalException("\'input\' requires no arguments")
        }
        Value.Str(readlnOrNull() ?: "")
    }
}

internal fun Interpreter.builtin(primitive: PrimType, arguments: List<Expr>): Value {
    return BUILTIN_FUNCTIONS[primitive]?.let { it(arguments) } ?: throw EvalException("Unsupported builtin function ${primitive.symbol}")
}