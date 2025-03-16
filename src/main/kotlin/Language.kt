import internals.*
import java.io.BufferedReader
import java.io.File
import java.util.LinkedList

/**
 * A class describing the type of data to read the program from
 */
data class InputData private constructor(val identifier: String, val contents: BufferedReader) {
    constructor(replInput: String) : this("REPL", BufferedReader(replInput.reader()))
    constructor(file: File) : this(file.name, file.reader().buffered())
}

data class Token(val value: String, val line: Int, val column: Int)

fun String.tokenize(): Iterator<Token> = InputData(this).tokenize()

fun File.tokenize(): Iterator<Token> = InputData(this).tokenize()

fun InputData.tokenize(): Iterator<Token> = Tokenizer(this)

/**
 * A class containing all the parseable expressions
 */
sealed interface Expr {

    val prettyString: String

    data class LInt(val value: Int) : Expr {
        override val prettyString get() = "$value"

        override fun toString() = "LInt $value"
    }

    data class LFloat(val value: Double) : Expr {
        override val prettyString get() = "$value"

        override fun toString() = "LFloat $value"
    }

    data class LBool(val value: Boolean) : Expr {
        override val prettyString get() = "$value"

        override fun toString() = "LBool $value"
    }

    data class LStr(val value: String) : Expr {
        override val prettyString get() = "\"$value\""

        override fun toString() = "LStr \"$value\""
    }

    data class LSym(val symbol: String) : Expr {
        override val prettyString get() = symbol

        override fun toString() = "LSym $symbol"
    }

    data class SExpr(val expressions: List<Expr>) : Expr {
        override val prettyString get() = asPrettyString()

        private fun asPrettyString(startIndent: String = ""): String {
            return if (expressions.size > 3) {
                val indent = "$startIndent${" ".repeat(expressions[0].prettyString.length + 2)}"
                val firstLine = "${expressions[0].prettyString} ${expressions[1].prettyString}"
                val nextLines = expressions.subList(2, expressions.size).joinToString("\n") {
                    when (it) {
                        is SExpr -> "$indent${it.asPrettyString(indent)}"
                        else -> "$indent${it.prettyString}"
                    }
                }
                "($firstLine\n$nextLines)"
            } else {
                expressions.joinToString(" ", "(", ")", transform = Expr::prettyString)
            }
        }

        override fun toString() = "SExpr $expressions"
    }
}

typealias Program = List<Expr>

fun Iterator<Token>.parse(): Program = (this as Tokenizer).parse(LinkedList())

fun String.parse(): Program = InputData(this).tokenize().parse()

fun File.parse(): Program = InputData(this).tokenize().parse()

sealed interface Value {

    // TODO: implement side-effects using algebraic effects and handlers instead
    data object Void : Value {
        override fun toString() = "<void>"
    }

    data class Int(val value: kotlin.Int) : Value {
        override fun toString(): String = value.toString()
    }

    data class Float(val value: Double) : Value {
        override fun toString(): String = value.toString()
    }

    data class Bool(val value: Boolean) : Value {
        override fun toString(): String = value.toString()
    }

    data class Str(val value: String) : Value {
        override fun toString(): String = "\"$value\""
    }

    data class Closure(val parameters: List<String>, val body: Expr, val environment: Environment) : Value {
        override fun toString(): String = body.prettyString
    }

    data class Prim(val name: String) : Value {
        override fun toString(): String = name
    }
}

data class Thunk(val expression: Expr, val environment: Environment) {
    override fun toString(): String = expression.prettyString
}

// TODO: type checking
typealias Environment = MutableMap<String, Thunk>
typealias Binding = Pair<String, Expr>

fun Environment.copy() = toMutableMap()

operator fun Environment.plusAssign(binding: Binding) {
    this[binding.first] = Thunk(binding.second, copy())
}

fun Environment.extend(bindings: List<Binding>): Environment {
    val result = copy()
    for (binding in bindings) {
        result += binding
    }
    return result
}

fun Environment.extendRecursive(bindings: List<Binding>): Environment {
    val result = copy()
    for (binding in bindings) {
        result[binding.first] = Thunk(binding.second, result)
    }
    return result
}

fun Environment.extend(bindings: List<Binding>, outer: Environment): Environment {
    val result = copy()
    val copy = outer.copy()
    for (binding in bindings) {
        result[binding.first] = Thunk(binding.second, copy)
    }
    return result
}

val Environment.prettyString: String get() = map { (name, thunk) ->
    val beginning = "$name -> "
    val indent = " ".repeat(beginning.length + 1)
    val end = thunk.toString().split("\n").mapIndexed { index, line ->
        if (index != 0) {
            "$indent$line"
        } else {
            line
        }
    }.joinToString("\n")
    "$beginning$end"
}.joinToString(",\n ", prefix = "[", postfix = "]")

fun Program.eval(environment: Environment = mutableMapOf()): Sequence<Value> = asSequence().eval(environment)

fun Program.debug(environment: Environment = mutableMapOf()): Sequence<Value> = asSequence().debug(environment)
