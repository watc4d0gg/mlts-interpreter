import java.io.EOFException
import java.io.File
import kotlin.system.measureTimeMillis

fun main(vararg args: String) {
    val argMap = args.toList().chunked(2) { it[0] to it[1] }.toMap()

    argMap["-f"]?.let { filepath ->
        InputData(File(filepath))
            .parse()
            .forEach(::println)
        return
    }

    var displayTiming = false

    while (true) {
        print("lisp> ")
        try {
            when (val input = readlnOrNull()) {
                ":timing" -> {
                    displayTiming = !displayTiming
                }
                null, "quit", "exit" -> {
                    break
                }
                else -> {
                    val runTime = measureTimeMillis {
                        InputData(input)
                            .parse()
                            .forEach(::println)
                    }

                    if (displayTiming) {
                        println("Took ${runTime}ms")
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EOFException -> break
                else -> e.printStackTrace(System.out)
            }
        }
    }
}