package ie.atu.sw;

import javax.swing.SwingUtilities;
import static java.lang.System.*;

import java.io.IOException;

public class Runner {
	public static DataLogger dataLogger;
	
	public static void main(String[] args) throws Exception {
		
		dataLogger = new DataLogger("./resources/training_data.csv");
		
		GameWindow window = new GameWindow();
		
		
		/*
		 * Always run a GUI in a separate thread from the main thread.
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dataLogger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
//		/*SwingUtilities.invokeAndWait(() -> { //Sounds like the Command Pattern at work!
//			try {
//				new GameWindow();
//			} catch (Exception e) {
//				out.println("[ERROR] Yikes...problem starting up " + e.getMessage());
//			}
//		});*/
	}
}