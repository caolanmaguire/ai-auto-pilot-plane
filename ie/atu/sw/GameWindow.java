package ie.atu.sw;

import java.util.List;
//import java.util.List;
import java.util.ArrayList;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.JFrame;
import static ie.atu.sw.Runner.dataLogger;

public class GameWindow extends JFrame implements KeyListener {
	private GameView view;

	public GameWindow() throws Exception {
//		super("ATU - B.Sc. in Software Development");

		view = new GameView(true); // Use true to get the plane to fly in autopilot mode...
		view.setFocusable(true);
		view.requestFocusInWindow();

		getContentPane().setLayout(new FlowLayout());
		add(view);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 1000);
		setLocation(100, 100);
		pack();
		setVisible(true);

//		addKeyListener(this);
		view.addKeyListener(this);


		loadSprites();
	}

	/*
	 * Load the sprite graphics from the image directory
	 */
	public void loadSprites() throws Exception {
		var player = new Sprite("Player", 2, "images/0.png", "images/1.png");
		view.setSprite(player);

		var explosion = new Sprite("Explosion", 7, "images/2.png", "images/3.png", "images/4.png",
				"images/5.png", "images/6.png", "images/7.png", "images/8.png");
		view.setDyingSprite(explosion);
	}

	/*
	 * KEYBOARD OPTIONS
	 * ---------------- UP Arrow Key: Moves plane up DOWN Arrow Key: Moves plane
	 * down S: Resets and restarts the game
	 */
	private void logTrainingExample(int label) {
		try {
			List<Integer> inputVector = new ArrayList<>();
			int[][] grid = view.getGridSnapshot(5); // 5 columns ahead

			for (int x = 0; x < grid.length; x++) {
				for (int y = 0; y < grid[0].length; y++) {
					inputVector.add(grid[x][y]);
				}
			}

			if (dataLogger != null) {
				dataLogger.log(inputVector, label);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_S) {
			view.reset();
			return;
		}
		
		if (e.getKeyCode() == KeyEvent.VK_A) {
		    view.setAutoMode(true);
		    System.out.println("Switched to AUTOPILOT mode");
		    return;
		} else if (e.getKeyCode() == KeyEvent.VK_M) {
		    view.setAutoMode(false);
		    System.out.println("Switched to MANUAL mode");
		    return;
		}

		if (e.getKeyCode() == KeyEvent.VK_UP) {
			view.move(-1);
			view.setLastAction(0); // UP
			logTrainingExample(0);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			view.move(1);
			view.setLastAction(2); // DOWN
			logTrainingExample(2);
		} else {
			view.setLastAction(1); // STAY
			logTrainingExample(1);
		}
	}

	public void keyReleased(KeyEvent e) {

	}

	public void keyTyped(KeyEvent e) {

	}
}
