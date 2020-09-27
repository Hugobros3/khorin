import IRNode.Expression.PrimOp.PrimOps.*

class TypeException(msg: String) : Exception(msg)

fun type(program: Program) = program.labels.forEach { program.type(it.key, it.value) }

fun Program.type(functionName: String, function: Function) {
    val calleeType = type(function.second.callee)
    if(calleeType !is Type.FnType)
        throw TypeException("In function '$functionName', callee does not type (should be of type Fn, is $calleeType)")

    calleeType.parametersTypes.zip(function.second.arguments).forEachIndexed { i, (pt, arg) ->
        if (type(arg) != pt)
            throw TypeException("In function '$functionName', callee argument $i does not type (is ${type(arg)}, should be $pt)")
    }
}

fun Program.type(expression: IRNode.Expression): Type = when (expression) {
    is IRNode.Expression.PrimOp -> type(expression)
    is IRNode.Expression.Abstraction -> (labels[expression.fnName]
        ?: error("Unbound function name: " + expression.fnName)).first
    is IRNode.Expression.Parameter -> (labels[expression.fnName]
        ?: error("Unbound function name: " + expression.fnName)).first.parametersTypes[expression.i]

    is IRNode.Expression.QuoteLiteral -> expression.lit.type
    is IRNode.Expression.Cast -> type(expression)
}

private val int_t = Type.PrimitiveType.Int
private val bool_t = Type.PrimitiveType.Bool

fun Program.type(primOp: IRNode.Expression.PrimOp): Type {
    fun validate(guard: Boolean, message: String = "") {
        if (!guard)
            throw TypeException(message)
    }

    validate(primOp.operands.size == primOp.op.arity, "${primOp.op} takes ${primOp.op.arity} operands")
    when (primOp.op) {
        ADD, SUB, MUL, DIV, MOD -> {
            validate(primOp.operands.all { type(it) == int_t }, "${primOp.op} takes 2 integer operands")
            return int_t
        }
        AND, OR -> {
            validate(primOp.operands.all { type(it) == bool_t }, "${primOp.op} takes 2 boolean operands")
            return bool_t
        }
        NOT -> {
            validate(primOp.operands.all { type(it) == bool_t }, "${primOp.op} takes 1 boolean operands")
            return bool_t
        }
        INF, INFEQ, EQ -> {
            validate(primOp.operands.all { type(it) == int_t }, "${primOp.op} takes 2 integer operands")
            return bool_t
        }
    }
}

fun Program.type(cast: IRNode.Expression.Cast): Type {
    // Only allow int <-> bool casts
    val sourceType = type(cast.source)
    if (sourceType == int_t && cast.dstType == bool_t)
        return bool_t
    if (sourceType == bool_t && cast.dstType == int_t)
        return int_t
    throw TypeException("Unsupported cast: ${cast.source} to ${cast.dstType}")
}