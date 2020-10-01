package xform

import IRNode
import IRNode.Body.Intrinsic.IntrinsicOp.BRANCH
import Program
import Value
import boolValue
import evaluate
import intValue
import util.lit

val IRNode.Expression.isKnown: Boolean
    get() = this is IRNode.Expression.QuoteLiteral && this.lit !is Value.Literal.Bottom

val IRNode.Expression.boolValue: Boolean?
    get() = if (this is IRNode.Expression.QuoteLiteral && this.lit !is Value.Literal.Bottom && lit.type == Type.PrimitiveType.Bool) boolValue(lit) else null

val IRNode.Expression.intValue: Int?
    get() = if (this is IRNode.Expression.QuoteLiteral && this.lit !is Value.Literal.Bottom && lit.type == Type.PrimitiveType.Int) intValue(lit) else null

val IRNode.Expression.value: Value
    get() = (this as IRNode.Expression.QuoteLiteral).lit

/** Re-creates the program, performing constant folding as it goes */
fun Program.simplify() : Program {
    val i = Importer(this)
    i.import()

    return Program(i.newLabels)
}

class Importer(private val program: Program) {
    val newLabels = mutableMapOf<String, IRNode.Continuation>()

    val queue = mutableListOf<IRNode.Continuation>()
    val translated = mutableMapOf<IRNode, IRNode>()

    fun import(old: IRNode.Continuation): IRNode.Continuation {
        if (translated.containsKey(old))
            return translated[old] as IRNode.Continuation

        val new = IRNode.Continuation(old.name, old.attributes, old.signature, import(old.body))
        translated[old] = new
        return new
    }

    fun import(old: IRNode.Body): IRNode.Body {
        if (translated.containsKey(old))
            return translated[old] as IRNode.Body

        val new = when (old) {
            is IRNode.Body.Call -> {
                IRNode.Body.Call(import(old.callee), old.arguments.map { import(it) })
            }
            is IRNode.Body.Intrinsic -> {
                when (old.intrinsicOp) {
                    BRANCH -> {
                        val condition = import(old.arguments[0])
                        if (condition.isKnown) {
                            if (condition.boolValue!!)
                                IRNode.Body.Call(import(old.arguments[1]), emptyList())
                            else
                                IRNode.Body.Call(import(old.arguments[2]), emptyList())
                        } else {
                            IRNode.Body.Intrinsic(BRANCH, old.arguments.map { import(it) })
                        }
                    }
                }
            }
        }

        translated[old] = new
        return new
    }

    fun import(old: IRNode.Expression): IRNode.Expression {
        if (translated.containsKey(old))
            return translated[old] as IRNode.Expression

        val new: IRNode.Expression = when (old) {
            is IRNode.Expression.PrimOp -> simplify(old)
            is IRNode.Expression.Cast -> {
                val src = import(old.source)
                if (src.isKnown) {
                    if (old.dstType == Type.PrimitiveType.Int)
                        IRNode.Expression.QuoteLiteral(Value.Literal.IntValue(if (src.boolValue!!) 1 else 0))
                    else if (old.dstType == Type.PrimitiveType.Bool)
                        IRNode.Expression.QuoteLiteral(Value.Literal.BoolValue(src.intValue!! != 0))
                    else
                        throw Exception("Invalid cast")
                } else
                    src
            }
            // These don't change
            is IRNode.Expression.Abstraction -> {
                queue.add(program.labels[old.fnName]!!)
                old
            }
            is IRNode.Expression.Parameter -> {
                queue.add(program.labels[old.fnName]!!)
                old
            }
            is IRNode.Expression.QuoteLiteral -> old
        }
        translated[old] = new
        return new
    }

    /** Applies a bunch of constant folding rules... */
    fun simplify(old: IRNode.Expression.PrimOp): IRNode.Expression {
        val operands = old.operands.map { import(it) }

        if(operands.all { it.isKnown })
            return IRNode.Expression.QuoteLiteral(evaluate(old.op, operands.map { it.value }))

        when (old.op) {
            IRNode.Expression.PrimOp.PrimOps.MUL -> {
                // either operand being zero zeroes out the whole thing
                if(operands.any { it.isKnown && it.intValue == 0 })
                    return lit(0)
                // multiply by one does nothing
                if(operands[1].intValue == 1)
                    return operands[0]
                if(operands[0].intValue == 1)
                    return operands[1]
            }
            IRNode.Expression.PrimOp.PrimOps.DIV,
            IRNode.Expression.PrimOp.PrimOps.MOD -> {
                if(operands[1].intValue == 1)
                    return operands[0]
            }
            IRNode.Expression.PrimOp.PrimOps.AND -> {
                if(operands[0].boolValue == false || operands[1].boolValue == false)
                    return lit(false)
            }
            IRNode.Expression.PrimOp.PrimOps.OR -> {
                if(operands[0].boolValue == true || operands[1].boolValue == true)
                    return lit(true)
            }
            IRNode.Expression.PrimOp.PrimOps.NOT -> {
            }
            IRNode.Expression.PrimOp.PrimOps.INF -> {
            }
            IRNode.Expression.PrimOp.PrimOps.INFEQ -> {
            }
            IRNode.Expression.PrimOp.PrimOps.EQ -> {
            }
        }

        return IRNode.Expression.PrimOp(old.op, operands)
    }

    fun import() {
        for ((oldName, oldContinuation) in program.labels) {
            if (!oldContinuation.attributes.isExternal)
                continue // let non-externals die
            queue.add(oldContinuation)
        }

        while(queue.isNotEmpty()) {
            val oldContinuation = queue.removeAt(0)
            newLabels[oldContinuation.name] = import(oldContinuation)
        }
    }
}