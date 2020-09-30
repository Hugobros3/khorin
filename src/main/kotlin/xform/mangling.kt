package xform

import IRNode
import IRNode.*
import Program
import scope

typealias M = Map<IRNode, IRNode>

fun Program.mangle(b: Body, m: M): Body = when(b) {
    is Body.Call -> Body.Call(mangle(b.callee, m), b.arguments.map { mangle(it, m) })
    is Body.Intrinsic -> Body.Intrinsic(b.intrinsicOp, b.arguments.map { mangle(it, m) })
}

fun Program.mangle(e: Expression, m: M): Expression = m[e] as? Expression ?: when(e) {
    is Expression.PrimOp -> Expression.PrimOp(e.op, e.operands.map { mangle(it, m) })
    is Expression.Abstraction,
    is Expression.Parameter,
    is Expression.QuoteLiteral -> e
    is Expression.Cast -> Expression.Cast(mangle(e.source, m), e.dstType)
}

fun Program.mangle(entry: Continuation, t: Type.FnType, m_: M): Program {
    val newLabels = labels.toMutableMap()
    val newNames = m_.toMutableMap();
    val scope = scope(entry)

    for (l in scope.continuations) {
        if (l == entry)
            continue
        newNames[l] = Expression.Abstraction(l.name + "_" + (labels.size + newNames.size))
    }

    for (l in scope.continuations) {
        if (l == entry)
            continue

        val l_newname = (newNames[l] as Expression.Abstraction).fnName
        newLabels[l_newname] = Continuation(l_newname, l.signature, mangle(l.body, newNames))
    }

    val entry_newname = entry.name + "_" + (labels.size + newLabels.size)
    newLabels[entry_newname] = Continuation(entry_newname, t, mangle(entry.body, newNames))

    return Program(newLabels)
}