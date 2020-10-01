package xform

import IRNode
import IRNode.*
import Program
import scope

typealias M = Map<IRNode, IRNode>

fun Program.substitute(m: M): Program = Program(labels.map { (k,v) -> Pair(k, substitute(v, m))}.toMap())

fun Program.substitute(c: Continuation, m: M): Continuation = m[c] as? Continuation ?: Continuation(c.name, c.attributes, c.signature, substitute(c.body, m))

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
    //val newLabels = mutableMapOf<String, Continuation>()
    val newLabels = labels.toMutableMap()
    val newNames = m.toMutableMap();
    val scope = scope(entry)

    for (l in scope.continuations) {
        if (l == entry)
            continue

        /*val l_newname = l.name + "_" + (labels.size + newNames.size)
        newNames[l] = Expression.Abstraction(l_newname)

        for((i, p) in l.parameters.withIndex()) {
            println(p == p)
            newNames[p] = Expression.Parameter(l_newname, i)
        }*/
    }

    //for((i, p) in entry.parameters.withIndex())
    //    newNames[p] = Expression.Parameter(newName, i)

    println(scope.continuations.map { it.name })

    for (l in scope.continuations) {
        if (l == entry)
            continue

        //val l_newname = (newNames[l] as Expression.Abstraction).fnName
        newLabels[l.name] = Continuation(l.name, l.attributes, l.signature, substitute(l.body, newNames))
    }

    // Kill the old version of the continuation
    // This will make the IR invalid as the previous scope of this is still alive
    // A cleanup phase is therefore needed.
    // newLabels.remove(entry.name)

    newLabels[newName] = Continuation(newName, entry.attributes, t, substitute(entry.body, newNames))

    println(newLabels)

    return Program(newLabels)
}