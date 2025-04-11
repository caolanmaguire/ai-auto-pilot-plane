// 🔧 Updated NeuralTrainer.java using the new CSV format (11 inputs, 1 label)
package ie.atu.sw;

import org.encog.engine.network.activation.ActivationReLU;
import org.encog.engine.network.activation.ActivationSoftMax;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.persist.EncogDirectoryPersistence;

import java.io.*;
import java.util.*;

public class NeuralTrainer {
    private static final int INPUT_SIZE = 11;
    private static final int OUTPUT_SIZE = 3; // Up, Stay, Down

    public static void main(String[] args) throws Exception {
        List<double[]> inputs = new ArrayList<>();
        List<double[]> outputs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("./resources/training_data.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (parts.length != INPUT_SIZE + 1) continue;

                double[] input = new double[INPUT_SIZE];
                for (int i = 0; i < INPUT_SIZE; i++) {
                    input[i] = Double.parseDouble(parts[i]);
                }

                int action = Integer.parseInt(parts[INPUT_SIZE]);
                double[] output = new double[OUTPUT_SIZE];
                if (action == -1) output[0] = 1; // Up
                else if (action == 0) output[1] = 1; // Stay
                else if (action == 1) output[2] = 1; // Down

                inputs.add(input);
                outputs.add(output);
            }
        }

        MLDataSet dataSet = new BasicMLDataSet(
            inputs.toArray(new double[0][]),
            outputs.toArray(new double[0][])
        );

        BasicNetwork network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, INPUT_SIZE));
        network.addLayer(new BasicLayer(new ActivationReLU(), true, 16));
        network.addLayer(new BasicLayer(new ActivationSoftMax(), false, OUTPUT_SIZE));
        network.getStructure().finalizeStructure();
        network.reset();

        Backpropagation train = new Backpropagation(network, dataSet);
        int epoch = 0;
        do {
            train.iteration();
            System.out.printf("Epoch %d - Error: %.5f\n", epoch++, train.getError());
        } while (train.getError() > 0.01 && epoch < 100);

        train.finishTraining();
        EncogDirectoryPersistence.saveObject(new File("./resources/model.eg"), network);
        System.out.println("✅ Training complete. Model saved.");
    }
}