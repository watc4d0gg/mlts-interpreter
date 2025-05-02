package language.parser

import internals.Continuation
import internals.indentWith
import internals.or
import internals.result
import language.Expr
import language.SExpr

/**
 * Parsers
 */
fun interface Parser<T, R> {

    context(parser: Parser<T, R>)
    fun parse(expression: SExpr, continuation: Continuation<T, R>): Result<R>
}

context(parser: Parser<T, R>)
fun <T, R> SExpr.parse(continuation: Continuation<T, R>): Result<R> = parser.parse(this, continuation)

context(parser: Parser<T, R>)
fun <T, V, R> List<SExpr>.parse(
    parse: context(Parser<T, R>) SExpr.(Continuation<V, R>) -> Result<R>,
    continuation: Continuation<List<V>, R>
): Result<R> = result {
    if (isEmpty()) {
        continuation(emptyList())
    } else {
        fun nextContinuation(index: Int = 0, values: MutableList<V> = mutableListOf()): Continuation<V, R> =
            when (index) {
                size - 1 -> { value ->
                    values.add(value)
                    continuation(values)
                }

                else -> { value ->
                    values.add(value)
                    val nextContinuation = nextContinuation(index + 1, values)
                    this@parse[index + 1].parse(nextContinuation).bind()
                }
            }

        first().parse(nextContinuation()).bind()
    }
}

context(parser: Parser<T, R>)
fun <T, R> List<SExpr>.parse(continuation: Continuation<List<T>, R>): Result<R> =
    parse({ parse(it) }, continuation)

operator fun <T, R> Parser<T, R>.plus(other: Parser<T, R>): Parser<T, R> = Parser { expression, continuation ->
    parse(expression, continuation) or other.parse(expression, continuation)
}

typealias ASTParser<T> = Parser<Expr, T>

typealias PrettyPrinter<T> = Parser<String, T>

fun <T> prettyPrinter(): PrettyPrinter<T> = PrettyPrinter { expression, continuation ->
    result {
        when (expression) {
            is SExpr.LInt -> continuation(expression.value.toString())
            is SExpr.LFloat -> continuation(expression.value.toString())
            is SExpr.LBool -> continuation(expression.value.toString())
            is SExpr.LStr -> continuation("\"${expression.value}\"")
            is SExpr.LSym -> continuation(expression.symbol)
            is SExpr.SList -> {
                val expressions = expression.expressions
                if (expressions.size > 3) {
                    expressions[0].parse { e0 ->
                        val firstExpression = e0.indentWith(1)
                        val indent = firstExpression.split("\n").last().length + 2
                        expressions[1].parse { e1 ->
                            val secondExpression = e1.indentWith(indent)
                            val firstLine = "$firstExpression $secondExpression"
                            expressions.subList(2, expressions.size).parse { rest ->
                                val nextLines = rest.joinToString("\n") {
                                    it.indentWith(indent, firstLine = true)
                                }
                                continuation("($firstLine\n$nextLines)")
                            }.bind()
                        }.bind()
                    }.bind()

                } else {
                    expressions.parse { it ->
                        var indent = 0
                        val values = it.joinToString(" ") { value ->
                            value.indentWith(indent).also {
                                indent += it.split("\n").last().length + 2
                            }
                        }
                        continuation("($values)")
                    }.bind()
                }
            }
        }
    }
}

fun SExpr.pretty(): Result<String> = with(prettyPrinter<String>()) { parse { it } }