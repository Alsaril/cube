import com.jogamp.opengl.GL2
import display.GLHelper
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class Entity(val code: Array<Int>) {
    var x = 0
    var y = 0
    private var health = START_HEALTH
    private var direction = LimitedInt(NEIGHBOURS_COUNT)
    private var counter = LimitedInt(INSTRUCTIONS_COUNT)
    private var stack = LimitedStack(STACK_LIMIT, 0)
    private var memory = Array(MEMORY_SIZE) { 0 }
    private var memoryPointer = LimitedInt(MEMORY_SIZE)
    val channel = Channel<Message>()
    val id = random.nextLong()

    override fun equals(other: Any?): Boolean {
        if (other is Entity && id == other.id) {
            return true
        }
        return false
    }

    fun isAlive() = health > 0

    fun direction(d: Int) = (d + direction.value) % NEIGHBOURS_COUNT

    fun aheadX(d: Int): Int {
        val direction = direction(d)
        if (direction in 2..4) return x + 1;
        if (direction in 6..7 || direction == 0) return x - 1;
        return x
    }

    private fun aheadY(d: Int): Int {
        val direction = direction(d)
        if (direction > -1 && direction < 3) return y + 1
        return if (direction in 4..6) y - 1 else y
    }

    private fun n(v: Double): Double {
        val nv = v * 1.0 * health / START_HEALTH
        return if (nv > 1) 1.0 else nv
    }

    fun display(gl: GL2) {
        GLHelper.setColor(gl, n(0.4), n(0.4), n(0.9));
        GLHelper.drawRect(gl, x * CELL_SIZE.toDouble(), y * CELL_SIZE.toDouble(), CELL_SIZE.toDouble(), CELL_SIZE.toDouble(), true)

        val color = if (n(0.5) < 0.5) 1.0 else 0.0
        GLHelper.setColor(gl, color, color, color)
        GLHelper.drawLine(gl,
                x * CELL_SIZE + CELL_SIZE / 2.0, y * CELL_SIZE + CELL_SIZE / 2.0,
                CELL_SIZE / 2.5 * Math.cos(direction.value / 7.0 * Math.PI * 2),
                CELL_SIZE / 2.5 * Math.sin(direction.value / 7.0 * Math.PI * 2))
    }

    fun start() = launch {
        main@ while (isAlive()) {
            delay(10)
            val command = code[counter.value]
            when (command) {
                in 0..7 -> {
                    val deferredResult = CompletableDeferred<MoveResult>()
                    channel.send(MoveMessage(x, y, aheadX(command), aheadY(command), deferredResult))
                    val result = deferredResult.await()
                    if (result.alive) {
                        x = result.x
                        y = result.y
                    } else {
                        health = 0
                    }
                    health -= MOVE_COST
                }
                in 8..15 -> {
                    val deferredResult = CompletableDeferred<Int>()
                    channel.send(GetMessage(aheadX(command - 8), aheadY(command - 8), deferredResult))
                    stack.push(deferredResult.await())
                    health -= LOOK_COST
                }
                in 16..23 -> {
                    direction.add(command - 16)
                    health -= ROTATE_COST
                }
                in 24..31 -> {
                    val deferredResult = CompletableDeferred<Int>()
                    channel.send(InteractMessage(aheadX(command - 24), aheadY(command - 24), deferredResult))
                    val result = deferredResult.await()
                    health += result
                    health -= INTERACT_COST
                }
                in 32..39 -> stack.push(command - 32)
                40 -> {
                    counter.add(stack.pop())
                    continue@main
                }
                41 -> memoryPointer.set(stack.pop())
                42 -> stack.push(memoryPointer.value)
                43 -> memoryPointer.add(1)
                44 -> memoryPointer.add(-1)
                45 -> memory[memoryPointer.value] = stack.pop()
                46 -> stack.push(memory[memoryPointer.value])
                47 -> stack.push(stack.pop() + stack.pop())
                48 -> stack.push(stack.pop() - stack.pop())
                49 -> stack.push(stack.pop() * stack.pop())
                50 -> {
                    val a = stack.pop()
                    var b = stack.pop()
                    if (b == 0) {
                        b = 1
                    }
                    stack.push(a / b)
                }
                51 -> stack.push(stack.pop() shl stack.pop())
                52 -> stack.push(stack.pop() ushr stack.pop())
                53 -> stack.push(((stack.pop() and 7) shl 6) or (stack.pop() and 7))
                54 -> {
                    val first = stack.pop()
                    val second = stack.pop()

                    stack.push(first)
                    stack.push(second)
                }
                else -> {
                }
            }
            health -= INNER_COST
            counter.add(1)
        }
        channel.send(DieMessage(x, y))
        channel.close()
    }

}