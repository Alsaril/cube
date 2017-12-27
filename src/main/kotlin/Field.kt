import com.jogamp.opengl.GL2
import display.GLHelper
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.util.*

class Field {
    val cells = Array(FIELD_WIDTH) { Array(FIELD_HEIGHT) { CellType.EMPTY } }
    val entities = ArrayList<Entity>()
    private val mutex = Mutex()

    init {
        generateWalls()
        regenerateStatic()
    }

    suspend fun start() {
        while (true) {
            while (entities.size < MINIMAL_SIZE) {
                generateEntity()
            }

            val closed = mutableListOf<Entity>()

            select<Unit> {
                entities.forEach { entity ->
                    entity.channel.onReceiveOrNull {
                        if (it == null) {
                            closed.add(entity)
                        } else {
                            processMessage(it)
                        }
                    }
                }
            }

            mutex.withLock {
                entities.removeAll(closed)
            }
        }
    }

    private suspend fun processMessage(message: Message) {
        println("new message at ${System.currentTimeMillis()}: ${message}")
        when (message) {
            is MoveMessage -> with(message) {
                val moveResult = when (cells[xt][yt]) {
                    CellType.FOOD -> {
                        MoveResult(xt, yt, true, FOOD_HEALTH)
                    }
                    CellType.POISON -> MoveResult(x, y, false)
                    CellType.EMPTY -> MoveResult(x, y)
                    else -> MoveResult(x, y, true)
                }
                cells[x][y] = CellType.EMPTY
                cells[moveResult.x][moveResult.y] = CellType.ENTITY
                result.complete(moveResult)
            }
            is GetMessage -> {
                message.result.complete(cells[message.x][message.y].innerValue)
            }
            is InteractMessage -> with(message) {
                val interactResult = when (cells[x][y]) {
                    CellType.FOOD -> {
                        cells[x][y] = CellType.EMPTY
                        FOOD_HEALTH
                    }
                    CellType.POISON -> {
                        cells[x][y] = CellType.FOOD
                        0
                    }
                    else -> 0
                }
                result.complete(interactResult)
            }
            is DieMessage -> cells[message.x][message.y] = CellType.EMPTY
        }
    }

    private fun generateEntity() {
        val entity = Entity(Array(CODE_LENGTH) { random.nextInt(INSTRUCTIONS_COUNT) })
        while (!cells[entity.x][entity.y].isEmpty()) {
            entity.x = random.nextInt(FIELD_WIDTH)
            entity.y = random.nextInt(FIELD_HEIGHT)
        }
        entities.add(entity)
        cells[entity.x][entity.y] = CellType.ENTITY
        entity.start()
    }

    private fun generateWalls() {
        repeat(FIELD_WIDTH) {
            cells[it][0] = CellType.WALL
            cells[it][FIELD_HEIGHT - 1] = CellType.WALL
        }

        repeat(FIELD_HEIGHT) {
            cells[0][it] = CellType.WALL
            cells[FIELD_WIDTH - 1][it] = CellType.WALL
        }
    }

    private fun regenerateStatic() {
        val count = cells.sumBy { it.sumBy { if (it == CellType.FOOD || it == CellType.POISON) 1 else 0 } }
        repeat(STATIC_COUNT - count) {
            var x = 0
            var y = 0
            while (!cells[x][y].isEmpty()) {
                x = random.nextInt(FIELD_WIDTH)
                y = random.nextInt(FIELD_HEIGHT)
            }

            cells[x][y] = if (random.nextBoolean()) CellType.FOOD else CellType.POISON
        }
    }

    private fun n(health: Int, v: Double): Double {
        val nv = v * 1.0 * health / START_HEALTH
        return if (nv > 1) 1.0 else nv
    }


    fun display(gl: GL2) {
        repeat(FIELD_WIDTH) { i ->
            repeat(FIELD_HEIGHT) { j ->
                val cell = cells[i][j]
                GLHelper.setColor(gl, cell.red, cell.green, cell.blue)
                GLHelper.drawRect(gl, i * CELL_SIZE.toDouble(), j * CELL_SIZE.toDouble(), CELL_SIZE.toDouble(), CELL_SIZE.toDouble(), true)
            }
        }
        runBlocking {
            mutex.withLock {
                entities.forEach {
                    it.display(gl)
                }
            }
        }
    }
}

enum class CellType(val red: Double, val green: Double, val blue: Double, val innerValue: Int) {
    EMPTY(0.8, 0.8, 0.8, 0), WALL(0.2, 0.2, 0.2, 1), FOOD(0.2, 0.7, 0.2, 2), POISON(0.9, 0.2, 0.2, 3), ENTITY(0.0, 0.0, 0.0, 4);

    fun isEmpty() = this == EMPTY
}