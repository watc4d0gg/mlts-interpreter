package internals

//internal val PRIMITIVE_FUNCTIONS: Evaluator =
//    ARITHMETIC_OPERATORS + COMPARISON_OPERATORS + LOGICAL_OPERATORS + STRING_OPERATIONS + BUILTIN_FUNCTIONS
//
//internal suspend fun <T> Expr.evalWithDefine(environment: Environment, continuation: BigStepContinuation<T>): T? =
//    when (this) {
//        is Expr.Define -> {
//            environment[name] = Thunk(expression, environment)
//            null
//        }
//        else -> eval(environment, continuation)
//    }
//
//internal suspend fun <T> Expr.eval(environment: Environment, k: BigStepContinuation<T>): T = when (this) {
//    is Expr.Literal.Int -> k(Value.Int(value))
//    is Expr.Literal.Float -> k(Value.Float(value))
//    is Expr.Literal.Bool -> k(Value.Bool(value))
//    is Expr.Literal.Str -> k(Value.Str(value))
//    is Expr.Id -> when (name) {
//        in environment -> {
//            val thunk = environment.getValue(name)
//            thunk.get()?.let { k(it) }
//                ?: thunk.let { (expression, thunkEnvironment) ->
//                    expression.eval(thunkEnvironment) {
//                        thunk.result = it
//                        k(it)
//                    }
//                }
//        }
//        in PRIMITIVE_FUNCTIONS -> k(Value.Primitive(name))
//        else -> throw EvalException("\'$name\' is an unknown symbol")
//    }
//    is Expr.Define -> throw EvalException("Top-level declaration \'$name\' is not allowed inside an expression")
//    is Expr.Lambda -> k(Value.Closure(parameters, body, environment))
//    is Expr.Apply -> function.eval(environment) {
//        when (it) {
//            is Value.Closure -> {
//                if (it.parameters.size != arguments.size) {
//                    throw EvalException("Expected ${it.parameters.size} parameters, got ${arguments.size} ($arguments)")
//                }
//                it.body.eval(environment.extend(it.parameters.zip(arguments), environment), k)
//            }
//            is Value.Primitive -> it.eval(arguments, environment, k)
//            else -> throw EvalException("\'$it\' is not a function nor a primitive")
//        }
//    }
//    is Expr.Let -> body.eval(environment.extend(bindings), k)
//    is Expr.LetRec -> body.eval(environment.extendRecursive(bindings), k)
//}
//
//internal class EvalException(message: String) : Exception("EvalError: $message")

//internal data class ExprScope(val expression: Expr, val environment: Environment, var shadow: Thunk? = null) {
//    val thunk: Thunk = Thunk(expression, environment)
//
//    override fun toString(): String = "[${expression.pretty()} ${environment.pretty()} ${shadow}]"
//}

//data class Debug private constructor(
//    private val scopes: Deque<ExprScope>,
//    private val scopeMap: MutableMap<Thunk, ExprScope> = mutableMapOf(),
//    private val substitutions: MutableMap<Thunk, Value> = mutableMapOf(),
//    private var shortCircuit: Boolean = false
//) : Step {
//
//    constructor() : this(LinkedList())
//
//    context(Expr)
//    override suspend fun invoke(environment: Environment) = suspendCoroutine {
//        val prevScope = if (scopes.isNotEmpty()) scopes.peek() else null
//        val shouldShortCircuit = scopes.isEmpty() || shortCircuit
//        ExprScope(this@Expr, environment).let { scope ->
//            scopes.push(scope)
//            scopeMap[scope.thunk] = scope
//            if (shouldShortCircuit) {
//                prevScope?.shadow = scope.thunk
//                shortCircuit = false
//                step()
//            }
//        }
//        it.resumeWith(Result.success(Unit))
//    }
//
//    context(Expr)
//    override suspend operator fun invoke(value: Value): Value = suspendCoroutine {
//        val scope = scopes.pop()
//        scopeMap.remove(scope.thunk)
//        if ((this@Expr is Expr.Id && this@Expr.name in scope.environment
//                    || this@Expr !is Expr.Literal<*> && value !is Value.Closure)
//            && substitutions.put(scope.thunk, value) == null
//            && scope.shadow == null
//            && !startingThunk.reachesValue()) {
//            step()
//        }
//        it.resumeWith(Result.success(value))
//    }
//
//    context(Expr)
//    override suspend fun invoke(substitution: Expr): Expr = suspendCoroutine {
//        shortCircuit = true
//        it.resumeWith(Result.success(substitution))
//    }
//
//    private fun step() {
//        println(startingThunk.pretty())
//        while (true) {
//            print("Step? ")
//            when (readlnOrNull()) {
//                null, "exit", "quit" -> throw DebugStopException()
//                ":env" -> {
//                    println(scopes.peek().environment.pretty())
//                    System.out.flush()
//                }
//
//                else -> {
//                    System.out.flush()
//                    break
//                }
//            }
//        }
//    }
//
//    private val startingThunk: Thunk get() = scopes.last.thunk
//
//    private tailrec fun Thunk.reachesValue(): Boolean {
//        if (this in substitutions) {
//            return true
//        }
//        val scope = scopeMap[this]
//        if (scope?.shadow == null) {
//            return false
//        }
//        return scope.shadow!!.reachesValue()
//    }
//
//    private fun Thunk.pretty() = pretty(
//        from = { it.expression },
//        to = { (_, environment), expression -> Thunk(expression, environment) }) transform@{
//        var current = it
//        while (current in substitutions || it in scopeMap) {
//            if (it in substitutions) {
//                return@transform Either.Left("${substitutions[this]}")
//            }
//            if (it in scopeMap) {
//                scopeMap[it]?.shadow?.let { shadow -> current = shadow } ?: break
//            }
//        }
//        if (expression is Expr.Id) {
//            val thunk = environment[expression.name]
//            if (thunk != null && thunk in substitutions) {
//                return@transform Either.Right(thunk)
//            }
//        }
//        Either.Right(this)
//    }
//}
//
//internal class DebugStopException : Exception()