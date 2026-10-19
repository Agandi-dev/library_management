package library;

import java.awt.EventQueue;

import javax.swing.UIManager;

/**
 * Application entry point. Switches to the operating system's native look and feel
 * and opens the main window on the Swing event dispatch thread.
 */
public class LibraryManagementApp implements Runnable {

	/**
	 * @param args unused
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new LibraryManagementApp());
	}

	/**
	 * Configures the look and feel and creates the main application window.
	 */
	@Override
	public void run() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// fall back to the default Metal look and feel
		}
		new LibraryManagementFrame();
	}

}
