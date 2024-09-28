import java.io.File
import java.util.LinkedList

private val INTEGER_REGEX = Regex("-?[0-9]+")
private val FLOAT_REGEX = Regex("-?[0-9]+[.|,][0-9]+([e|E]-?[0-9]+)?")
private val STRING_REGEX = Regex("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
private val SYMBOL_REGEX = Regex("[^\\s|()\\[\\]]+")

// https://stackoverflow.com/a/9584469
private val TOKENIZER_REGEX = Regex("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*\$)")

private val BRACKET_MAP = mapOf("(" to ")", "[" to "]", "{" to "}")

data class InputData(val identifier: String, val contents: List<String>) {
    val tokens: List<Token> = contents.tokenize()

    constructor(replInput: String) : this("REPL", listOf(replInput))

    constructor(file: File) : this(file.name, file.useLines { it.toList() })
}

data class Token(val token: String, val lineIndex: Int, val index: Int)

fun List<String>.tokenize(): List<Token> = flatMapIndexed { lineIndex, line ->
    var remainingLine = line
    var lastIndex = 0
    line.replace("(", " ( ")
        .replace(")", " ) ")
        .replace("[", " [ ")
        .replace("]", " ] ")
        .trim()
        .split(TOKENIZER_REGEX)
        .map {
            val index = remainingLine.indexOf(it)
            remainingLine = remainingLine.substring(index + it.length)
            lastIndex += index + it.length
            Token(it, lineIndex, lastIndex - it.length)
        }
}

sealed interface Expr {

    data class LInt(val value: Int) : Expr {
        override fun toString() = "$value"
    }

    data class LFloat(val value: Double) : Expr {
        override fun toString() = "$value"
    }

    data class LStr(val value: String) : Expr {
        override fun toString() = "\"$value\""
    }

    data class LSym(val symbol: String) : Expr {
        override fun toString() = symbol
    }

    data class LCom(val comment: String) : Expr {
        override fun toString() = ";$comment"
    }

    data class SExpr(val expressions: List<Expr>) : Expr {
        override fun toString() = toString("")

        private fun toString(prevIndent: String): String {
            return if (expressions.size > 3) {
                val indent = "$prevIndent${" ".repeat(expressions[0].toString().length + 2)}"
                "(${expressions[0]} ${expressions[1]}\n${expressions.subList(2, expressions.size)
                    .joinToString("\n") { 
                        when (it) {
                            is SExpr -> "$indent${it.toString(indent)}"
                            else -> "$indent$it"
                        }
                    }})"
            } else {
                expressions.joinToString(" ", "(", ")")
            }
        }
    }
}

typealias Program = MutableList<Expr>

private class SyntaxException(message: String, data: InputData, token: Token, offset: Int = 0)
    : Exception("""
        ${data.identifier.let { if (it != "REPL") "$it:${token.lineIndex}:${token.index + offset}" else it}}: $message:
        ${token.lineIndex} | ${data.contents[token.lineIndex]}
        ${" ".repeat(token.lineIndex.toString().length + 3 + token.index + offset)}^
    """.trimIndent())

fun InputData.parse(index: Int = 0): Program {
    if (index == tokens.size) {
        return LinkedList()
    }
    val token = tokens[index]
    when (token.token) {
        "(", "[" -> {
            val (result, nextIndex) = parseSExpr(bracket = token.token, startIndex = index + 1)
            return result + parse(nextIndex)
        }
        ")", "]" -> {
            throw SyntaxException("Unexpected closing bracket", this, token)
        }
        else -> {
            (token.parseInt() ?: token.parseFloat() ?: token.parseStr() ?: token.parseSym() ?: token.parseCom())
                ?.let { return it + parse(index + 1) }
                ?: throw SyntaxException("Unparsable expression", this, token)
        }
    }
}

private fun Token.parseInt(): Expr? = INTEGER_REGEX.matchEntire(token)
    ?.let { Expr.LInt(it.value.toInt()) }

private fun Token.parseFloat(): Expr? = FLOAT_REGEX.matchEntire(token)
    ?.let { Expr.LFloat(it.value.toDouble()) }

private fun Token.parseStr(): Expr? = STRING_REGEX.matchEntire(token)
    ?.let { Expr.LStr(it.value.substring(1, it.value.length - 1)) }

private fun Token.parseSym(): Expr? = SYMBOL_REGEX.matchEntire(token)
    ?.let { Expr.LSym(it.value) }

private fun Token.parseCom(): Expr? = if (token.startsWith(";")) Expr.LCom(token.substring(1)) else null

private fun InputData.parseSExpr(bracket: String = "(", startIndex: Int = 1): Pair<Expr, Int> {
    val expressions = LinkedList<Expr>()
    var index = startIndex
    while (index != tokens.size) {
        val token = tokens[index]
        when (token.token) {
            "(", "[" -> {
                val (result, nextIndex) = parseSExpr(bracket = token.token, index + 1)
                expressions += result
                index = nextIndex
            }
            in BRACKET_MAP.values - BRACKET_MAP[bracket] -> {
                throw SyntaxException("Unexpected closing bracket", this, token)
            }
            BRACKET_MAP[bracket] -> {
                return Expr.SExpr(expressions) to index + 1
            }
            else -> {
                (token.parseInt() ?: token.parseFloat() ?: token.parseStr() ?: token.parseSym() ?: token.parseCom())
                    ?.let {
                        expressions += it
                        index++
                    } ?: throw SyntaxException("Unparsable expression", this, token)
            }
        }
    }
    throw SyntaxException("Missing closing bracket", this, tokens[index - 1], offset = 1)
}

private operator fun <E> E.plus(list: MutableList<E>): MutableList<E> =
    list.apply { addFirst(this@plus) }