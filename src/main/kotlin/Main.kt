import internals.*
import internals.DebugStopException
import internals.SyntaxException
import java.io.EOFException
import java.io.File
import java.util.*
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

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

    // Interpret an input file
    argMap["-f"]?.let { (filepath) ->
        try {
            File(filepath).parse().eval().forEach { println(it) }
        } catch (e: SyntaxException) {
            System.err.println(e.message)
            exitProcess(1)
        }
        return
    }

    val debug = argMap["-g"] != null
    val environment: Environment = mutableMapOf()

    // Initialise REPL
    while (true) {
        print("MLTS> ")
        try {
            when (val input = readlnOrNull()) {
                null, "quit", "exit", -> break
                "" -> continue
                else -> {
                    if (input.startsWith(":timing")) {
                        val time = measureTimeMillis {
                            input.substringAfter(":timing ").parse().eval(environment).forEach { println(it) }
                        }
                        println("Took ${time}ms")
                    } else if (input.startsWith(":lex")) {
                        input.substringAfter(":lex ")
                            .tokenize()
                            .asSequence()
                            .toCollection(LinkedList())
                            .apply { println(this) }
                    } else if (input.startsWith(":parse")) {
                        input.substringAfter(":parse ").parse().apply { println(this) }
                    } else if (input.startsWith(":debug") || debug) {
                        input.substringAfter(":debug ").parse().debug(environment).forEach { println(it) }
                    } else if (input.startsWith(":env")) {
                        println(environment.prettyString)
                    } else if (input.startsWith(":clearenv")) {
                        environment.clear()
                    } else {
                        input.parse().eval(environment).forEach { println(it) }
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EOFException -> break
                is SyntaxException, is EvalException -> println(e.message)
                is DebugStopException -> {
                    println("Stopping the debugger...")
                    continue
                }
                else -> {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}