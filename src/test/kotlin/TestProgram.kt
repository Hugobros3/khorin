import analyses.callGraph
import org.junit.Test
import util.CallGraphPrinter
import util.IRDotPrinter
import java.io.File
import java.io.FileWriter

fun dumpIR(p: Program) {
    val f = File("ir.dot")
    val w = FileWriter(f)
    IRDotPrinter(p, w).print()
    w.flush()
}

fun dumpCalls(p: Program) {
    val f = File("calls.dot")
    val w = FileWriter(f)
    CallGraphPrinter(p.callGraph(), w).print()
    w.flush()
}

abstract class TestProgram {
    abstract val p: Program

    @Test
    fun testTyping() {
        type(p)
    }

    @Test
    fun testPrinting() {
        println(p)
    }

    @Test
    fun testIRDotPrinter() {
        dumpIR(p)
    }

    @Test
    fun testGraphDotPrinter() {
        dumpCalls(p)
    }
}