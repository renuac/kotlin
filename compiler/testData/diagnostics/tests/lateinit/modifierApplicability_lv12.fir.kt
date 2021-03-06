// !LANGUAGE: +LateinitTopLevelProperties +LateinitLocalVariables
import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

public abstract class A<T: Any, V: String?>(lateinit var p2: String) {

    public lateinit val a: String
    lateinit val b: T
    private lateinit var c: CharSequence

    lateinit val d: String
        get

    public lateinit var e: String
        get
        private set

    fun a() {
        lateinit var a: String
    }

    lateinit var e1: V
    lateinit var e2: String?
    lateinit var e3: Int
    lateinit var e4: Int?
    lateinit var e5 = "A"

    // With initializer, primitive
    lateinit var e6 = 3

    lateinit var e7 by CustomDelegate()

    lateinit var e8: String
        get() = "A"

    lateinit var e9: String
        set(v) { field = v }

    abstract lateinit var e10: String

    lateinit var String.e11: String

    lateinit var String.e12: String
}

lateinit val topLevel: String
lateinit var topLevelMutable: String

public interface Intf {
    lateinit var str: String
}

public abstract class AbstractClass {
    abstract var str: String
}

public class AbstractClassImpl : AbstractClass() {
    override lateinit var str: String
}

public class B {
    lateinit var a: String

    init {
        a.length
    }
}
