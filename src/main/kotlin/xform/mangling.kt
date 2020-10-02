package xform

import IRNode
import IRNode.*
import Program
import scope
import visitGraph

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
    val newLabels = labels.toMutableMap()
    val newNames = m.toMutableMap();
    val scope = scope(entry)

    for (l in scope.continuations) {
        if (l == entry)
            continue

        newLabels[l.name] = Continuation(l.name, l.attributes, l.signature, substitute(l.body, newNames))
    }

    // Kill the old version of the continuation
    // This will make the IR invalid as the previous scope of this is still alive
    // A cleanup phase is therefore needed.

    // Re: in fact do not remove this, we only mangle here, we assume nothing about users of the pre-mangled version
    // newLabels.remove(entry.name)

    newLabels[newName] = Continuation(newName, entry.attributes, t, substitute(entry.body, newNames))

    return Program(newLabels)
}

/** Finds all the calls to `old` and replaces them to calls to `new`, using the arguments of the original call, ordered and selected by `argumentsMapping` */
fun Program.update_callsites(old: Continuation, new: Continuation, argumentsMapping: List<Int>): Program {
    val callsiteSubstitutions = mutableMapOf<IRNode, IRNode>()
    visitGraph {
        body ->
            if (body is Body.Call && body.callee == old.abstraction) {
                callsiteSubstitutions[body] = Body.Call(new.abstraction, argumentsMapping.map { old_i -> body.arguments[old_i] })
            }
    }
    return substitute(callsiteSubstitutions)
}