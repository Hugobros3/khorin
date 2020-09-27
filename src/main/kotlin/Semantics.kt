import IRNode.Expression.PrimOp.PrimOps.*

import Value.Literal.*

fun Program.evaluate(continuation: IRNode.Continuation, environment: Environment): Pair<IRNode.Continuation, Environment> {
    val argumentsValues = continuation.body.arguments.map { evaluate(it, environment) }
    return when(continuation.body) {
        is IRNode.Body.Call -> {
            val calledClosure = evaluate(continuation.body.callee, environment) as Value.Closure
            val newEnvironment = calledClosure.environment.addOrReplaceKey(calledClosure.fn, argumentsValues)
            Pair(calledClosure.fn, newEnvironment)
        }
        is IRNode.Body.Intrinsic -> {
            when(continuation.body.intrinsicOp) {
                IRNode.Body.Intrinsic.IntrinsicOp.BRANCH -> {
                    val jumpTo = argumentsValues[if(boolValue(argumentsValues[0])) 2 else 1] as Value.Closure
                    Pair(jumpTo.fn, jumpTo.environment)
                }
            }
        }
    }
}

fun Program.evaluate(expression: IRNode.Expression, environment: Environment): Value = when(expression) {
    is IRNode.Expression.PrimOp -> evaluate(expression, expression.operands.map { evaluate(it, environment) })
    is IRNode.Expression.Abstraction -> Value.Closure(labels[expression.fnName] ?: error("Unbound function name: "+expression.fnName), environment)
    is IRNode.Expression.Parameter -> environment[labels[expression.fnName] ?: error("Unbound function name: "+expression.fnName)]!![expression.i]
    is IRNode.Expression.QuoteLiteral -> expression.lit
    is IRNode.Expression.Cast -> castValue(evaluate(expression.source, environment), expression.dstType)
}

fun castValue(value: Value, dstType: Type): Value = when (dstType) {
    Type.PrimitiveType.Bool -> BoolValue((value as IntValue).value > 0)
    else -> IntValue(if ((value as BoolValue).value) 1 else 0)
}

fun evaluate(primOp: IRNode.Expression.PrimOp, ops: List<Value>) : Value {
    fun intValues() = ops.map { (it as IntValue).value }
    fun boolValues() = ops.map { (it as BoolValue).value }

    return when(primOp.op) {
        ADD -> IntValue(intValues()[0] + intValues()[1])
        SUB -> IntValue(intValues()[0] - intValues()[1])
        MUL -> IntValue(intValues()[0] * intValues()[1])
        DIV -> IntValue(intValues()[0] / intValues()[1])
        MOD -> IntValue(intValues()[0] % intValues()[1])
        AND -> BoolValue(boolValues()[0] && boolValues()[1])
        OR  -> BoolValue(boolValues()[0] || boolValues()[1])
        NOT -> BoolValue(!boolValues()[0])
        INF -> BoolValue(intValues()[0] < intValues()[1])
        INFEQ -> BoolValue(intValues()[0] <= intValues()[1])
        EQ -> BoolValue(intValues()[0] == intValues()[1])
    }
}