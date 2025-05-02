package internals

import InputData
import Token
import language.SExpr
import java.util.*

// https://stackoverflow.com/a/9584469
private val TOKENIZER_REGEX = Regex("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*\$)")

private val INTEGER_REGEX = Regex("-?[0-9]+")
private val FLOAT_REGEX = Regex("-?[0-9]+[.|,][0-9]+([e|E]-?[0-9]+)?")
private val STRING_REGEX = Regex("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
private val SYMBOL_REGEX = Regex("[^\\s|()\\[\\]]+")

private val BRACKET_MAP = mapOf("(" to ")", "[" to "]", "{" to "}")

data class Tokenizer(val data: InputData): Iterator<Token> {
    private val lines = data.contents.lineSequence().iterator()

    private var currentLineNumber = 1
    var currentLine = lines.next()
        private set
    private var tokenBuffer = generateTokens().iterator()
    private var prevToken: Token? = null
    private var currentToken: Token? = null

    override fun hasNext(): Boolean {
        if (!tokenBuffer.hasNext() && lines.hasNext()) {
            currentLineNumber++
            currentLine = lines.next()
            tokenBuffer = generateTokens().iterator()
        }
        return tokenBuffer.hasNext() || lines.hasNext()
    }

    override fun next(): Token {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        prevToken = currentToken
        currentToken = tokenBuffer.next()
        return currentToken!!
    }

    fun hasPrevious(): Boolean = prevToken != null

    fun peekPrevious(): Token {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        return prevToken!!
    }

    private fun generateTokens(): Sequence<Token> = sequence {
        if (currentLine.isNotEmpty()) {
            var lastColumn = 0
            val values = currentLine.replace("(", " ( ")
                .replace(")", " ) ")
                .replace("[", " [ ")
                .replace("]", " ] ")
                .trim()
                .splitToSequence(TOKENIZER_REGEX)
            for (value in values) {
                val column = currentLine.indexOf(value, startIndex = lastColumn)
                lastColumn = column + value.length
                yield(Token(value, currentLineNumber, column))
            }
        }
    }
}

internal fun Tokenizer.read(): Sequence<SExpr> = sequence {
    while (hasNext()) {
        val token = next()
        when (val value = token.value) {
            "(", "[", "{" -> readSList(value)
            ")", "]", "}" -> throw SyntaxException("Unexpected closing bracket", this@read, token)
            else -> if (value.startsWith(";")) null else readLiteral(token)
        }?.let { yield(it) }
    }
}

private fun Token.readInt(): SExpr? = INTEGER_REGEX.matchEntire(value)
    ?.let { SExpr.LInt(it.value.toInt()) }

private fun Token.readFloat(): SExpr? = FLOAT_REGEX.matchEntire(value)
    ?.let { SExpr.LFloat(it.value.toDouble()) }

private fun Token.readBool(): SExpr? = when (value) {
    "true" -> SExpr.LBool(true)
    "false" -> SExpr.LBool(false)
    else -> null
}

private fun Token.readStr(): SExpr? = STRING_REGEX.matchEntire(value)
    ?.let { SExpr.LStr(it.value.substring(1, it.value.length - 1)) }

private fun Token.readSym(): SExpr? = SYMBOL_REGEX.matchEntire(value)
    ?.let { SExpr.LSym(it.value) }

private fun Tokenizer.readLiteral(token: Token): SExpr =
    token.readInt()
        ?: token.readFloat()
        ?: token.readBool()
        ?: token.readStr()
        ?: token.readSym()
        ?: throw SyntaxException("Unparsable literal", this, token)

private fun Tokenizer.readSList(bracket: String): SExpr {
    val expressions = LinkedList<SExpr>()
    while (hasNext()) {
        val token = next()
        expressions += when (val value = token.value) {
            "(", "[", "{" -> readSList(value)
            in BRACKET_MAP.values - BRACKET_MAP[bracket] -> throw SyntaxException("Unexpected closing bracket", this, token)
            BRACKET_MAP[bracket] -> {
                return SExpr.SList(expressions)
            }
            else -> readLiteral(token)
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