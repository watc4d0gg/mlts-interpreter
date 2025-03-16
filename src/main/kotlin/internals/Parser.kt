package internals

import Expr
import Program
import Token
import java.util.*

private val INTEGER_REGEX = Regex("-?[0-9]+")
private val FLOAT_REGEX = Regex("-?[0-9]+[.|,][0-9]+([e|E]-?[0-9]+)?")
private val STRING_REGEX = Regex("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
private val SYMBOL_REGEX = Regex("[^\\s|()\\[\\]]+")

private val BRACKET_MAP = mapOf("(" to ")", "[" to "]", "{" to "}")

internal tailrec fun Tokenizer.parse(accumulator: MutableList<Expr>): Program {
    if (!hasNext()) {
        return accumulator
    }
    val token = next()
    val expr: Expr? = when (val value = token.value) {
        "(", "[", "{" -> parseSExpr(bracket = value)
        ")", "]", "}" -> throw SyntaxException("Unexpected closing bracket", this, token)
        else -> if (value.startsWith(";")) null else parseLiteral(token)
    }
    return parse(accumulator + expr)
}

private fun Token.parseInt(): Expr? = INTEGER_REGEX.matchEntire(value)
    ?.let { Expr.LInt(it.value.toInt()) }

private fun Token.parseFloat(): Expr? = FLOAT_REGEX.matchEntire(value)
    ?.let { Expr.LFloat(it.value.toDouble()) }

private fun Token.parseBool(): Expr? = when (value) {
    "true" -> Expr.LBool(true)
    "false" -> Expr.LBool(false)
    else -> null
}

private fun Token.parseStr(): Expr? = STRING_REGEX.matchEntire(value)
    ?.let { Expr.LStr(it.value.substring(1, it.value.length - 1)) }

private fun Token.parseSym(): Expr? = SYMBOL_REGEX.matchEntire(value)
    ?.let { Expr.LSym(it.value) }

private fun Tokenizer.parseLiteral(token: Token): Expr =
    token.parseInt()
    ?: token.parseFloat()
    ?: token.parseBool()
    ?: token.parseStr()
    ?: token.parseSym()
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

internal class SyntaxException(message: String, tokenizer: Tokenizer, token: Token, offset: Int = 0):
    Exception("""
        ${tokenizer.data.identifier.let { "$it:${token.line}:${token.column + offset}" }}: Syntax Error: $message:
        ${token.line} | ${tokenizer.currentLine}
        ${" ".repeat(token.line.toString().length + 3 + token.column + offset)}^
        """.trimIndent())