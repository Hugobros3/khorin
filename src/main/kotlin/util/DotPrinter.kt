package util

import IRNode
import Program
import java.io.Writer

class DotPrinter(val program: Program, val output: Writer) {
    private var indent = 0;

    private val done = mutableSetOf<IRNode>()

    private operator fun Writer.plusAssign(s: String) {
        for (i in 0..indent)
            output.write("    ")
        output.write(s)
        output.write("\n")
    }

    fun print() {
        output += "digraph Program{"
        indent++
        output += "bgcolor=transparent;"
        for (c in program.labels.values)
            dump(c)
        indent--
        output += "}"
    }

    private fun IRNode.unique_name() = "${this.hashCode()}"

    data class NodeAppearance(val shape: String = "ellipse", val color: String = "black", val style: String = "solid")

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

    private fun IRNode.appearance(): NodeAppearance = when(this) {
        is IRNode.Continuation -> NodeAppearance("rectangle")
        is IRNode.Expression.Parameter -> NodeAppearance(color = "lightgrey")

        is IRNode.Body.Intrinsic -> NodeAppearance("rectangle", color = "lightblue", style="filled")
        is IRNode.Body.Call -> NodeAppearance("rectangle", color = "orange", style="filled")
        is IRNode.Expression.Abstraction -> NodeAppearance(color = "orange")

        is IRNode.Expression.Cast,
        is IRNode.Expression.PrimOp -> NodeAppearance(color = "darkseagreen1", style = "filled")

        is IRNode.Expression.QuoteLiteral -> NodeAppearance(style = "dotted")

        else -> NodeAppearance()
    }

    enum class ArrowType(val arrowHead: String, val fontSize: Int = 8, val fontColor: String = "grey") {
        DataDependency(arrowHead = "vee"),
        ControlFlow(arrowHead = "normal"),
        ContinuationBody(arrowHead = "none"),
        ParameterOf(arrowHead = "none"),
        ArgumentOf(arrowHead = "empty")
    }

    private fun arrow(src: IRNode, dst: IRNode, type: ArrowType, name: String? = null) {
        var arrowParams = "arrowhead=" + type.arrowHead
        if(name != null)
            arrowParams += ",label=\"$name\""
        arrowParams += ",fontsize=${type.fontSize}"
        arrowParams += ",fontcolor=${type.fontColor}"
        output += src.unique_name() + " -> " + dst.unique_name() + "[$arrowParams];"
    }

    private fun dump(n: IRNode) {
        if (n in done)
            return

        done.add(n)

        output += n.unique_name() + " [ "
        indent++

        output += "label = \"${n.label()}\";"

        val a = n.appearance()
        output += "shape = ${a.shape};"
        output += "color = ${a.color};"
        output += "style = ${a.style};"

        indent--
        output += "]"

        when (n) {
            is IRNode.Continuation -> {
                dump(n.body)
                arrow(n, n.body, ArrowType.ContinuationBody)
            }
            is IRNode.Body.Call -> {
                dump(n.callee)
                arrow(n, n.callee, ArrowType.ControlFlow)
                for ((i, arg) in n.arguments.withIndex()) {
                    dump(arg)
                    arrow(arg, n, ArrowType.ArgumentOf, "arg$i")
                }
            }
            is IRNode.Body.Intrinsic -> {
                for ((i, arg) in n.arguments.withIndex()) {
                    dump(arg)
                    arrow(arg, n, ArrowType.ArgumentOf, "arg$i")
                }
            }
            is IRNode.Expression.PrimOp -> {
                for ((i, op) in n.operands.withIndex()) {
                    dump(op)
                    arrow(n, op, ArrowType.DataDependency, "op$i")
                }
            }
            is IRNode.Expression.Abstraction -> {
                val resolved = program.labels[n.fnName] as IRNode.Continuation
                dump(resolved)
                arrow(n, resolved, ArrowType.DataDependency)
            }
            is IRNode.Expression.Parameter -> {
                val resolved = program.labels[n.fnName] as IRNode.Continuation
                dump(resolved)
                arrow(n, resolved, ArrowType.ParameterOf)
            }
            is IRNode.Expression.QuoteLiteral -> {
            }
            is IRNode.Expression.Cast -> {
                dump(n.source)
                arrow(n, n.source, ArrowType.DataDependency)
            }
        }
    }
}