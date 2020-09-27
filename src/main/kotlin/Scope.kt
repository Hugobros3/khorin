fun Program.visit(visitor: (IRNode)->Unit ) {
    val visited = mutableSetOf<IRNode>()
    for(f in labels.values) {
        visit(f.second, visited, visitor)
    }
}

fun Program.visit(node: IRNode, visited: MutableSet<IRNode> = mutableSetOf(), visitor: (IRNode)->Unit) {
    if(visited.contains(node))
        return

    visited += node
    visitor(node)
    when (node) {
        is IRNode.Body -> {
            node.arguments.forEach { visit(it, visited, visitor) }
        }
        is IRNode.Expression.PrimOp -> {
            node.operands.forEach { visit(it, visited, visitor) }
        }
        is IRNode.Expression.Abstraction -> {
            visit(labels[node.fnName]!!.second.callee, visited, visitor)
        }
        is IRNode.Expression.Parameter -> {
            visit(labels[node.fnName]!!.second.arguments[node.i], visited, visitor)
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
        visit(f.second) {
            if(it == node) {
                list += node
            }
        }
    }
    return list
}

fun Program.isLive(fn: Function, from: Function): Boolean {
    if(fn == from)
        return false
    val fbody = fn.second
    for(i in 0 .. from.second.arguments.size) {
        val li = from.second.arguments[i]
        if(isInSubtree(li, fbody))
            return true
    }
    return false
}

fun Program.scope(function: Function) {
    val body = function.second
    val scope = mutableSetOf<Function>()
    for(f in labels.values) {
        TODO()
    }
}