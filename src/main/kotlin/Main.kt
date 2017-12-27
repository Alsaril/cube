import com.jogamp.opengl.GL2
import display.GLDisplay
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val field = Field()

    GLDisplay("cube",
            FIELD_WIDTH * CELL_SIZE,
            FIELD_HEIGHT * CELL_SIZE,
            GLDisplay.CoordSystem.CORNER,
            object : GLDisplay.Draw {
                override fun init(gl: GL2) = Unit

                override fun display(gl: GL2) {
                    field.display(gl)
                }
            })
    runBlocking {
        field.start()
    }
}