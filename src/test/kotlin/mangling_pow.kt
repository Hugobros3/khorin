import analyses.callGraph
import org.junit.Test
import util.*
import xform.mangle
import xform.simplify
import xform.substitute
import java.io.File
import java.io.FileWriter

/** Figure 6(a) */
class TestFig6Mangling {
    val p = program {
        var x: IRNode.Expression = bot(fn_type())
        var y: IRNode.Expression = bot(fn_type())
        var ret: IRNode.Expression = bot(fn_type())
        var a: IRNode.Expression = bot(fn_type())
        var b: IRNode.Expression = bot(fn_type())
        var i: IRNode.Expression = bot(fn_type())
        var r: IRNode.Expression = bot(fn_type())

        function("f", fn_type(int, int, fn_type(int))) {
            markExternal()

            x = param(0)
            y = param(1)
            ret = param(2)

            branch(bot(bool), fn("calcx"), fn("calcy"))
        }

        function("pow", fn_type(int, int)) {
            a = param(0)
            b = param(1)

            branch(eq(param(1), lit(0)), fn("then"), fn("else"))
        }

        function("then", fn_type()) {
            call(ret, lit(1))
        }

        function("else", fn_type()) {
            call(fn("head"), lit(0), a)
        }

        function("head", fn_type(int, int)) {
            i = param(0)
            r = param(1)

            branch(inf(i, b), fn("body"), fn("next"))
        }

        function("body", fn_type()) {
            call(fn("head"), add(i, lit(1)), mul(r, a))
        }

        function("next", fn_type()) {
            call(ret, r)
        }

        function("calcx", fn_type()) {
            call(fn("pow"), x, lit(3))
        }

        function("calcy", fn_type()) {
            call(fn("pow"), y, lit(3))
        }
    }

    @Test
    fun testIRDotPrinter() {
        //val w = System.out.bufferedWriter()
        val f = File("ir.dot")
        val w = FileWriter(f)
        IRDotPrinter(p, w).print()
        w.flush()

        println(f.absoluteFile.path)
    }

    @Test
    fun testGraphDotPrinter() {
        //val w = System.out.bufferedWriter()
        val f = File("calls.dot")
        val w = FileWriter(f)
        CallGraphPrinter(p.callGraph(), w).print()
        w.flush()

        println(f.absoluteFile.path)
    }

    @Test
    fun dropTest() {
        val oldpow = p.labels["pow"] as IRNode.Continuation

        val callsiteUpdates: Map<IRNode, IRNode> = p.uses[oldpow]!!.flatMap { use ->
            val use = use as IRNode.Expression.Abstraction
            p.uses[use]!!.map {
                val call = it as IRNode.Body.Call
                val newcall = IRNode.Body.Call(IRNode.Expression.Abstraction("pow_d"), listOf(call.arguments[0]))
                Pair(call, newcall)
            }
        }.toMap()

        val a = oldpow.parameters[0]
        val b = oldpow.parameters[1]
        val mangled = p.mangle(oldpow, "pow_d", fn_type(int),
            mutableMapOf(
                a to IRNode.Expression.Parameter("pow_d", 0),
                b to IRNode.Expression.QuoteLiteral(Value.Literal.IntValue(3))
            )
        )

        val fixedCallsites = mangled.substitute(callsiteUpdates)

        val simplified = fixedCallsites.simplify()

        val f = File("ir.dot")
        val w = FileWriter(f)
        IRDotPrinter(simplified, w).print()
        w.flush()
    }
}