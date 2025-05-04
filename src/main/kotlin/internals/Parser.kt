package internals

import language.Binding
import language.Expr
import language.SExpr
import language.Value
import language.parser.Parser
import language.parser.parse
import language.parser.pretty

internal fun <T> defaultParser(): Parser<Expr, T> = Parser { expression, continuation ->
    result {
        when (expression) {
            is SExpr.LInt -> continuation(Value.Literal.Int(expression.value))
            is SExpr.LFloat -> continuation(Value.Literal.Float(expression.value))
            is SExpr.LBool -> continuation(Value.Literal.Bool(expression.value))
            is SExpr.LStr -> continuation(Value.Literal.Str(expression.value))
            is SExpr.LSym -> continuation(Expr.Id(expression.symbol))
            is SExpr.SList -> {
                val expressions = expression.expressions
                if (expressions.isEmpty()) {
                    throw ParseException("Cannot evaluate an empty s-expression")
                }
                val (expression, arguments) = expressions.headTail
                when (expression) {
                    is SExpr.LSym -> {
                        when (expression.symbol) {
                            "define" -> {
                                if (arguments.size != 2 && arguments.size != 3) {
                                    throw ParseException("\'define\' requires exactly two or three arguments")
                                }
                                if (arguments.size == 2) {
                                    val (name, body) = arguments
                                    if (name !is SExpr.LSym) {
                                        throw ParseException("\'${name.pretty().bind()}\' is an invalid name symbol")
                                    }
                                    body.parse {
                                        continuation(Expr.Define(name.symbol, it))
                                    }.bind()
                                } else {
                                    val (name, parameters, body) = arguments
                                    if (name !is SExpr.LSym) {
                                        throw ParseException("\'${name.pretty().bind()}\' is an invalid name symbol")
                                    }
                                    if (parameters !is SExpr.SList) {
                                        throw ParseException(
                                            "\'${
                                                name.pretty().bind()
                                            }\' parameters need to defined as an s-expression"
                                        )
                                    }
                                    val names = parameters.expressions.map {
                                        if (it !is SExpr.LSym) {
                                            throw ParseException(
                                                "\'${
                                                    it.pretty().bind()
                                                }\' is an invalid parameter name"
                                            )
                                        }
                                        it.symbol
                                    }
                                    body.parse { body ->
                                        continuation(Expr.Define(name.symbol, Expr.Lambda(names, body)))
                                    }.bind()
                                }
                            }

                            "lambda" -> {
                                if (arguments.size != 2) {
                                    throw ParseException("\'lambda\' requires exactly two arguments")
                                }
                                val (parameters, body) = arguments
                                if (parameters !is SExpr.SList) {
                                    throw ParseException("\'lambda\' parameters need to defined as an s-expression")
                                }
                                val names = parameters.expressions.map {
                                    if (it !is SExpr.LSym) {
                                        throw ParseException("\'${it.pretty().bind()}\' is not a valid parameter name")
                                    }
                                    it.symbol
                                }
                                body.parse { body -> continuation(Expr.Lambda(names, body)) }.bind()
                            }

                            "let" -> {
                                if (arguments.size != 2 && arguments.size != 3) {
                                    throw ParseException("\'let\' requires exactly two or three arguments")
                                }
                                if (arguments.size == 2) {
                                    val (variables, body) = arguments
                                    if (variables !is SExpr.SList) {
                                        throw ParseException("\'let\' variables need to defined as an s-expression")
                                    }
                                    variables.expressions.parse({ parseBinding(it) }) { bindings ->
                                        body.parse { body -> continuation(Expr.Let(bindings, body)) }.bind()
                                    }.bind()

                                } else {
                                    val (name, parameters, body) = arguments
                                    if (name !is SExpr.LSym) {
                                        throw ParseException("\'${name.pretty().bind()}\' is an invalid name symbol")
                                    }
                                    if (parameters !is SExpr.SList) {
                                        throw ParseException("\'let\' parameters need to defined as an s-expression")
                                    }
                                    parameters.expressions.parse({ parseBinding(it) }) { bindings ->
                                        val bindings = bindings.toMutableList()
                                        body.parse { body ->
                                            val lambda = Expr.Lambda(bindings.map { it.name }, body)
                                            bindings += Binding(name.symbol, lambda)
                                            if (bindings.distinctBy { it.name }.size != bindings.size) {
                                                throw ParseException(
                                                    "\'let\' bindings require to be distinct (${
                                                        bindings.joinToString(" ") { it.name }
                                                    }")
                                            }
                                            continuation(Expr.Let(bindings, lambda.body))
                                        }.bind()
                                    }.bind()
                                }
                            }

                            "letrec" -> {
                                if (arguments.size != 2) {
                                    throw ParseException("\'letrec\' requires exactly two arguments")
                                }
                                val (variables, body) = arguments
                                if (variables !is SExpr.SList) {
                                    throw ParseException("\'letrec\' variables need to defined as an s-expression")
                                }
                                variables.expressions.parse({ parseBinding(it) }) { bindings ->
                                    if (bindings.distinctBy { it.name }.size != bindings.size) {
                                        throw ParseException(
                                            "\'letrec\' bindings require to be distinct (${
                                                bindings.joinToString(" ") { it.name }
                                            }")
                                    }
                                    body.parse { body ->
                                        continuation(Expr.LetRec(bindings, body))
                                    }.bind()
                                }.bind()
                            }

                            else -> arguments.parse {
                                continuation(Expr.Apply(Expr.Id(expression.symbol), it))
                            }.bind()
                        }
                    }

                    is SExpr.SList -> expression.parse { function ->
                        arguments.parse { arguments ->
                            continuation(Expr.Apply(function, arguments))
                        }.bind()
                    }.bind()

                    else -> throw ParseException("\'${expression.pretty().bind()}\' does not parse to a valid expression"
                    )
                }
            }
        }
    }
}

context(parser: Parser<Expr, T>)
private fun <T> SExpr.parseBinding(continuation: Continuation<Binding, T>): Result<T> = result {
    if (this@parseBinding !is SExpr.SList) {
        throw ParseException("\'${pretty().bind()}\' is an invalid binding expression")
    }
    if (expressions.size != 2) {
        throw ParseException("Bindings require exactly two arguments")
    }
    val (name, value) = expressions
    if (name !is SExpr.LSym) {
        throw ParseException("\'${name.pretty().bind()}\' is an invalid name symbol")
    }
    value.parse { continuation(Binding(name.symbol, it)) }.bind()
}

internal class ParseException(message: String) : Exception("ParseError: $message")