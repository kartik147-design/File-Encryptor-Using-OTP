package views;

import dao.UserDAO;
import model.User;
import service.GenerateOTP;
import service.SendOTPService;
import service.UserService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class Welcome {
    private JFrame frame;

    public void showWelcomeScreen() {
        frame = new JFrame("Welcome");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridLayout(3, 1));

        JButton loginButton = new JButton("Login");
        JButton signupButton = new JButton("Signup");
        JButton exitButton = new JButton("Exit");

        loginButton.addActionListener(e -> showLoginScreen());
        signupButton.addActionListener(e -> showSignupScreen());
        exitButton.addActionListener(e -> System.exit(0));

        panel.add(loginButton);
        panel.add(signupButton);
        panel.add(exitButton);

        frame.setVisible(true);
    }

    private void showLoginScreen() {
        frame.getContentPane().removeAll();
        frame.repaint();

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridLayout(3, 2));

        JLabel emailLabel = new JLabel("Email: ");
        JTextField emailField = new JTextField();
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            String email = emailField.getText();
            try {
                if (UserDAO.isExists(email)) {
                    String genOTP = GenerateOTP.getOTP();
                    SendOTPService.sendOTP(email, genOTP);
                    String otp = JOptionPane.showInputDialog("Enter the OTP sent to your email:");
                    if (otp != null && otp.equals(genOTP)) {
                        frame.dispose(); // Close current frame
                        UserView userViewGUI = new UserView(email);
                        userViewGUI.hashCode();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Wrong OTP", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "User not found", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(loginButton);

        frame.revalidate();
        frame.repaint();
    }

    private void showSignupScreen() {
        frame.getContentPane().removeAll();
        frame.repaint();

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridLayout(4, 2));

        JLabel nameLabel = new JLabel("Name: ");
        JTextField nameField = new JTextField();
        JLabel emailLabel = new JLabel("Email: ");
        JTextField emailField = new JTextField();
        JButton signupButton = new JButton("Signup");

        signupButton.addActionListener(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String genOTP = GenerateOTP.getOTP();
            SendOTPService.sendOTP(email, genOTP);
            String otp = JOptionPane.showInputDialog("Enter the OTP sent to your email:");
            if (otp != null && otp.equals(genOTP)) {
                User user = new User(name, email);
                int response = UserService.saveUser(user);
                switch (response) {
                    case 0:
                        JOptionPane.showMessageDialog(frame, "User registered successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case 1:
                        JOptionPane.showMessageDialog(frame, "User already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Wrong OTP", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(signupButton);

        frame.revalidate();
        frame.repaint();
    }
}
