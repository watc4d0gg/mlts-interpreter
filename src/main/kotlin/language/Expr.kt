package language

/**
 * AST
 */
sealed interface Expr {

    @JvmInline
    value class Id(val name: String) : Expr

    data class Define(val name: String, val expression: Expr) : Expr

    data class Lambda(val parameters: List<String>, val body: Expr) : Expr

    data class Apply(val function: Expr, val arguments: List<Expr>) : Expr

    data class Let(val bindings: List<Binding>, val body: Expr) : Expr {
        fun asPair(): Pair<List<Binding>, Expr> = bindings to body
    }

    data class LetRec(val bindings: List<Binding>, val body: Expr) : Expr {
        fun asPair(): Pair<List<Binding>, Expr> = bindings to body
    }
}

typealias Program = Sequence<Expr>

data class Binding(val name: String, val expression: Expr) {
    fun asPair(): Pair<String, Expr> = name to expression
}