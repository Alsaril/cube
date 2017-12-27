package display

import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.Animator
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.WindowConstants

class GLDisplay(title: String, width: Int, height: Int, coordSystem: CoordSystem, draw: Draw) {

    private val frame: JFrame = JFrame(title)
    private val canvas: GLCanvas
    private lateinit var side: JPanel
    private lateinit var bottom: JTextArea

    init {
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        canvas = GLCanvas()
        canvas.preferredSize = Dimension(width, height)
        canvas.addGLEventListener(GLEL(width, height, draw, coordSystem.value))
        frame.add(canvas, BorderLayout.CENTER)
        frame.pack()
        frame.isVisible = true
        val animator = Animator(canvas)
        animator.setRunAsFastAsPossible(true)
        animator.start()
    }

    fun enableSide(width: Int, height: Int): JPanel {
        side = JPanel()
        side.preferredSize = Dimension(width, height)
        frame.add(side, BorderLayout.EAST)
        frame.pack()
        return side
    }

    fun enableBottom(width: Int, height: Int): JTextArea {
        bottom = JTextArea()
        bottom.preferredSize = Dimension(width, height)
        frame.add(bottom, BorderLayout.SOUTH)
        frame.pack()
        return bottom
    }

    fun addKeyListener(listener: KeyListener) {
        canvas.addKeyListener(listener)
    }

    interface Draw {
        fun init(gl: GL2)

        fun display(gl: GL2)
    }

    enum class CoordSystem private constructor(val value: Boolean) {
        CENTER(true), CORNER(false)
    }

    private class GLEL constructor(private val width: Int, private val height: Int, private val draw: Draw, private val coordSystem: Boolean) : GLEventListener {

        override fun init(glAutoDrawable: GLAutoDrawable) {
            val gl = glAutoDrawable.gl.gL2
            gl.glClearColor(1f, 1f, 1f, 1f)
            gl.glEnable(GL2.GL_BLEND)
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA)
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST)
            gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT, GL2.GL_NICEST)
            gl.glEnable(GL2.GL_LINE_SMOOTH)
            gl.glEnable(GL2.GL_POLYGON_SMOOTH)
            gl.glEnable(GL2.GL_MULTISAMPLE)
            draw.init(gl)
        }

        override fun dispose(glAutoDrawable: GLAutoDrawable) {}

        override fun display(glAutoDrawable: GLAutoDrawable) {
            val gl = glAutoDrawable.gl.gL2
            draw.display(gl)
        }

        override fun reshape(glAutoDrawable: GLAutoDrawable, x: Int, y: Int, w: Int, h: Int) {
            val gl = glAutoDrawable.gl.gL2
            gl.glMatrixMode(GL2.GL_PROJECTION)
            gl.glLoadIdentity()
            if (coordSystem) {
                gl.glOrtho((-w / 2).toDouble(), (w / 2).toDouble(), (-h / 2).toDouble(), (h / 2).toDouble(), -1.0, 1.0)
            } else {
                gl.glOrtho(0.0, w.toDouble(), 0.0, h.toDouble(), -1.0, 1.0)
            }
            gl.glMatrixMode(GL2.GL_MODELVIEW)
        }
    }
}
