package analyses

import Program

data class CallGraph(val nodes: Set<Node>) {
    data class Node(val c: IRNode.Continuation, val callees: Set<Node>) {
        override fun hashCode() = c.hashCode()
        override fun equals(other: Any?) = (other as? Node)?.c?.equals(c) ?: false
    }
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

    val nodeEdges = nodes.keys.associateWith { mutableSetOf<CallGraph.Node>() }
    val nodesSet = nodes.keys.map { CallGraph.Node(it, nodeEdges[it]!!) }.toSet()
    val mapCont2Node = nodesSet.map { Pair(it.c, it) }.toMap()

    for(n2 in nodesSet) {
        for(edge in nodes[n2.c]!!) {
            nodeEdges[n2.c]!! += mapCont2Node[edge]!!
        }
    }

    return CallGraph(nodesSet)
}