package language

/**
 * A class containing all the parseable expressions
 */
sealed interface SExpr {

    data class LInt(val value: Int) : SExpr {

        override fun toString() = "LInt $value"
    }

    data class LFloat(val value: Double) : SExpr {

        override fun toString() = "LFloat $value"
    }

    data class LBool(val value: Boolean) : SExpr {

        override fun toString() = "LBool $value"
    }

    data class LStr(val value: String) : SExpr {

        override fun toString() = "LStr \"$value\""
    }

    data class LSym(val symbol: String) : SExpr {

        override fun toString() = "LSym $symbol"
    }

    data class SList(val expressions: List<SExpr>) : SExpr {

        override fun toString() = "SList $expressions"
    }
}