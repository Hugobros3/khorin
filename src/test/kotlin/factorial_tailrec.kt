import util.*

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
}