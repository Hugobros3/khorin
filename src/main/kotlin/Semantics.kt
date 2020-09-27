import IRNode.Expression.PrimOp.PrimOps.*

import Value.Literal.*

fun Program.evaluate(body: IRNode.Body, environment: Environment): Pair<IRNode.Body, Environment> {
    val calledClosure = evaluate(body.callee, environment) as Value.Closure
    val argumentsValues = body.arguments.map { evaluate(it, environment) }
    val newEnvironment = calledClosure.environment.addOrReplaceKey(calledClosure.fn, argumentsValues)
    return Pair(calledClosure.fn.second, newEnvironment)
}

fun Program.evaluate(expression: IRNode.Expression, environment: Environment): Value = when(expression) {
    is IRNode.Expression.PrimOp -> evaluate(expression, expression.operands.map { evaluate(it, environment) })
    is IRNode.Expression.Abstraction -> Value.Closure(labels[expression.fnName] ?: error("Unbound function name: "+expression.fnName), expression.fnName, environment)
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