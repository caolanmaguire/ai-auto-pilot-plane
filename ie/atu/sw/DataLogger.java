package ie.atu.sw;

import java.io.*;
import java.util.*;

public class DataLogger {
    private BufferedWriter writer;

    public DataLogger(String filePath) throws IOException {
        writer = new BufferedWriter(new FileWriter(filePath, true)); // Append mode
    }

    public void log(List<Integer> inputVector, int action) throws IOException {
        for (int i = 0; i < inputVector.size(); i++) {
            writer.write(inputVector.get(i).toString());
            writer.write(",");
        }
        writer.write(String.valueOf(action)); // Label
        writer.newLine();
        writer.flush(); // 🆕 Ensures it writes immediately
    }


    public void close() throws IOException {
        writer.close();
    }
}
