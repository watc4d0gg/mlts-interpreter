import java.io.BufferedReader
import java.io.File
import java.util.*

// https://stackoverflow.com/a/9584469
private val TOKENIZER_REGEX = Regex("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*\$)")

data class InputData(val identifier: String, val contents: BufferedReader) {
    constructor(replInput: String) : this("REPL", BufferedReader(replInput.reader()))
    constructor(file: File) : this(file.name, file.reader().buffered())
}

data class Token(val value: String, val line: Int, val column: Int)

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

    private fun generateTokens(): Sequence<Token> {
        var lastColumn = 0
        return currentLine.replace("(", " ( ")
            .replace(")", " ) ")
            .replace("[", " [ ")
            .replace("]", " ] ")
            .trim()
            .splitToSequence(TOKENIZER_REGEX)
            .map {
                val column = currentLine.indexOf(it, startIndex = lastColumn)
                lastColumn = column + it.length
                Token(it, currentLineNumber, column)
            }
    }
}