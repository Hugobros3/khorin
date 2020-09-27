import org.junit.Test

fun factorialProgram() = program {
    var n: IRNode.Expression = lit(-1) // NB: we don't care about this literal, we just need to init this to something to set it later in "fac"
    var ret = n // same deal

    function("fac", fn_type(int, fn_type(int))) {
        val condition = infeq(param(0), lit(0)) // n <= 0
        n = param(0) // save a reference to fac's "n" parameter
        ret = param(1) // save a reference to fac's "ret" parameter
        branch(condition, fn("then"), fn("else"))
    }

    function("then", fn_type()) {
        call(ret, lit(1))
    }

    function("else", fn_type()) {
        call(fn("head"), lit(2), lit(1))
    }

    var i: IRNode.Expression = lit(-1)
    var r: IRNode.Expression = lit(-1)
    function("head", fn_type(int, int)) {
        val condition = infeq(param(0), n)
        i = param(0)
        r = param(1)
        branch(condition, fn("body"), fn("next"))
    }

    function("body", fn_type()) {
        call(fn("head"), add(i, lit(1)), mul(i, r))
    }

    function("next", fn_type()) {
        call(ret, r)
    }
}

class TestFactorial {
    @Test
    fun testTyping() {
        val p = factorialProgram()
        type(p)
    }

    @Test
    fun testPrinting() {
        val p = factorialProgram()

        println(p)
    }

    @Test
    fun testScopes() {
        val p = factorialProgram()

        println("Uses: "+p.findUses(p.labels["fac"]!!))
    }
}
