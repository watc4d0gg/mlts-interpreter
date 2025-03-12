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
            File(filepath).parse().eval()
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
                null, "quit", "exit" -> break
                else -> {
                    if (input.startsWith(":timing")) {
                        val time = measureTimeMillis {
                            input.substringAfter(":timing ")
                                .parse()
                                .eval(environment = environment)
                                .forEach { println(it) }
                        }
                        println("Took ${time}ms")
                    } else if (input.startsWith(":lex")) {
                        Tokenizer(InputData(input.substringAfter(":lex ")))
                            .asSequence()
                            .map { it.value }
                            .toCollection(LinkedList())
                            .apply { println(this) }
                    } else if (input.startsWith(":parse")) {
                        input.substringAfter(":parse ")
                            .parse()
                            .apply { println(this) }
                    } else if (input.startsWith(":debug")) {
                        input.substringAfter(":debug ")
                            .parse()
                            .eval(environment = environment, debug = true)
                            .forEach { println(it) }
                    } else {
                        input.parse()
                            .eval(environment = environment, debug = debug)
                            .forEach { println(it) }
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is SyntaxException -> println(e.message)
                is DebugStopException -> {
                    println("Stopping the debugger...")
                    continue
                }
                is EOFException -> break
                else -> {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}