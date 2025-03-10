import java.io.EOFException
import java.io.File
import java.util.*
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main(vararg args: String) {
    val argMap = args.toList().chunked(2) { it[0] to it[1] }.toMap()

    // Interpret an input file
    argMap["-f"]?.let { filepath ->
        try {
            File(filepath)
                .parse()
                .forEach { println(it.toPrettyString()) }
        } catch (e: SyntaxException) {
            System.err.println(e.message)
            exitProcess(1)
        }
        return
    }

    // Initialise REPL
    while (true) {
        print("MLTS> ")
        try {
            when (val input = readlnOrNull()) {
                null, "quit", "exit" -> {
                    break
                }
                else -> {
                    if (input.startsWith(":timing")) {
                        val time = measureTimeMillis {
                            input.substringAfter(":timing ")
                                .parse()
                                .forEach { println(it.toPrettyString()) }
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
                    } else {
                        input.parse().forEach { println(it.toPrettyString()) }
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is SyntaxException -> println(e.message)
                is EOFException -> break
                else -> {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}