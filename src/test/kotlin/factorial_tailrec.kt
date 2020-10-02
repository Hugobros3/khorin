import org.junit.Test
import util.*
import xform.mangle
import xform.simplify
import xform.update_callsites

class TestFig9FactorialTailrec : TestProgram() {
    override val p = program {
        var n: IRNode.Expression = bot(int)
        var f_ret: IRNode.Expression = bot(fn_type(int))

        function("fac", fn_type(int, fn_type(int))) {
            markExternal()
            n = param(0)
            f_ret = param(1)

            branch(infeq(n, lit(1)), fn("then"), fn("else"))
        }
        function("then", fn_type()) {
            call(f_ret, lit(1))
        }
        function("else", fn_type()) {
            call(fn("help"), lit(1), lit(2), n, f_ret)
        }

        var i: IRNode.Expression = bot(int)
        var r: IRNode.Expression = bot(int)
        var h_n: IRNode.Expression = bot(int)
        var h_ret: IRNode.Expression = bot(fn_type(int))
        function("help", fn_type(int, int, int, fn_type(int))) {
            i = param(0)
            r = param(1)
            h_n = param(2)
            h_ret = param(3)

            branch(infeq(i, h_n), fn("then2"), fn("else2"))
        }
        function("then2", fn_type()) {
            call(fn("help"), add(i, lit(1)), mul(i, r), h_n, h_ret)
        }
        function("else2", fn_type()) {
            call(h_ret, r)
        }
    }

    @Test
    fun mangleSimpleRecursion() {
        val fac = p.labels["fac"] as IRNode.Continuation
        val n = fac.parameters[0]
        val f_ret = fac.parameters[1]

        val help = p.labels["help"] as IRNode.Continuation
        val i = help.parameters[0]
        val r = help.parameters[1]
        val h_n = help.parameters[2]
        val h_ret = help.parameters[3]

        val mangled = p.mangle(help, "help_d", fn_type(int, int), mapOf(
            i to IRNode.Expression.Parameter("help_d", 0),
            r to IRNode.Expression.Parameter("help_d", 1),
            h_n to n,
            h_ret to f_ret
        ))

        val updated = mangled.update_callsites(help, mangled.labels["help_d"]!!, listOf(0, 1)).simplify()

        dumpIR(updated)
        dumpCalls(updated)
    }
}