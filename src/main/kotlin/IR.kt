typealias Function = Pair<Type.FnType, IRNode.Body>
typealias Environment = Map<Function, List<Value>>

class Program(val labels: Map<String, Function>) {
    override fun toString() = labels.map { (k, v) -> "$k : ${v.first} = ${v.second}" }.joinToString("\n")
}

/** Do we consider types as part of the (main) IR graph ? We don't have dependent types so far, so I'd say no */
sealed class Type {
    sealed class PrimitiveType : Type() {
        object Int : PrimitiveType() {
            override fun toString() = "int"
        }

        object Bool : PrimitiveType() {
            override fun toString() = "bool"
        }
    }

    data class FnType(val parametersTypes: List<Type>) : Type() {
        override fun toString() = "fn(" + parametersTypes.joinToString(", ") { it.toString() } + ")"
    }
}

sealed class IRNode {
    open class Body(val callee: Expression, val arguments: List<Expression>) : IRNode() {
        override fun toString() = "$callee(" + arguments.joinToString(", ") { it.toString() } + ")"
    }

    class Intrinsic(val intrinsicOp: IntrinsicOp, argumentsTypes: List<Type>, arguments: List<Expression>) :
        Body(Expression.QuoteLiteral(Value.Literal.Bottom(Type.FnType(argumentsTypes))), arguments) {

        override fun toString() = "$intrinsicOp(" + arguments.joinToString(", ") { it.toString() } + ")"

        enum class IntrinsicOp {
            BRANCH
        }
    }

    sealed class Expression: IRNode() {
        data class PrimOp(val op: PrimOps, val operands: List<Expression>) : Expression() {
            enum class PrimOps(val arity: Int, val symbol: String) {
                ADD(2, "+"),
                SUB(2, "-"),
                MUL(2, "*"),
                DIV(2, "/"),
                MOD(2, "%"),
                AND(2, "⋀"),
                OR(2, "⋁"),
                NOT(1, "!"),
                INF(2, "<"),
                INFEQ(2, "<="),
                EQ(2, "="),
            }

            override fun toString() = if (op.arity == 1)
                "(" + op.symbol + operands[0] + ")"
            else
                "(" + operands[0] + op.symbol + operands[1] + ")"
        }

        data class Abstraction(/*val fn: Function*/val fnName: String) : Expression() { override fun toString() = fnName}
        data class Parameter(/*val fn: Function*/val fnName: String, val i: Int) : Expression() { override fun toString() = "$fnName.args[$i]" }

        /** Not in the paper but you'll obviously need that */
        data class QuoteLiteral(val lit: Value.Literal) : Expression() {
            override fun toString() = lit.toString()
        }

        data class Cast(val source: Expression, val dstType: Type) : Expression() {
            override fun toString() = "($source as $dstType)"
        }
    }
}

sealed class Value {
    sealed class Literal(val type: Type) : Value() {
        data class Bottom(private val type_: Type) : Literal(type_) {
            override fun toString() = "⊥"
        }

        data class IntValue(val value: Int) : Literal(Type.PrimitiveType.Int) {
            override fun toString() = "$value"
        }

        data class BoolValue(val value: Boolean) : Literal(Type.PrimitiveType.Bool) {
            override fun toString() = "$value"
        }
    }

    data class Closure(val fn: Function, val fnName: String, val environment: Environment) : Value() {
        override fun toString() = "<$fnName: ${fn.first}, $environment>"
    }
}