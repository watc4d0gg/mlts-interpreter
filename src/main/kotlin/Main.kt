import internals.SyntaxException
import language.interpreter.Environment
import language.interpreter.EvaluationStrategy
import language.interpreter.prettyPrinter
import language.transform

import java.io.File
import kotlin.system.exitProcess

fun main(vararg args: String) {
    val argMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    args.toList().fold("") { lastArg, arg ->
        if (arg.startsWith("-")) {
            argMap[arg] = mutableListOf()
            arg
        } else {
            argMap[lastArg]?.add(arg)
            lastArg
        }
    }

    val environment = Environment(EvaluationStrategy.Lazy)

    // Interpret an input file
    argMap["-f"]?.let { (filepath) ->
        try {
            with(prettyPrinter<Unit>()) {
                File(filepath).read().parse().forEach {
                    it.onSuccess { expression ->
                        expression.transform(environment, ::println)
                            .onFailure { error -> System.err.println(error.stackTraceToString()) }
                    }
                }
            }

        } catch (e: SyntaxException) {
            System.err.println(e.message)
            exitProcess(1)
        }
        return
    }

    val debug = argMap["-g"] != null


    // Initialise REPL
//    while (true) {
//        print("MLTS> ")
//        try {
//            when (val input = readlnOrNull()) {
//                null, "quit", "exit", -> break
//                "" -> continue
//                else -> {
//                    if (input.startsWith(":timing")) {
//                        val time = measureTimeMillis {
//                            input.substringAfter(":timing ").parse().eval(environment).forEach { println(it) }
//                        }
//                        println("Took ${time}ms")
//                    } else
//                    if (input.startsWith(":lex")) {
//                        input.substringAfter(":lex ")
//                            .tokenize()
//                            .asSequence()
//                            .toCollection(LinkedList())
//                            .apply { println(this) }
//                    } else if (input.startsWith(":parse")) {
//                        input.substringAfter(":parse ").parse().apply { println(this) }
//                    } else if (input.startsWith(":debug") || debug) {
//                        input.substringAfter(":debug ").parse().debug(environment).forEach { println(it) }
//                    } else if (input.startsWith(":env")) {
//                        println(environment.pretty())
//                    } else if (input.startsWith(":clearenv")) {
//                        environment.clear()
//                    } else {
//                        input.parse().eval(environment).forEach { println(it) }
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            when (e) {
//                is EOFException -> break
//                is SyntaxException -> println(e.message)
//                else -> {
//                    e.printStackTrace()
//                    break
//                }
//            }
//        }
//    }
}