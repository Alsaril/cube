import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class LimitedStack<T>(private val limit: Int, private val default: T) {
    private val stack = mutableListOf<T>()

    fun push(value: T) {
        stack.add(value)
        if (stack.size > limit) {
            stack.removeAt(0)
        }
    }

    fun pop(): T {
        if (stack.isEmpty()) return default
        return stack.removeAt(stack.size - 1)
    }
}

class LimitedInt(private val limit: Int, initial: Int = 0) {
    var value = initial
        private set

    fun set(value: Int) {
        this.value = value % limit
        if (this.value < 0) {
            this.value += limit
        }
    }

    fun add(value: Int) {
        set(this.value + value)
    }
}

fun <T> doubleChannel(source: ReceiveChannel<T>, dest1: SendChannel<T>, dest2: SendChannel<T>) = launch {
    for (t in source) {
        dest1.send(t)
        dest2.send(t)
    }
    dest1.close()
    dest2.close()
}

fun main(args: Array<String>) {
    val c = Channel<Int>(Channel.UNLIMITED)
    val d1 = Channel<Int>(Channel.UNLIMITED)
    val d2 = Channel<Int>(Channel.UNLIMITED)
    doubleChannel(c, d1, d2)
    val l1 = launch {
        for (t in d1) {
            println("From d1: $t")
        }
        println("d1 closed")
    }

    val l2 = launch {
        for (t in d2) {
            println("From d2: $t")
        }
        println("d2 closed")
    }

    runBlocking {
        for (i in 0 until 10) {
            c.send(i)
        }
        c.close()
        l1.join()
        l2.join()
    }
}