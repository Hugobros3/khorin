fun Program.visit(visitor: (IRNode)->Unit ) {
    val visited = mutableSetOf<IRNode>()
    for(f in labels.values) {
        visit(f, visited, visitor)
    }
}

fun Program.visit(node: IRNode, visited: MutableSet<IRNode> = mutableSetOf(), visitor: (IRNode)->Unit) {
    if(visited.contains(node))
        return

    visited += node
    visitor(node)
    when (node) {
        is IRNode.Continuation -> {
            visit(node.body, visited, visitor)
        }
        is IRNode.Body -> {
            node.arguments.forEach { visit(it, visited, visitor) }
        }
        is IRNode.Expression.PrimOp -> {
            node.operands.forEach { visit(it, visited, visitor) }
        }
        is IRNode.Expression.Abstraction -> {
            visit(labels[node.fnName]!!.body, visited, visitor)
        }
        is IRNode.Expression.Parameter -> {
            visit(labels[node.fnName]!!.body.arguments[node.i], visited, visitor)
        }
        is IRNode.Expression.QuoteLiteral -> {}
        is IRNode.Expression.Cast -> visit(node.source, visited, visitor)
    }
}

fun Program.isInSubtree(what: IRNode, where: IRNode): Boolean {
    var found = false
    visit(where) {
        if(it == what) {
            found = true
        }
    }
    return found
}

fun Program.findUses(node: IRNode) : List<IRNode> {
    val list = mutableListOf<IRNode>()
    for(f in labels.values) {
        visit(f) {
            if(it == node) {
                list += node
            }
        }
    }
    return list
}

fun Program.isLive(fn: IRNode.Continuation, from: IRNode.Continuation): Boolean {
    if(fn == from)
        return false
    val fbody = fn.body
    for(i in 0 .. from.body.arguments.size) {
        val li = from.body.arguments[i]
        if(isInSubtree(li, fbody))
            return true
    }
    return false
}

fun Program.scope(continuation: IRNode.Continuation) {
    val body = continuation.body
    val scope = mutableSetOf<IRNode.Continuation>()
    for(f in labels.values) {
        TODO()
    }
}