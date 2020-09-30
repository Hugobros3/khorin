package util

import IRNode.*
import IRNode.Body.Intrinsic.IntrinsicOp.*
import IRNode.Expression.*
import IRNode.Expression.PrimOp.PrimOps.*
import Program
import Type
import Type.FnType
import Type.PrimitiveType
import Value

val int = PrimitiveType.Int
val bool = PrimitiveType.Bool
fun fn_type(vararg parameters: Type) = FnType(parameters.toList())

/** Helper DSL to write thorin programs inline without too much complexity */
fun program(code: ProgramBuilder.() -> Unit): Program = ProgramBuilder().apply(code).finish()

class ProgramBuilder {
    val functions = mutableMapOf<String, Continuation>()

    class FnBuilder(val name: String, val type: FnType) {
        lateinit var function: Continuation

        fun call(callee: Expression, vararg arguments: Expression) {
            function = Continuation(name, type, Body.Call(callee, arguments.toList()))
        }

        fun branch(condition: Expression, ifTrue: Expression, ifFalse: Expression) {
            function = Continuation(name, type, Body.Intrinsic(BRANCH, listOf(condition, ifTrue, ifFalse)))
        }

        fun param(i: Int) = Expression.Parameter(name, i)
    }

    fun function(name: String, type: FnType, code: FnBuilder.() -> Unit) {
        functions[name] = FnBuilder(name, type).apply(code).function
    }

    fun lit(int: Int) = QuoteLiteral(Value.Literal.IntValue(int))
    fun lit(bool: Boolean) = QuoteLiteral(Value.Literal.BoolValue(bool))

    fun fn(name: String) = Abstraction(name)

    fun add(lhs: Expression, rhs: Expression) = PrimOp(ADD, listOf(lhs, rhs))
    fun sub(lhs: Expression, rhs: Expression) = PrimOp(SUB, listOf(lhs, rhs))
    fun mul(lhs: Expression, rhs: Expression) = PrimOp(MUL, listOf(lhs, rhs))
    fun div(lhs: Expression, rhs: Expression) = PrimOp(DIV, listOf(lhs, rhs))
    fun mod(lhs: Expression, rhs: Expression) = PrimOp(MOD, listOf(lhs, rhs))

    fun and(lhs: Expression, rhs: Expression) = PrimOp(AND, listOf(lhs, rhs))
    fun or(lhs: Expression, rhs: Expression) = PrimOp(OR, listOf(lhs, rhs))
    fun not(b: Expression) = PrimOp(NOT, listOf(b))

    fun inf(lhs: Expression, rhs: Expression) = PrimOp(INF, listOf(lhs, rhs))
    fun infeq(lhs: Expression, rhs: Expression) = PrimOp(INFEQ, listOf(lhs, rhs))
    fun eq(lhs: Expression, rhs: Expression) = PrimOp(EQ, listOf(lhs, rhs))

    fun finish() = Program(functions)
}