import IRNode.Expression.PrimOp.PrimOps.*
import util.bool
import util.fn_type

class TypeException(msg: String) : Exception(msg)

fun type(program: Program) = program.labels.forEach { program.type(it.value) }

fun Program.type(function: IRNode.Continuation) {
    val calleeSignature = signature(function.body)
    if(calleeSignature !is Type.FnType)
        throw TypeException("In function '${function.name}', callee does not type (should be of type Fn, is $calleeSignature)")

    calleeSignature.parametersTypes.zip(function.body.arguments).forEachIndexed { i, (pt, arg) ->
        if (type(arg) != pt)
            throw TypeException("In function '${function.name}', callee argument $i does not type (is ${type(arg)}, should be $pt)")
    }
}

fun Program.signature(body: IRNode.Body) = when(body) {
    is IRNode.Body.Call -> type(body.callee)
    is IRNode.Body.Intrinsic -> when(body.intrinsicOp) {
        IRNode.Body.Intrinsic.IntrinsicOp.BRANCH -> fn_type(bool, fn_type(), fn_type())
    }
}

fun Program.type(expression: IRNode.Expression): Type = when (expression) {
    is IRNode.Expression.PrimOp -> type(expression)
    is IRNode.Expression.Abstraction -> (labels[expression.fnName]
        ?: error("Unbound function name: " + expression.fnName)).signature
    is IRNode.Expression.Parameter -> (labels[expression.fnName]
        ?: error("Unbound function name: " + expression.fnName)).signature.parametersTypes[expression.i]

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
    // Only allow util.getInt <-> util.getBool casts
    val sourceType = type(cast.source)
    if (sourceType == int_t && cast.dstType == bool_t)
        return bool_t
    if (sourceType == bool_t && cast.dstType == int_t)
        return int_t
    throw TypeException("Unsupported cast: ${cast.source} to ${cast.dstType}")
}