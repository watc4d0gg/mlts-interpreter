//package primitives
//
//import Value
//import internals.EvalException
//import internals.eval
//
//// Arithmetic
//private val ADD = Value.Primitive("+")
//private val SUB = Value.Primitive("-")
//private val MUL = Value.Primitive("*")
//private val DIV = Value.Primitive("/")
//private val MOD = Value.Primitive("%")
//
//internal val ARITHMETIC_OPERATORS = createEvaluations {
//    // ADDITION
//    ADD += { arguments, environment, k ->
//        if (arguments.size != 2) {
//            throw EvalException("\'+\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Int(v1.value + v2.value))
//                        is Value.Float -> k(Value.Float(v1.value + v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Float(v1.value + v2.value))
//                        is Value.Float -> k(Value.Float(v1.value + v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int nor a float")
//            }
//        }
//    }
//
//    // NEGATION/SUBTRACTION
//    SUB += { arguments, environment, k ->
//        if (arguments.size != 1 && arguments.size != 2) {
//            throw EvalException("\'-\' requires exactly one or two arguments")
//        }
//        if (arguments.size == 1) {
//            val (e) = arguments
//            e.eval(environment) { v ->
//                when (v) {
//                    is Value.Int -> k(Value.Int(-v.value))
//                    is Value.Float -> k(Value.Float(-v.value))
//                    else -> throw EvalException("$v is not an int nor a float")
//                }
//            }
//        } else {
//            val (e1, e2) = arguments
//            e1.eval(environment) { v1 ->
//                when (v1) {
//                    is Value.Int -> e2.eval(environment) { v2 ->
//                        when (v2) {
//                            is Value.Int -> k(Value.Int(v1.value - v2.value))
//                            is Value.Float -> k(Value.Float(v1.value - v2.value))
//                            else -> throw EvalException("$v2 is not an int nor a float")
//                        }
//                    }
//
//                    is Value.Float -> e2.eval(environment) { v2 ->
//                        when (v2) {
//                            is Value.Int -> k(Value.Float(v1.value - v2.value))
//                            is Value.Float -> k(Value.Float(v1.value - v2.value))
//                            else -> throw EvalException("$v2 is not an int nor a float")
//                        }
//                    }
//
//                    else -> throw EvalException("$v1 is not an int nor a float")
//                }
//            }
//        }
//    }
//
//    // MULTIPLICATION
//    MUL += { arguments, environment, k ->
//        if (arguments.size != 2) {
//            throw EvalException("\'*\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Int(v1.value * v2.value))
//                        is Value.Float -> k(Value.Float(v1.value * v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Float(v1.value * v2.value))
//                        is Value.Float -> k(Value.Float(v1.value * v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int nor a float")
//            }
//        }
//    }
//
//    // DIVISION
//    DIV += { arguments, environment, k ->
//        if (arguments.size != 2) {
//            throw EvalException("\'/\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Float(v1.value.toDouble() / v2.value.toDouble()))
//                        is Value.Float -> k(Value.Float(v1.value / v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                is Value.Float -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Float(v1.value / v2.value))
//                        is Value.Float -> k(Value.Float(v1.value / v2.value))
//                        else -> throw EvalException("$v2 is not an int nor a float")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int nor a float")
//            }
//        }
//    }
//
//    // MODULO
//    MOD += { arguments, environment, k ->
//        if (arguments.size != 2) {
//            throw EvalException("\'%\' requires exactly two arguments")
//        }
//        val (e1, e2) = arguments
//        e1.eval(environment) { v1 ->
//            when (v1) {
//                is Value.Int -> e2.eval(environment) { v2 ->
//                    when (v2) {
//                        is Value.Int -> k(Value.Int(v1.value % v2.value))
//                        else -> throw EvalException("$v2 is not an int")
//                    }
//                }
//
//                else -> throw EvalException("$v1 is not an int")
//            }
//        }
//    }
//}