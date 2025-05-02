//package primitives
//
//import Value
//import internals.EvalException
//import internals.eval
//
//// Comparisons
//private val EQ = Value.Primitive("=")
//private val NE = Value.Primitive("!=")
//private val LT = Value.Primitive("<")
//private val GT = Value.Primitive(">")
//private val LEQ = Value.Primitive("<=")
//private val GEQ = Value.Primitive(">=")
//
//internal val COMPARISON_OPERATORS = createEvaluations {
//    // EQUALITY
//    EQ += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'=\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value == v2.value))
//                        is Value.Float -> continuation(Value.Bool(v1.value.toDouble() == v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value == v2.value.toDouble()))
//                        is Value.Float -> continuation(Value.Bool(v1.value == v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Bool -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Bool -> continuation(Value.Bool(v1.value == v2.value))
//                        else -> throw EvalException("$v2 is not a bool")
//                    }
//                }
//
//                is Value.Str -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Str -> continuation(Value.Bool(v1.value == v2.value))
//                        else -> throw EvalException("$v2 is not a str")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int, float, bool nor str")
//            }
//        }
//    }
//
//    // NEGATION -- "desugared" into equality
//    NE += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'!=\' requires exactly two arguments")
//        }
//        EQ.eval(arguments, environment, step) { v ->
//            when (v) {
//                is Value.Bool -> continuation(Value.Bool(!v.value))
//                else -> throw EvalException("$v is not a bool")
//            }
//        }
//    }
//
//    // LESS THAN
//    LT += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'<\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value < v2.value))
//                        is Value.Float -> continuation(Value.Bool(v1.value < v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value < v2.value))
//                        is Value.Float -> continuation(Value.Bool(v1.value < v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Str -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Str -> continuation(Value.Bool(v1.value < v2.value))
//                        else -> throw EvalException("$v2 is not an str")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int, float nor str")
//            }
//        }
//    }
//
//    // LESS THAN OR EQUAL TO
//    LEQ += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'<=\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment, step) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value <= v2.value))
//                        is Value.Float -> continuation(Value.Bool(v1.value <= v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Int -> continuation(Value.Bool(v1.value <= v2.value))
//                        is Value.Float -> continuation(Value.Bool(v1.value <= v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Str -> e2.eval(environment, step) { v2 ->
//                    when (v2) {
//                        is Value.Str -> continuation(Value.Bool(v1.value <= v2.value))
//                        else -> throw EvalException("$v2 is not an str")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int, float nor str")
//            }
//        }
//    }
//
//    // GREATER THAN -- "desugared" in to less than or equal to
//    GT += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'>\' requires exactly two arguments")
//        }
//       LEQ.eval(arguments, environment, step) { v ->
//           when (v) {
//               is Value.Bool -> continuation(Value.Bool(!v.value))
//               else -> throw EvalException("$v is not a bool")
//           }
//        }
//    }
//
//    // GREATER THAN OR EQUAL TO -- "desugared" in to less than
//    GEQ += { arguments, environment, step, continuation ->
//        if (arguments.size != 2) {
//            throw EvalException("\'>=\' requires exactly two arguments")
//        }
//        LT.eval(arguments, environment, step) { v ->
//            when (v) {
//                is Value.Bool -> continuation(Value.Bool(!v.value))
//                else -> throw EvalException("$v is not a bool")
//            }
//        }
//    }
//}
//
