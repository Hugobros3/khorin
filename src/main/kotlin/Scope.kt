fun Program.visitGraph(visitor: (IRNode) -> Unit) {
    val visited = mutableSetOf<IRNode>()
    for (f in labels.values) {
        visitSubgraph(f, visited, visitor)
    }
}

fun Program.visitSubgraph(node: IRNode, visited: MutableSet<IRNode> = mutableSetOf(), visitor: (IRNode) -> Unit) {
    if (visited.contains(node))
        return

    visited += node
    visitor(node)
    when (node) {
        is IRNode.Continuation -> {
            visitSubgraph(node.body, visited, visitor)
        }
        is IRNode.Body -> {
            node.arguments.forEach { visitSubgraph(it, visited, visitor) }
        }
        is IRNode.Expression.PrimOp -> {
            node.operands.forEach { visitSubgraph(it, visited, visitor) }
        }
        is IRNode.Expression.Abstraction -> {
            visitSubgraph(labels[node.fnName]!!.body, visited, visitor)
        }
        is IRNode.Expression.Parameter -> {
            visitSubgraph(labels[node.fnName]!!.body.arguments[node.i], visited, visitor)
        }
        is IRNode.Expression.QuoteLiteral -> {
        }
        is IRNode.Expression.Cast -> visitSubgraph(node.source, visited, visitor)
    }
}

fun Program.isInSubtree(what: IRNode, where: IRNode): Boolean {
    var found = false
    visitSubgraph(where) {
        if (it == what) {
            found = true
        }
    }
    return found
}

fun Program.findUses(node: IRNode, uses: MutableSet<IRNode> = mutableSetOf<IRNode>()): Set<IRNode> {
    visitGraph { user ->
        visitSubgraph(user) { used ->
            if (used == node && !uses.contains(user)) {
                uses += user
            }
        }
    }
    return uses
}

fun Program.isLive(node: IRNode.Continuation, scopeEntry: IRNode.Continuation): Boolean {
    if (node == scopeEntry)
        return false
    val fbody = node.body
    for (i in 0..scopeEntry.body.arguments.size) {
        val li = scopeEntry.body.arguments[i]
        if (isInSubtree(li, fbody))
            return true
    }
    return false
}

data class Scope(val nodes: Set<IRNode>)

fun Program.scope(scopeEntry: IRNode.Continuation): Scope {
    val nodes = mutableSetOf<IRNode>()
    val queue = mutableListOf<IRNode>()

    fun enqueue(node: IRNode) {
        if (!nodes.contains(node)) {
            nodes.add(node)

            queue.add(node)
            if (node is IRNode.Continuation) {
                for (param in node.parameters) {
                    if (!nodes.add(param))
                        throw Exception("Assertion broken: callee param added twice")
                    queue.add(param)
                }
            }
        }
    }

    enqueue(scopeEntry)

    while (queue.isNotEmpty()) {
        val node = queue.removeAt(0)
        if (node == scopeEntry)
            continue
        for (use in findUses(node)) {
            enqueue(use)
        }
    }

    return Scope(nodes)
}