import internals.Tokenizer
import internals.defaultParser
import internals.read
import language.Expr
import language.SExpr
import language.transform
import java.io.BufferedReader
import java.io.File

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

fun String.read(): Sequence<SExpr> = Tokenizer(InputData(this)).read()

fun File.read(): Sequence<SExpr> = Tokenizer(InputData(this)).read()

fun Sequence<SExpr>.parse(): Sequence<Result<Expr>> = sequence {
    with(defaultParser<Expr>()) {
        forEach { expression ->
            yield(expression.transform { it })
        }
    }
}