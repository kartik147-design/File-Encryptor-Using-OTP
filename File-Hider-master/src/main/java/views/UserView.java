package views;

import dao.DataDAO;
import model.Data;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class UserView {
    private JFrame frame;
    private String email;
    private static final String host = "imap.gmail.com";
    private static final String username = "srivastavakritikey4@gmail.com";
    private static final String password = "dgki mece dabf kndu";
    private int lastMessageCount = 0;
    private AtomicBoolean monitorEmails = new AtomicBoolean(false);

    public UserView(String email) {
        this.email = email;
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Home");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridLayout(8, 1));

        JLabel welcomeLabel = new JLabel("Welcome " + email);
        panel.add(welcomeLabel);

        JButton showFilesButton = new JButton("Show Hidden Files");
        JButton hideFileButton = new JButton("Hide a New File");
        JButton unhideFileButton = new JButton("Unhide a File");
        JButton segregateFilesButton = new JButton("Segregate Files");
        JButton sendEmailButton = new JButton("Send Email/Attachments");
        JButton readInbox = new JButton("Read Inbox");
        JButton activatePushNotificationButton = new JButton("Activate Push Email Notification");
        JButton checkForAbusiveEmailsButton = new JButton("Toggle Email Monitoring");
        JButton exitButton = new JButton("Exit");

        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exitPanel.add(exitButton);
        panel.add(exitPanel, BorderLayout.SOUTH);

        showFilesButton.addActionListener(e -> {
            try {
                List<Data> files = DataDAO.getAllFiles(email);
                StringBuilder sb = new StringBuilder("ID - File Name\n");
                for (Data file : files) {
                    sb.append(file.getId()).append(" - ").append(file.getFileName()).append("\n");
                }
                JOptionPane.showMessageDialog(frame, sb.toString(), "Hidden Files", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to fetch hidden files", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        hideFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Text or Image File to Hide");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text and Image Files", "txt", "jpg", "jpeg", "png"));

            int userSelection = fileChooser.showOpenDialog(frame);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                String fileName = selectedFile.getName().toLowerCase();

                if (fileName.endsWith(".txt") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                    // Determine hidden directory path
                    String hiddenDirPath = System.getProperty("user.home") + File.separator + ".hiddenFiles";
                    File hiddenDir = new File(hiddenDirPath);
                    if (!hiddenDir.exists()) {
                        hiddenDir.mkdirs();
                    }

                    // New hidden file path
                    String hiddenFilePath = hiddenDirPath + File.separator + selectedFile.getName();
                    File hiddenFile = new File(hiddenFilePath);

                    try {
                        // Copy file to hidden location
                        Files.copy(selectedFile.toPath(), hiddenFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        // Create Data object
                        Data file = new Data(0, selectedFile.getName(), hiddenFilePath, email);

                        // Hide file in the database
                        DataDAO.hideFile(file);

                        // Delete original file from the desktop
                        boolean deleted = selectedFile.delete();

                        if (deleted) {
                            JOptionPane.showMessageDialog(frame, "File hidden!.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(frame, "File hidden but failed to delete from the desktop.", "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (SQLException | IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Failed to hide file", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Selected file is not a supported text or image file.", "Unsupported File", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        unhideFileButton.addActionListener(e -> {
            try {
                List<Data> files = DataDAO.getAllFiles(email);
                StringBuilder sb = new StringBuilder("ID - File Name\n");
                for (Data file : files) {
                    sb.append(file.getId()).append(" - ").append(file.getFileName()).append("\n");
                }
                String fileIdStr = JOptionPane.showInputDialog(frame, sb.toString(), "Unhide File", JOptionPane.PLAIN_MESSAGE);
                if (fileIdStr != null) {
                    int fileId = Integer.parseInt(fileIdStr.trim());
                    boolean isValidID = files.stream().anyMatch(file -> file.getId() == fileId);
                    if (isValidID) {
                        DataDAO.unhide(fileId);
                        JOptionPane.showMessageDialog(frame, "File unhidden successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Invalid File ID", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException | IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to unhide file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        segregateFilesButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Directory to Segregate Files");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int userSelection = fileChooser.showOpenDialog(frame);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File directory = fileChooser.getSelectedFile();
                segregateFiles(directory);
            }
        });

        sendEmailButton.addActionListener(e -> {
            sendEmail();
        });

        readInbox.addActionListener(e -> readEmails());

        activatePushNotificationButton.addActionListener(e -> {
            Timer timer = new Timer(10000, evt -> checkForNewEmails());
            timer.setInitialDelay(0);
            timer.start();
            JOptionPane.showMessageDialog(frame, "Push email notification activated.", "Activated", JOptionPane.INFORMATION_MESSAGE);
        });


        checkForAbusiveEmailsButton.addActionListener(e -> {
            monitorEmails.set(!monitorEmails.get());
            if (monitorEmails.get()) {
                JOptionPane.showMessageDialog(frame, "Email monitoring activated.", "Activated", JOptionPane.INFORMATION_MESSAGE);
                checkForAbusiveEmails();
            } else {
                JOptionPane.showMessageDialog(frame, "Email monitoring deactivated.", "Deactivated", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        exitButton.addActionListener(e -> System.exit(0));

        panel.add(showFilesButton);
        panel.add(hideFileButton);
        panel.add(unhideFileButton);
        panel.add(segregateFilesButton);
        panel.add(sendEmailButton);
        panel.add(readInbox);
        panel.add(activatePushNotificationButton);
        panel.add(checkForAbusiveEmailsButton);
        panel.add(exitButton);

        frame.setVisible(true);
    }

    public void segregateFiles(File directory) {
        File[] allFiles = directory.listFiles();
        if (allFiles != null) {
            for (File file : allFiles) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    String destDirPath = directory.getAbsolutePath();

                    if (fileName.endsWith(".pdf")) {
                        moveFile(file, createDirectory(destDirPath, "PDF"), fileName);
                    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                        moveFile(file, createDirectory(destDirPath, "IMAGE"), fileName);
                    } else if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                        moveFile(file, createDirectory(destDirPath, "DOCUMENT"), fileName);
                    } else if (fileName.endsWith(".txt")) {
                        moveFile(file, createDirectory(destDirPath, "TEXT"), fileName);
                    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                        moveFile(file, createDirectory(destDirPath, "VIDEO"), fileName);
                    } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                        moveFile(file, createDirectory(destDirPath, "AUDIO"), fileName);
                    }
                }
            }
            JOptionPane.showMessageDialog(frame, "Files segregated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "No files found in selected directory.", "Empty Directory", JOptionPane.WARNING_MESSAGE);
        }
    }

    private File createDirectory(String parentDirPath, String dirName) {
        File directory = new File(parentDirPath + File.separator + dirName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private void moveFile(File file, File destDir, String fileName) {
        File destFile = new File(destDir.getAbsolutePath() + File.separator + fileName);
        file.renameTo(destFile);
    }

    private void sendEmail() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField toField = new JTextField(20);
        JTextField subjectField = new JTextField(20);
        JTextArea messageArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        JButton attachButton = new JButton("Attach File");
        JLabel attachmentLabel = new JLabel("No file attached");
        final File[] attachmentFile = {null};

        panel.add(new JLabel("To:"));
        panel.add(toField);
        panel.add(new JLabel("Subject:"));
        panel.add(subjectField);
        panel.add(new JLabel("Message:"));
        panel.add(scrollPane);
        panel.add(attachButton);
        panel.add(attachmentLabel);

        attachButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select File to Attach");
            int userSelection = fileChooser.showOpenDialog(frame);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                attachmentFile[0] = fileChooser.getSelectedFile();
                attachmentLabel.setText("Attached: " + attachmentFile[0].getName());
            }
        });

        int result = JOptionPane.showConfirmDialog(frame, panel, "Send Email", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String to = toField.getText().trim();
            String subject = subjectField.getText().trim();
            String message = messageArea.getText().trim();

            if (!to.isEmpty() && !subject.isEmpty() && !message.isEmpty()) {
                sendEmailInBackground(to, subject, message, attachmentFile[0]);
            } else {
                JOptionPane.showMessageDialog(frame, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendEmailInBackground(String to, String subject, String message, File attachment) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                Message mimeMessage = new MimeMessage(session);
                mimeMessage.setFrom(new InternetAddress(username));
                mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                mimeMessage.setSubject(subject);
                mimeMessage.setSentDate(new Date());

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(message);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);

                if (attachment != null) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachment);
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(attachment.getName());
                    multipart.addBodyPart(attachmentPart);
                }

                mimeMessage.setContent(multipart);

                Transport.send(mimeMessage);
                JOptionPane.showMessageDialog(frame, "Email sent successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (MessagingException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to send email", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    public void readEmails() {
        new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            try {
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(host, username, password);

                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                Message[] messages = inbox.getMessages();
                int messageCount = messages.length;
                int start = Math.max(0, messageCount - 3); // Get the last 3 messages
                StringBuilder sb = new StringBuilder();

                for (int i = messageCount - 1; i >= start; i--) {
                    Message message = messages[i];
                    sb.append("From: ").append(InternetAddress.toString(message.getFrom())).append("\n");
                    sb.append("Subject: ").append(message.getSubject()).append("\n");
                    sb.append("Date: ").append(message.getReceivedDate()).append("\n\n");
                }

                JOptionPane.showMessageDialog(frame, sb.toString(), "Inbox", JOptionPane.INFORMATION_MESSAGE);
            } catch (MessagingException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to read emails", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void checkForNewEmails() {
        new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            try {
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(host, username, password);

                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                while (true) {
                    int currentMessageCount = inbox.getMessageCount();
                    if (currentMessageCount > lastMessageCount) {
                        // New emails have arrived
                        int newMessagesCount = currentMessageCount - lastMessageCount;
                        lastMessageCount = currentMessageCount;

                        // Fetch new messages
                        Message[] newMessages = inbox.getMessages(currentMessageCount - newMessagesCount + 1, currentMessageCount);
                        StringBuilder sb = new StringBuilder();
                        for (Message message : newMessages) {
                            sb.append("From: ").append(InternetAddress.toString(message.getFrom())).append("\n");
                            sb.append("Subject: ").append(message.getSubject()).append("\n");
                            sb.append("Date: ").append(message.getReceivedDate()).append("\n\n");
                        }

                        SwingUtilities.invokeLater(() -> {
                            // Show notification for new emails
                            JOptionPane.showMessageDialog(frame, sb.toString(), "New Email Notification", JOptionPane.INFORMATION_MESSAGE);
                        });
                    }

                    Thread.sleep(10000); // Check every 10 seconds
                }
            } catch (MessagingException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void checkForAbusiveEmails() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(host, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE); // Open inbox in read-write mode to delete messages

            int currentMessageCount = inbox.getMessageCount();
            if (currentMessageCount > lastMessageCount) {
                // New emails have arrived
                int newMessagesCount = currentMessageCount - lastMessageCount;
                lastMessageCount = currentMessageCount;

                // Fetch only the latest message
                Message latestMessage = inbox.getMessage(currentMessageCount);

                String content = getTextFromMessage(latestMessage);
                if (containsAbusiveLanguage(content)) {
                    int option = JOptionPane.showConfirmDialog(frame, "Abusive email detected:\n\n" +
                            "From: " + InternetAddress.toString(latestMessage.getFrom()) + "\n" +
                            "Subject: " + latestMessage.getSubject() + "\n\n" +
                            "Do you want to delete this email?", "Abusive Email Detected", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        latestMessage.setFlag(Flags.Flag.DELETED, true);
                        JOptionPane.showMessageDialog(frame, "Abusive email deleted.", "Email Deleted", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to check for abusive emails.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private boolean containsAbusiveLanguage(String content) {
        String[] abusiveWords = {"abusiveWord1", "abusiveWord2", "abusiveWord3"}; // Add more abusive words as needed
        for (String word : abusiveWords) {
            if (content.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            String result = "";
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result += bodyPart.getContent().toString();
                    break;
                }
            }
            return result;
        }
        return "";
    }
}
