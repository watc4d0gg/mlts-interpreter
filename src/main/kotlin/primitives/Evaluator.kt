//package primitives
//
//import Environment
//import Expr
//import Value
//import internals.BigStepContinuation
//import internals.PRIMITIVE_FUNCTIONS
//
//internal typealias Evaluation<T> = suspend Expr.(List<Expr>, Environment, BigStepContinuation<T>) -> T
//internal typealias Evaluator = Map<String, Evaluation<*>>
//
///**
// * A class describing a primitive function evaluator
// */
//internal class EvaluatorBuilder : MutableMap<String, Evaluation<*>> by mutableMapOf() {
//
//    operator fun Value.Primitive.plusAssign(evaluation: Evaluation<*>) {
//        this@EvaluatorBuilder[name] = evaluation
//    }
//}
//
//internal fun createEvaluations(builder: EvaluatorBuilder.() -> Unit): Evaluator = EvaluatorBuilder().apply(builder)
//
//@Suppress("UNCHECKED_CAST")
//private operator fun <T> Evaluator.get(primitive: Value.Primitive): Evaluation<T> = getValue(primitive.name) as Evaluation<T>
//
//context(Expr)
//internal suspend fun <T> Value.Primitive.eval(
//    arguments: List<Expr>,
//    environment: Environment,
//    continuation: BigStepContinuation<T>): T =
//    PRIMITIVE_FUNCTIONS.get<T>(this)(this@Expr, arguments, environment, continuation)
