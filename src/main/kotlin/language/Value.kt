package language

import language.interpreter.Environment
import language.interpreter.pretty

sealed interface Value : Expr {

    sealed interface Literal<T> : Expr {

        val value: T

        @JvmInline
        value class Int(override val value: kotlin.Int) : Literal<kotlin.Int>

        @JvmInline
        value class Float(override val value: Double) : Literal<Double>

        @JvmInline
        value class Bool(override val value: Boolean) : Literal<Boolean>

        @JvmInline
        value class Str(override val value: String) : Literal<String>
    }

    // TODO: implement side-effects using algebraic effects and handlers instead
    data object Void : Value {
        override fun toString() = "<void>"
    }

    data class Closure(val parameters: List<String>, val body: Expr, val environment: Environment) : Value {
        override fun toString(): String = "{lambda ${parameters.joinToString(" ")}. ${body.pretty(environment).getOrThrow()}, $environment}"
    }

    @JvmInline
    value class Primitive(val name: String) : Value {
        override fun toString(): String = name
    }

    data class Thunk(var expression: Expr, val environment: Environment, var result: Value? = null) : Value {
        override fun toString(): String = expression.pretty(environment).getOrThrow()
    }
}