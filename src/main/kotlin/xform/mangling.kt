package xform

import IRNode
import IRNode.*
import Program
import scope

typealias M = Map<IRNode, IRNode>

fun Program.substitute(m: M): Program = Program(labels.map { (k,v) -> Pair(k, substitute(v, m))}.toMap())

fun Program.substitute(c: Continuation, m: M): Continuation = m[c] as? Continuation ?: Continuation(c.name, c.signature, substitute(c.body, m))

fun Program.substitute(b: Body, m: M): Body = m[b] as? Body ?: when(b) {
    is Body.Call -> Body.Call(substitute(b.callee, m), b.arguments.map { substitute(it, m) })
    is Body.Intrinsic -> Body.Intrinsic(b.intrinsicOp, b.arguments.map { substitute(it, m) })
}

fun Program.substitute(e: Expression, m: M): Expression = m[e] as? Expression ?: when(e) {
    is Expression.PrimOp -> Expression.PrimOp(e.op, e.operands.map { substitute(it, m) })
    is Expression.Abstraction,
    is Expression.Parameter,
    is Expression.QuoteLiteral -> e
    is Expression.Cast -> Expression.Cast(substitute(e.source, m), e.dstType)
}

fun Program.mangle(entry: Continuation, newName: String, t: Type.FnType, m: M): Program {
    val newLabels = labels.toMutableMap()
    val newNames = m.toMutableMap();
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
        newLabels[l_newname] = Continuation(l_newname, l.signature, substitute(l.body, newNames))
    }

    newLabels[newName] = Continuation(newName, t, substitute(entry.body, newNames))

    return Program(newLabels)
}