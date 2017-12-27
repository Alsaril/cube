package display

import com.jogamp.opengl.GL2

object GLHelper {
    private val CIRCLE_VERTEX_COUNT = 36

    fun setColor(gl: GL2, r: Double, g: Double, b: Double, a: Double = 1.0) {
        gl.glColor4d(r, g, b, a)
    }

    fun drawLine(gl: GL2, x: Double, y: Double, w: Double, h: Double) {
        gl.glBegin(GL2.GL_LINES)
        gl.glVertex2d(x, y)
        gl.glVertex2d(x + w, y + h)
        gl.glEnd()
    }

    fun drawRect(gl: GL2, x: Double, y: Double, w: Double, h: Double, fill: Boolean) {
        if (fill) {
            gl.glBegin(GL2.GL_QUADS)
        } else {
            gl.glBegin(GL2.GL_LINE_LOOP)
        }
        gl.glVertex2d(x, y)
        gl.glVertex2d(x + w, y)
        gl.glVertex2d(x + w, y + h)
        gl.glVertex2d(x, y + h)
        gl.glEnd()
    }

    fun drawCircle(gl: GL2, x: Double, y: Double, r: Double, fill: Boolean) {
        if (fill) {
            gl.glBegin(GL2.GL_TRIANGLE_FAN)
            gl.glVertex2d(x, y)
        } else {
            gl.glBegin(GL2.GL_LINE_LOOP)
        }
        for (i in 0 until CIRCLE_VERTEX_COUNT) {
            gl.glVertex2d(x + r * Math.cos(2 * Math.PI / CIRCLE_VERTEX_COUNT * i),
                    y + r * Math.sin(2 * Math.PI / CIRCLE_VERTEX_COUNT * i))
        }
        gl.glEnd()
    }
}