package language.parser

import internals.indentWith
import internals.result
import language.*

/**
 * Parsers
 */
typealias Parser<T> = TransformationWithoutState<SExpr, Expr, T>

class ParseException(message: String, cause: Throwable? = null) :
    TransformationException("ParseError: $message", cause)

/**
 * Pretty printing
 */
typealias PrettyPrinter<T> = TransformationWithoutState<SExpr, String, T>

fun <T> prettyPrinter(): PrettyPrinter<T> = PrettyPrinter { expression, _, continuation ->
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
                    expressions[0].transform { first ->
                        val first = first.indentWith(1)
                        val indent = first.split("\n").last().length + 2
                        expressions[1].transform { second ->
                            val second = second.indentWith(indent)
                            val firstLine = "$first $second"
                            expressions.subList(2, expressions.size).transform { rest ->
                                val nextLines = rest.joinToString("\n") {
                                    it.indentWith(indent, firstLine = true)
                                }
                                continuation("($firstLine\n$nextLines)")
                            }.bind()
                        }.bind()
                    }.bind()

                } else {
                    expressions.transform { it ->
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

fun SExpr.pretty(): Result<String> = with(prettyPrinter<String>()) { transform { it } }