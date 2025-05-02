//package primitives
//
//import Value
//import internals.EvalException
//import internals.eval
//
//// Logical operators
//private val NOT = Value.Primitive("not")
//private val AND = Value.Primitive("and")
//private val OR = Value.Primitive("or")
//private val IF = Value.Primitive("if")
//
//internal val LOGICAL_OPERATORS = createEvaluations {
//    // NOT
//    NOT += { arguments, environment, step, continuation ->
//        if (arguments.size != 1) {
//            throw EvalException("\'not\' requires exactly one argument")
//        }
//        val (e) = arguments
//        e.eval(environment, step) { v ->
//            when (v) {
//                is Value.Bool -> continuation(Value.Bool(!v.value))
//                else -> throw EvalException("$v is not a bool")
//            }
//        }
//    }
//
//    // AND -- short-circuiting
//    AND += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'and\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Bool -> when (v1.value) {
//                    true -> step(e2).eval(environment, step) { v2 ->
//                        when (v2) {
//                            is Value.Bool -> continuation(v2)
//                            else -> throw EvalException("$v2 is not a bool")
//                        }
//                    }
//
//                    false -> continuation(v1)
//                }
//
//                else -> throw EvalException("$v1 is not a bool")
//            }
//        }
//    }
//
//    // OR -- short-circuiting
//    OR += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'or\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Bool -> when (v1.value) {
//                    true -> continuation(v1)
//                    false -> {
//                        step(e2).eval(environment, step) { v2 ->
//                            when (v2) {
//                                is Value.Bool -> continuation(v2)
//                                else -> throw EvalException("$v2 is not a bool")
//                            }
//                        }
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not a bool")
//            }
//        }
//    }
//
//    // IF -- short-circuiting
//    IF += { arguments, environment, step, continuation ->
//        if (arguments.size != 3) {
//            throw EvalException("\'if\' requires exactly three arguments")
//        }
//        val (e1, e2, e3) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Bool -> when (v1.value) {
//                    true -> step(e2).eval(environment, step, continuation)
//                    false -> step(e3).eval(environment, step, continuation)
//                }
//
//                else -> throw EvalException("$v1 is not a bool")
//            }
//        }
//    }
//}