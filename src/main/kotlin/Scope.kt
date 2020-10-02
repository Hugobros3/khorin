fun Program.visitGraph(visitor: (IRNode) -> Unit) {
    val visited = mutableSetOf<IRNode>()
    for (f in labels.values) {
        visitSubgraph(f, visited, visitor)
    }
}

fun Program.visitSubgraph(node: IRNode, visited: MutableSet<IRNode> = mutableSetOf(), visitor: (IRNode) -> Unit) {
    if (node in visited)
        return

    visited += node
    visitor(node)
    when (node) {
        is IRNode.Continuation -> {
            visitSubgraph(node.body, visited, visitor)
            node.parameters.forEach { visitSubgraph(it, visited, visitor) }
        }
        is IRNode.Body -> {
            if (node is IRNode.Body.Call)
                visitSubgraph(node.callee, visited, visitor)
            node.arguments.forEach { visitSubgraph(it, visited, visitor) }
        }
        is IRNode.Expression.PrimOp -> {
            node.operands.forEach { visitSubgraph(it, visited, visitor) }
        }
        is IRNode.Expression.Abstraction -> {
            visitSubgraph(labels[node.fnName]!!, visited, visitor)
        }
        is IRNode.Expression.Parameter -> {
            visitSubgraph(labels[node.fnName]!!.parameters[node.i], visited, visitor)
        }
        is IRNode.Expression.QuoteLiteral -> { }
        is IRNode.Expression.Cast -> visitSubgraph(node.source, visited, visitor)
    }
}

fun Program.uses() : Map<IRNode, Set<IRNode>> {
    val uses = mutableMapOf<IRNode, MutableSet<IRNode>>()

    fun addDep(node: IRNode, of: IRNode) {
        uses[of]!!.add(node)
    }

    fun visitExpression(expression: IRNode.Expression) {
        when(expression) {
            is IRNode.Expression.PrimOp -> expression.operands.forEach { addDep(expression, it) }
            is IRNode.Expression.Abstraction -> addDep(expression, labels[expression.fnName]!!)
            is IRNode.Expression.Parameter -> addDep(expression, labels[expression.fnName]!!.parameters[expression.i])
            is IRNode.Expression.QuoteLiteral -> { }
            is IRNode.Expression.Cast -> addDep(expression, expression.source)
        }
    }

    fun visitNode(node: IRNode) {
        when(node) {
            is IRNode.Continuation -> addDep(node, node.body)
            is IRNode.Body.Call -> {
                addDep(node, node.callee)
                node.arguments.forEach { addDep(node, it) }
            }
            is IRNode.Body.Intrinsic -> node.arguments.forEach { addDep(node, it) }
            is IRNode.Expression -> visitExpression(node)
        }
    }

    visitGraph { uses[it] = mutableSetOf() }
    visitGraph { visitNode(it) }

    return uses
}

data class Scope(val nodes: Set<IRNode>) {
    val continuations = nodes.filterIsInstance<IRNode.Continuation>()
}

fun Program.scope(scopeEntry: IRNode.Continuation): Scope {
    val nodes = mutableSetOf<IRNode>()
    val queue = mutableListOf<IRNode>()

    fun enqueue(node: IRNode) {
        if (node !in nodes) {
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
        for (use in uses[node]!!) {
            enqueue(use)
        }
    }

    // The entry continuation does not belong in it's
    nodes.remove(scopeEntry)

    return Scope(nodes)
}