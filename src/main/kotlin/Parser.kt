import java.io.File
import java.util.*

private val INTEGER_REGEX = Regex("-?[0-9]+")
private val FLOAT_REGEX = Regex("-?[0-9]+[.|,][0-9]+([e|E]-?[0-9]+)?")
private val STRING_REGEX = Regex("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
private val SYMBOL_REGEX = Regex("[^\\s|()\\[\\]]+")

private val BRACKET_MAP = mapOf("(" to ")", "[" to "]", "{" to "}")

sealed interface Expr {

    fun toPrettyString(): String

    data class LInt(val value: Int) : Expr {
        override fun toPrettyString() = "$value"

        override fun toString() = "LInt $value"
    }

    data class LFloat(val value: Double) : Expr {
        override fun toPrettyString() = "$value"

        override fun toString() = "LFloat $value"
    }

    data class LStr(val value: String) : Expr {
        override fun toPrettyString() = "\"$value\""

        override fun toString() = "LStr \"$value\""
    }

    data class LSym(val symbol: String) : Expr {
        override fun toPrettyString() = symbol

        override fun toString() = "LSym $symbol"
    }

    data class LCom(val comment: String) : Expr {
        override fun toPrettyString() = ";$comment"

        override fun toString() = "LCom \"$comment\""
    }

    data class SExpr(val expressions: List<Expr>) : Expr {
        override fun toPrettyString() = toPrettyString("")

        private fun toPrettyString(startIndent: String): String {
            return if (expressions.size > 3) {
                val indent = "$startIndent${" ".repeat(expressions[0].toPrettyString().length + 1)}"
                val firstLine = "${expressions[0].toPrettyString()} ${expressions[1].toPrettyString()}"
                val nextLines = expressions.subList(2, expressions.size).joinToString("\n") {
                        when (it) {
                            is SExpr -> "$indent${it.toPrettyString(indent)}"
                            else -> "$indent${it.toPrettyString()}"
                        }
                    }
                "$firstLine\n$nextLines"
            } else {
                expressions.joinToString(" ", "(", ")", transform = Expr::toPrettyString)
            }
        }

        override fun toString() = "SExpr $expressions"
    }
}

typealias Program = MutableList<Expr>

fun String.parse(): Program = Tokenizer(InputData(this)).parse()

fun File.parse(): Program = Tokenizer(InputData(this)).parse()

private tailrec fun Tokenizer.parse(accumulator: MutableList<Expr> = LinkedList()): Program {
    if (!hasNext()) {
        return accumulator
    }
    val token = next()
    val expr: Expr = when (token.value) {
        "(", "[", "{" -> parseSExpr(bracket = token.value)
        ")", "]", "}" -> throw SyntaxException("Unexpected closing bracket", this, token)
        else -> parseLiteral(token)
    }
    return parse(accumulator + expr)
}

private fun Token.parseInt(): Expr? = INTEGER_REGEX.matchEntire(value)
    ?.let { Expr.LInt(it.value.toInt()) }

private fun Token.parseFloat(): Expr? = FLOAT_REGEX.matchEntire(value)
    ?.let { Expr.LFloat(it.value.toDouble()) }

private fun Token.parseStr(): Expr? = STRING_REGEX.matchEntire(value)
    ?.let { Expr.LStr(it.value.substring(1, it.value.length - 1)) }

private fun Token.parseSym(): Expr? = SYMBOL_REGEX.matchEntire(value)
    ?.let { Expr.LSym(it.value) }

private fun Token.parseCom(): Expr? = if (value.startsWith(";")) Expr.LCom(value.substring(1)) else null

private fun Tokenizer.parseLiteral(token: Token) : Expr = (token.parseInt() ?: token.parseFloat() ?: token.parseStr() ?: token.parseSym() ?: token.parseCom())
    ?: throw SyntaxException("Unparsable literal", this, token)

private fun Tokenizer.parseSExpr(bracket: String = "("): Expr {
    val expressions = LinkedList<Expr>()
    while (hasNext()) {
        val token = next()
        expressions += when (token.value) {
            "(", "[", "{" -> parseSExpr(bracket = token.value)
            in BRACKET_MAP.values - BRACKET_MAP[bracket] -> throw SyntaxException("Unexpected closing bracket", this, token)
            BRACKET_MAP[bracket] -> {
                return Expr.SExpr(expressions)
            }
            else -> parseLiteral(token)
        }
    }
    throw SyntaxException("Missing closing bracket", this, peekPrevious(), offset = 1)
}

class SyntaxException(message: String, tokenizer: Tokenizer, token: Token, offset: Int = 0):
    Exception("""
        ${tokenizer.data.identifier.let { if (it != "REPL") "$it:${token.line}:${token.column + offset}" else it}}: Syntax Error: $message:
        ${token.line} | ${tokenizer.currentLine}
        ${" ".repeat(token.line.toString().length + 3 + token.column + offset)}^
        """.trimIndent())

private operator fun <E> MutableList<E>.plus(elem: E): MutableList<E> = apply { addLast(elem) }