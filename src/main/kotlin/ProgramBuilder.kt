import Type.*

val int = PrimitiveType.Int
val bool = PrimitiveType.Bool
fun fn_type(vararg parameters: Type) = FnType(parameters.toList())

/** Helper DSL to write thorin programs inline without too much complexity */
fun program(code: ProgramBuilder.() -> Unit): Program = ProgramBuilder().apply(code).finish()

class ProgramBuilder {
    val functions = mutableMapOf<String, Function>()

    class FnBuilder(val name: String, val type: FnType) {
        lateinit var function: Function

        fun call(callee: IRNode.Expression, vararg arguments: IRNode.Expression) {
            function = Pair(type, IRNode.Body(callee, arguments.toList()))
        }

        fun branch(condition: IRNode.Expression, ifTrue: IRNode.Expression, ifFalse: IRNode.Expression) {
            val intrinsic_signature = fn_type(bool, fn_type(), fn_type())
            function = Pair(type, IRNode.Intrinsic(
                IRNode.Intrinsic.IntrinsicOp.BRANCH,
                intrinsic_signature.parametersTypes, listOf(condition, ifTrue, ifFalse)
            )
            )
        }

        fun param(i: Int) = IRNode.Expression.Parameter(name, i)
    }
    fun function(name: String, type: FnType, code: FnBuilder.() -> Unit) {
        functions[name] = FnBuilder(name, type).apply(code).function
    }

    fun lit(int: Int) = IRNode.Expression.QuoteLiteral(Value.Literal.IntValue(int))
    fun lit(bool: Boolean) = IRNode.Expression.QuoteLiteral(Value.Literal.BoolValue(bool))

    fun fn(name: String) = IRNode.Expression.Abstraction(name)

    fun add(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.ADD, listOf(lhs, rhs))
    fun sub(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.SUB, listOf(lhs, rhs))
    fun mul(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.MUL, listOf(lhs, rhs))
    fun div(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.DIV, listOf(lhs, rhs))
    fun mod(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.MOD, listOf(lhs, rhs))

    fun and(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.AND, listOf(lhs, rhs))
    fun or(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.OR, listOf(lhs, rhs))
    fun not(b: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.NOT, listOf(b))

    fun inf(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.INF, listOf(lhs, rhs))
    fun infeq(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.INFEQ, listOf(lhs, rhs))
    fun eq(lhs: IRNode.Expression, rhs: IRNode.Expression) = IRNode.Expression.PrimOp(IRNode.Expression.PrimOp.PrimOps.EQ, listOf(lhs, rhs))

    fun finish() = Program(functions)
}