/**
 * 
 */
package jarmos.pc.visual;

import jarmos.visual.OpenGLBase;
import jarmos.visual.VisualizationData;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 * @author CreaByte
 * 
 */
public class JOGLRenderer extends OpenGLBase implements GLEventListener {

	public JOGLRenderer(VisualizationData vData, int width, int height) {
		super(vData, width, height);
	}

	/**
	 * @see javax.media.opengl.GLEventListener#display(javax.media.opengl.GLAutoDrawable)
	 */
	@Override
	public void display(GLAutoDrawable drawable) {
		// clear the screen to black (0,0,0) color
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		if (!is2D()) // enable depth test for 3D rendering
			gl.glEnable(GL2.GL_DEPTH_TEST);

		if ((isFrontFace) || (is2D())) {
			// enable blending (for rendering wireframe)
			if (!is2D())
				gl.glDisable(GL2.GL_CULL_FACE);
			gl.glFrontFace(GL2.GL_CCW);
		} else {
			gl.glEnable(GL2.GL_CULL_FACE);
			gl.glFrontFace(GL2.GL_CW);
		}

		// reset transformation matrix
		gl.glLoadIdentity();

		// setup Light
		if (!is2D()) {
			gl.glEnable(GL2.GL_LIGHTING); // Enable light
			gl.glEnable(GL2.GL_LIGHT0); // turn on the light
			gl.glEnable(GL2.GL_COLOR_MATERIAL); // turn on color lighting

			// material shininess
			gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 128);

			// ambient light
			float lightAmbient[] = { 0.5f, 0.5f, 0.5f, 1.0f };
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
			// diffuse light
			float lightDiffuse[] = { 0.8f, 0.8f, 0.8f, 1.0f };
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
			// specular light
			float[] lightSpecular = { 0.7f, 0.7f, 0.7f, 1.0f };
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);
			// light position
			float[] lightPosition = { -getBoxSize(), -getBoxSize(), 0.0f, 0.0f };
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
			// light direction
			float[] lightDirection = { getBoxSize(), getBoxSize(), getBoxSize(), 0.0f };
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, lightDirection, 0);
			// 90 degree FOV
			gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 45.0f);

			// using our normal data
			floatBuf.position(getCurrentNormalsOffset());
			gl.glNormalPointer(GL2.GL_FLOAT, 0, floatBuf);
		}

		// zoom in/out the model
		gl.glScalef(getScalingFactor(), getScalingFactor(), getScalingFactor());

		/*
		 * Control view 2D: we just move the object around 3D: we rotate the
		 * object
		 */
		if (is2D()) {
			gl.glTranslatef(getXTranslation(), getYTranslation(), 0);
		} else {
			gl.glMultMatrixf(getRotationMatrix(), 0);
		}

		/*
		 * Set pointer to vertex data Always uses currentFrame, which is zero in
		 * case of no animation.
		 */
		floatBuf.position(getCurrentVertexOffset());
		gl.glVertexPointer(3, GL2.GL_FLOAT, 0, floatBuf);

		/*
		 * specify the color data for the current frame Four values each: R, G,
		 * B, Alpha
		 */
		floatBuf.position(getCurrentColorOffset());
		gl.glColorPointer(4, GL2.GL_FLOAT, 0, floatBuf);

		/*
		 * Draw the elements using the above declared nodes and color data
		 */
		shortBuf.position(getFaceOffset());
		gl.glDrawElements(GL2.GL_TRIANGLES, getNumFaces() * 3, GL2.GL_UNSIGNED_SHORT, shortBuf);

		// Draw the wireframe for a n field object
		// vData.isConstantFeature(currentColorField))
		if (!is2D()) {
			// Draw the wireframe mesh
			gl.glColor4f(0.1f, 0.1f, 0.1f, 0.5f);
			shortBuf.position(getCurrentWireframeOffset());
			gl.glDrawElements(GL2.GL_LINES, getNumFaces() * 6, GL2.GL_UNSIGNED_SHORT, shortBuf);
		}

		frameRendered();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.media.opengl.GLEventListener#dispose(javax.media.opengl.GLAutoDrawable
	 * )
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.media.opengl.GLEventListener#init(javax.media.opengl.GLAutoDrawable
	 * )
	 */
	@Override
	public void init(GLAutoDrawable drawable) {
		initRendering();

		// define the color we want to be displayed as the "clipping wall"
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_PROJECTION);

		// orthographic view
		float[] o = getOrtographicProj();
		gl.glOrthof(o[0], o[1], o[2], o[3], o[4], o[5]);

		gl.glViewport(0, 0, getWidth(), getHeight());
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		// define the color we want to be displayed as the "clipping wall"
		// gl.glClearColor(0f, 0f, 0f, 1.0f);
		gl.glClearColor(1f, 1f, 1f, 1.0f);

		// enable the differentiation of which side may be visible
		gl.glEnable(GL2.GL_CULL_FACE);
		// which is the front? the one which is drawn counter clockwise
		gl.glFrontFace(GL2.GL_CCW);
		// which one should NOT be drawn
		gl.glCullFace(GL2.GL_BACK);

		// Switch on client states in order to make GL10 use the vertex and
		// color data
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

		// Enable normal for 3D object
		if (!is2D())
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
	}

	/**
	 * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable,
	 *      int, int, int, int)
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		setSize(w, h);
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(0, 0, w, h);
	}

}
