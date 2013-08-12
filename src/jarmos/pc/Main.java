package jarmos.pc;

import jarmos.SimulationResult;
import jarmos.geometry.GeometryData;
import jarmos.io.AModelManager;
import jarmos.io.AModelManager.ModelManagerException;
import jarmos.io.FileModelManager;
import jarmos.io.WebModelManager;
import jarmos.pc.visual.JOGLRenderer;
import jarmos.visual.ColorGenerator;
import jarmos.visual.VisualizationData;

import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.net.MalformedURLException;

import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFileChooser;

import rb.RBContainer;
import rb.RBSystem;

import com.jogamp.opengl.util.Animator;

/**
 * Main program for desktop-based reduced model simulation using Java.
 * 
 * @author Daniel Wirtz
 */
public class Main {

	private float x = 0, y = 0;

	/**
	 * @param args
	 * @throws ModelManagerException
	 */
	public static void main(String[] args) throws ModelManagerException {

		// Create a file chooser
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		AModelManager f = null;
		if (args != null && args.length == 1) {
			File dir = new File(args[0]);
			if (dir.exists() && dir.isDirectory()) {
				f = new FileModelManager(dir.getParent());
				f.useModel(dir.getName());
			} else {
				throw new RuntimeException("Directory not found or invalid: " + args[0]);
			}
		} else if (fc.showDialog(null, "Select model directory") == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			if (file.exists() && file.isDirectory()) {
				f = new FileModelManager(file.getParent());
				f.useModel(file.getName());
			}
		} else {
			try {
				f = new WebModelManager("http://www.agh.ians.uni-stuttgart.de/jarmosa");
				f.useModel("rbm_advec");
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
		}

		RBContainer rb = new RBContainer();
		rb.loadModel(f);

		// Perform the solve
		RBSystem s = rb.mRbSystem;
		double[] par = s.getParams().getRandomParam();
		s.getParams().setCurrent(par);
		s.computeRBSolution(s.getNBF());
		SimulationResult res = s.getSimulationResults();

		// s.performSweep(0, 4);
		// SimulationResult res = s.getSweepSimResults();

		GeometryData g = rb.mRbSystem.getGeometry();
		VisualizationData v = new VisualizationData(g);

		v.useResult(res);
		v.computeVisualFeatures(new ColorGenerator());

		Main m = new Main();
		m.visualize(v);
	}

	public void visualize(VisualizationData vData) {
		final Frame frame = new java.awt.Frame("Model visualization");
		frame.setSize(400, 600);
		frame.setLayout(new java.awt.BorderLayout());

		final Animator animator = new Animator();
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				// Run this on another thread than the AWT event queue to
				// make sure the call to Animator.stop() completes before
				// exiting
				new Thread(new Runnable() {
					public void run() {
						animator.stop();
						System.exit(0);
					}
				}).start();
			}
		});

		GLCanvas canvas = new GLCanvas();
		animator.add(canvas);
		// GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
		// GLCanvas canvas = new GLCanvas(caps);

		final JOGLRenderer rend = new JOGLRenderer(vData, 700, 400);
		canvas.addGLEventListener(rend);

		canvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					rend.nextColorField();
					break;
				case MouseEvent.BUTTON3:
					rend.isFrontFace = !rend.isFrontFace;
					break;
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				x = e.getX();
				y = e.getY();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				// isDown = false;
			}
		});

		canvas.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() > 0)
					rend.zoomIn();
				else
					rend.zoomOut();
			}
		});

		canvas.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				float smooth = 40;
				rend.addPos((e.getX() - x) / smooth, (y - e.getY()) / smooth);
				rend.isContinuousRotation = true;
				x = e.getX();
				y = e.getY();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
			}
		});

		frame.add(canvas, java.awt.BorderLayout.CENTER);
		frame.validate();

		frame.setVisible(true);
		animator.start();
	}

}
