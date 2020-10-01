import analyses.callGraph
import org.junit.Test
import util.*
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
}