package ie.atu.sw;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GameView extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private static final int MODEL_WIDTH = 30;
    private static final int MODEL_HEIGHT = 20;
    private static final int SCALING_FACTOR = 30;
    private static final int MIN_TOP = 2;
    private static final int MIN_BOTTOM = 18;
    private static final int PLAYER_COLUMN = 15;
    private static final int TIMER_INTERVAL = 100;

    private static final byte ONE_SET = 1;
    private static final byte ZERO_SET = 0;

    private final LinkedList<byte[]> model = new LinkedList<>();
    private final Dimension dim;
    private final Font font = new Font("Dialog", Font.BOLD, 50);
    private final Font over = new Font("Dialog", Font.BOLD, 100);

    private int prevTop = MIN_TOP;
    private int prevBot = MIN_BOTTOM;
    private int playerRow = 11;
    private int lastAction = 1;
    private int index = MODEL_WIDTH - 1;

    private long time;
    private boolean auto;
    private BasicNetwork neuralNet;

    private Sprite sprite;
    private Sprite dyingSprite;
    private javax.swing.Timer timer;

    public GameView(boolean auto) throws Exception {
        this.auto = auto;
        this.setBackground(Color.LIGHT_GRAY);
        this.setDoubleBuffered(true);

        dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
        setPreferredSize(dim);

        initModel();
        timer = new javax.swing.Timer(TIMER_INTERVAL, this);
        timer.start();
    }

    private void initModel() {
        for (int i = 0; i < MODEL_WIDTH; i++) model.add(new byte[MODEL_HEIGHT]);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        drawBackground(g2);
        drawModel(g2);
        drawHUD(g2);
        drawGameOver(g2);
    }

    private void drawBackground(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim.width, dim.height);
        g2.setFont(font);
        g2.setColor(Color.BLUE);
        g2.drawString(auto ? "AUTOPILOT" : "MANUAL", SCALING_FACTOR + 10, 3 * SCALING_FACTOR);
    }

    private void drawModel(Graphics2D g2) {
        for (int x = 0; x < MODEL_WIDTH; x++) {
            for (int y = 0; y < MODEL_HEIGHT; y++) {
                int x1 = x * SCALING_FACTOR;
                int y1 = y * SCALING_FACTOR;

                if (model.get(x)[y] != 0) {
                    if (x == PLAYER_COLUMN && y == playerRow) timer.stop();
                    g2.setColor(Color.BLACK);
                    g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
                }

                if (x == PLAYER_COLUMN && y == playerRow) {
                    Image img = timer.isRunning() ? sprite.getNext() : dyingSprite.getNext();
                    g2.drawImage(img, x1, y1, null);
                }
            }
        }
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(font);
        g2.setColor(Color.RED);
        g2.fillRect(SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
        g2.setColor(Color.WHITE);
        g2.drawString("Time: " + (int) (time * (TIMER_INTERVAL / 1000.0d)) + "s",
                SCALING_FACTOR + 10, (15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));
    }

    private void drawGameOver(Graphics2D g2) {
        if (!timer.isRunning()) {
            g2.setFont(over);
            g2.setColor(Color.RED);
            g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2 * SCALING_FACTOR);
        }
    }

    public void move(int step) {
        int newRow = playerRow + step;
        if (newRow >= 0 && newRow < MODEL_HEIGHT) {
            playerRow = newRow;
            repaint();
            System.out.println("✈️ Moved to row: " + playerRow);
        }
    }

    public void actionPerformed(ActionEvent e) {
        time++;
        repaint();

        index = (index + 1) % MODEL_WIDTH;
        generateNext();

        if (auto) {
            autoMove();
        } else if (time % 3 == 0) {
            recordManualData();
        }
    }

    private void autoMove() {
        try {
            if (neuralNet == null) {
                neuralNet = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File("./resources/model.eg"));
                System.out.println("✅ Neural model loaded.");
            }

            int[][] snapshot = getGridSnapshot(5);
            double[] flat = Arrays.stream(snapshot)
                                  .flatMapToInt(Arrays::stream)
                                  .asDoubleStream()
                                  .toArray(); // 100 values

            // Add the 101st value: normalized player row
            double[] inputVector = Arrays.copyOf(flat, flat.length + 1);
            inputVector[inputVector.length - 1] = playerRow / 19.0; // Normalize (0 to 1)

            MLData input = new BasicMLData(inputVector);
            double[] output = neuralNet.compute(input).getData();

            System.out.printf("🤖 Prediction Raw Output: Up=%.2f Stay=%.2f Down=%.2f%n", output[0], output[1], output[2]);

            int prediction = 0;
            for (int i = 1; i < output.length; i++) {
                if (output[i] > output[prediction]) prediction = i;
            }

            move(prediction - 1); // 0=Up, 1=Stay, 2=Down → -1=Up, 0=Stay, 1=Down
            lastAction = prediction;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void recordManualData() {
        try {
            int[][] snapshot = getGridSnapshot(5);
            double[] flat = Arrays.stream(snapshot)
                                  .flatMapToInt(Arrays::stream)
                                  .asDoubleStream()
                                  .toArray();

            double[] inputVector = Arrays.copyOf(flat, flat.length + 1);
            inputVector[inputVector.length - 1] = playerRow / 19.0;

            StringBuilder line = new StringBuilder();
            for (double d : inputVector) {
                line.append(d).append(",");  // ⚠️ keep decimal!
            }
            line.append(lastAction);

            FileWriter fw = new FileWriter("./resources/training_data.csv", true);
            fw.write(line.toString() + "\n");
            fw.close();

            lastAction = 1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    private void generateNext() {
        byte[] next = model.pollFirst();
        model.addLast(next);
        Arrays.fill(next, ONE_SET);

        int minspace = 4;
        prevTop += current().nextBoolean() ? 1 : -1;
        prevBot += current().nextBoolean() ? 1 : -1;

        prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace));
        prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

        Arrays.fill(next, prevTop, prevBot, ZERO_SET);
    }

    public int[][] getGridSnapshot(int columnsAhead) {
        int[][] snapshot = new int[columnsAhead][MODEL_HEIGHT];
        for (int i = 0; i < columnsAhead; i++) {
            int columnIndex = (PLAYER_COLUMN + 1 + i + index) % MODEL_WIDTH;
            byte[] column = model.get(columnIndex);
            for (int y = 0; y < MODEL_HEIGHT; y++) snapshot[i][y] = column[y];
        }
        return snapshot;
    }

    public double[] sample() {
        double[] vector = new double[MODEL_WIDTH * MODEL_HEIGHT];
        int i = 0;
        for (byte[] col : model) for (byte b : col) vector[i++] = b;
        return vector;
    }

    public void reset() {
        model.forEach(col -> Arrays.fill(col, ZERO_SET));
        playerRow = 11;
        time = 0;
        timer.restart();
    }

    public void setAutoMode(boolean isAuto) { this.auto = isAuto; }
    public void setSprite(Sprite sprite) { this.sprite = sprite; }
    public void setDyingSprite(Sprite dyingSprite) { this.dyingSprite = dyingSprite; }
    public void setLastAction(int action) { this.lastAction = action; }
    public int getLastAction() { return lastAction; }
}