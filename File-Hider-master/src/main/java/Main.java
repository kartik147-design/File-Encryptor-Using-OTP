import views.Welcome;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Welcome welcomeGUI = new Welcome();
            welcomeGUI.showWelcomeScreen();

        });
    }
}
