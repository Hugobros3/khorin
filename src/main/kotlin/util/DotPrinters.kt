package util

import IRNode
import Program
import analyses.CallGraph
import java.io.Writer

// All those helper functions live outside because multiple printers might need them

private fun IRNode.unique_name() = "${this.hashCode()}"

private fun IRNode.label(): String = when(this) {
    is IRNode.Continuation -> "$name : $signature"
    is IRNode.Expression.Abstraction -> "&${this.fnName}"
    is IRNode.Expression.PrimOp -> this.op.symbol
    is IRNode.Body.Intrinsic -> this.intrinsicOp.toString()
    is IRNode.Expression.Parameter -> "param${this.i}"
    is IRNode.Body.Call -> "Call()"
    is IRNode.Expression.QuoteLiteral -> "${this.lit} : ${this.lit.type}"
    is IRNode.Expression.Cast -> "cast(${this.dstType})"
    else -> unique_name() + " : " + this.javaClass.simpleName
}

private fun IRNode.appearance(): DotPrinter.NodeAppearance = when(this) {
    is IRNode.Continuation -> {
        if(attributes.isExternal)
            DotPrinter.NodeAppearance("rectangle", color = "pink", style = "filled")
        else
            DotPrinter.NodeAppearance("rectangle")
    }
    is IRNode.Expression.Parameter -> DotPrinter.NodeAppearance(color = "lightgrey")

    is IRNode.Body.Intrinsic -> DotPrinter.NodeAppearance("rectangle", color = "lightblue", style = "filled")
    is IRNode.Body.Call -> DotPrinter.NodeAppearance("rectangle", color = "orange", style = "filled")
    is IRNode.Expression.Abstraction -> DotPrinter.NodeAppearance(color = "orange")

    is IRNode.Expression.Cast,
    is IRNode.Expression.PrimOp -> DotPrinter.NodeAppearance(color = "darkseagreen1", style = "filled")

    is IRNode.Expression.QuoteLiteral -> DotPrinter.NodeAppearance(style = "dotted")

    else -> DotPrinter.NodeAppearance()
}

val DataDependency = DotPrinter.ArrowStyle(arrowHead = "vee")
val ControlFlow = DotPrinter.ArrowStyle(arrowHead = "normal")
val ContinuationBody = DotPrinter.ArrowStyle(arrowHead = "none")
val ParameterOf = DotPrinter.ArrowStyle(arrowHead = "none")
val ArgumentOf = DotPrinter.ArrowStyle(arrowHead = "empty")

class IRDotPrinter(private val program: Program, output: Writer) : DotPrinter(output) {
    private val done = mutableSetOf<IRNode>()

    fun print() {
        output += "digraph Program{"
        indent++
        output += "bgcolor=transparent;"
        for (c in program.labels.values)
            if(c.attributes.isExternal)
                dump(c)
        indent--
        output += "}"
    }

    private fun dump(n: IRNode) {
        if (n in done)
            return

        done.add(n)

        node(n.unique_name(), n.label(), n.appearance())

        when (n) {
            is IRNode.Continuation -> {
                for(p in n.parameters)
                    dump(p)

                dump(n.body)
                arrow(n, n.body, ContinuationBody)
            }
            is IRNode.Body.Call -> {
                dump(n.callee)
                arrow(n, n.callee, ControlFlow)
                for ((i, arg) in n.arguments.withIndex()) {
                    dump(arg)
                    arrow(arg, n, ArgumentOf, "arg$i")
                }
            }
            is IRNode.Body.Intrinsic -> {
                for ((i, arg) in n.arguments.withIndex()) {
                    dump(arg)
                    arrow(arg, n, ArgumentOf, "arg$i")
                }
            }
            is IRNode.Expression.PrimOp -> {
                for ((i, op) in n.operands.withIndex()) {
                    dump(op)
                    arrow(n, op, DataDependency, "op$i")
                }
            }
            is IRNode.Expression.Abstraction -> {
                val resolved = program.labels[n.fnName] as IRNode.Continuation
                dump(resolved)
                arrow(n, resolved, DataDependency)
            }
            is IRNode.Expression.Parameter -> {
                val resolved = program.labels[n.fnName] as IRNode.Continuation
                dump(resolved)
                arrow(n, resolved, ParameterOf)
            }
            is IRNode.Expression.QuoteLiteral -> {
            }
            is IRNode.Expression.Cast -> {
                dump(n.source)
                arrow(n, n.source, DataDependency)
            }
        }
    }

    private fun arrow(src: IRNode, dst: IRNode, style: ArrowStyle, name: String? = null) {
        arrow(src.unique_name(), dst.unique_name(), style, name)
    }
}

class CallGraphPrinter(private val graph: CallGraph, output: Writer) : DotPrinter(output) {
    fun print() {
        output += "digraph CallGraph {"
        indent++
        output += "bgcolor=transparent;"
        for(n in graph.nodes) {
            val (continuation, edges) = n
            node(n.c.unique_name(), n.c.label(), n.c.appearance())
            for(edge in edges) {
                arrow(n.c, edge.c, ControlFlow)
            }
        }
        indent--
        output += "}"
    }

    private fun arrow(src: IRNode, dst: IRNode, style: ArrowStyle, name: String? = null) {
        arrow(src.unique_name(), dst.unique_name(), style, name)
    }
}

abstract class DotPrinter(protected val output: Writer) {
    protected var indent = 0;

    protected operator fun Writer.plusAssign(s: String) {
        for (i in 0 until indent)
            output.write("    ")
        output.write(s)
        output.write("\n")
    }

    data class NodeAppearance(val shape: String = "ellipse", val color: String = "black", val style: String = "solid")

    data class ArrowStyle(val arrowHead: String, val fontSize: Int = 8, val fontColor: String = "grey")

    protected fun node(internalName: String, label: String, appearance: NodeAppearance) {
        output += "$internalName [ "
        indent++

        output += "label = \"$label\";"

        output += "shape = ${appearance.shape};"
        output += "color = ${appearance.color};"
        output += "style = ${appearance.style};"

        indent--
        output += "]"
    }

    protected fun arrow(src: String, dst: String, style: ArrowStyle, name: String? = null) {
        var arrowParams = "arrowhead=" + style.arrowHead
        if(name != null)
            arrowParams += ",label=\"$name\""
        arrowParams += ",fontsize=${style.fontSize}"
        arrowParams += ",fontcolor=${style.fontColor}"
        output += "$src -> $dst[$arrowParams];"
    }
}