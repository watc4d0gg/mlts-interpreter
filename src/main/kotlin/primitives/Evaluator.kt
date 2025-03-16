package primitives

import Expr
import Value
import Environment
import internals.EvalContext

internal typealias PrimType = Expr.LSym
internal typealias Evaluation = EvalContext.(List<Expr>, Environment) -> Value
internal typealias Evaluator = Map<String, Evaluation>

/**
 * A class describing a primitive function evaluator
 */
internal class EvaluatorBuilder: MutableMap<String, Evaluation> by mutableMapOf() {

    operator fun PrimType.plusAssign(evaluation: Evaluation) {
        this@EvaluatorBuilder[symbol] = evaluation
    }

    operator fun EvalContext.invoke(primitive: PrimType, arguments: List<Expr>, environment: Environment) =
        get(primitive)(arguments, environment)
}

internal fun createEvaluations(builder: EvaluatorBuilder.() -> Unit): Evaluator = EvaluatorBuilder().apply(builder)

internal operator fun Evaluator.get(primitive: PrimType): Evaluation = getValue(primitive.symbol)
