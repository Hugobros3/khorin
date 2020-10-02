package analyses

import Program
import Type
import scope

data class CallGraph(val program: Program, val nodes: Set<Node>) {
    data class Node(val c: IRNode.Continuation, val calls: Set<Call>) {
        override fun hashCode() = c.hashCode()
        override fun equals(other: Any?) = (other as? Node)?.c?.equals(c) ?: false
    }

    data class Call(val callee: Node, val order: Int, val isSimple: Boolean)
}

fun Type.order(): Int = when(this) {
    is Type.PrimitiveType -> 0
    is Type.FnType -> 1 + this.parametersTypes.map { it.order() }.maxOrNull()!!
}

fun IRNode.Continuation.order() = this.signature.parametersTypes.map { it.order() }.maxOrNull() ?: 0

fun make_call(p: Program, from: CallGraph.Node, to: CallGraph.Node): CallGraph.Call {
    val scope = p.scope(to.c)
    return CallGraph.Call(to, to.c.order(), scope.continuations.contains(from.c))
}

fun Program.callGraph(): CallGraph {
    val nodes = mutableMapOf<IRNode.Continuation, MutableSet<IRNode.Continuation>>()

    fun node(cont: IRNode.Continuation) = nodes.getOrPut(cont) { mutableSetOf() }

    for (cont in labels.values) {
        val callees = node(cont)

        fun findOccuringFns(e: IRNode.Expression) {
            when(e) {
                is IRNode.Expression.PrimOp -> { e.operands.forEach { findOccuringFns(it) }}
                is IRNode.Expression.Abstraction -> callees.add(labels[e.fnName]!!)
                is IRNode.Expression.Parameter -> {}
                is IRNode.Expression.QuoteLiteral -> {}
                is IRNode.Expression.Cast -> findOccuringFns(e)
            }
        }

        when(cont.body) {
            is IRNode.Body.Call -> {
                findOccuringFns(cont.body.callee)
                cont.body.arguments.forEach { findOccuringFns(it) }
            }
            is IRNode.Body.Intrinsic -> cont.body.arguments.forEach { findOccuringFns(it) }
        }
    }

    val nodeEdges = nodes.keys.associateWith { mutableSetOf<CallGraph.Call>() }
    val nodesSet = nodes.keys.map { CallGraph.Node(it, nodeEdges[it]!!) }.toSet()
    val mapCont2Node = nodesSet.map { Pair(it.c, it) }.toMap()

    for(n2 in nodesSet) {
        for(dstNode in nodes[n2.c]!!) {
            nodeEdges[n2.c]!! += make_call(this, n2, mapCont2Node[dstNode]!!)
        }
    }

    return CallGraph(this, nodesSet)
}