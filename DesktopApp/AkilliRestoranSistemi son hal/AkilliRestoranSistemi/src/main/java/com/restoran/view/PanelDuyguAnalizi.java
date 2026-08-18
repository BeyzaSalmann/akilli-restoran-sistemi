package com.restoran.view;

import com.restoran.util.Env;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import javax.imageio.ImageIO;

public class PanelDuyguAnalizi extends JPanel {

    private static final String SERVER_IP = Env.get("EMOTION_SERVER_HOST", "localhost");
    private static final int SERVER_PORT = Env.getInt("EMOTION_SERVER_PORT", 9999);

    private BufferedImage currentFrame = null;
    private JLabel lblAnlikDuygu;
    private JPanel videoPanel;
    private boolean isRunning = true;

    public PanelDuyguAnalizi() {
        initComponents();
        startCameraConnection();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setBackground(new Color(248, 249, 250));

        JLabel lblAnaBaslik = new JLabel("📹 Gerçek Zamanlı Duygu Analizi (Kamera)");
        lblAnaBaslik.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblAnaBaslik.setForeground(new Color(33, 37, 41));
        lblAnaBaslik.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(lblAnaBaslik, BorderLayout.NORTH);

        JPanel ortaPanel = new JPanel(new BorderLayout(15, 15));
        ortaPanel.setOpaque(false);

        videoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentFrame != null) {
                    g.drawImage(currentFrame, 0, 0, getWidth(), getHeight(), null);
                } else {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(240, 242, 245),
                        getWidth(), getHeight(), new Color(230, 232, 235)
                    );
                    g2.setPaint(gradient);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(Color.GRAY);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    String msg = "Python Sunucusu Bekleniyor...";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(msg)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(msg, x, y);
                    g2.dispose();
                }
            }
        };
        videoPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(2, 2, 2, 2, new Color(0, 123, 255)),
            new EmptyBorder(0, 0, 0, 0)
        ));
        videoPanel.setPreferredSize(new Dimension(650, 420));
        ortaPanel.add(videoPanel, BorderLayout.CENTER);

        JPanel duyguPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        duyguPanel.setOpaque(false);
        duyguPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JLabel lblAnlikLabel = new JLabel("ANLIK DUYGU:");
        lblAnlikLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAnlikLabel.setForeground(new Color(108, 117, 125));
        duyguPanel.add(lblAnlikLabel);

        lblAnlikDuygu = new JLabel("Veri Bekleniyor...");
        lblAnlikDuygu.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAnlikDuygu.setForeground(new Color(40, 167, 69));
        duyguPanel.add(lblAnlikDuygu);

        ortaPanel.add(duyguPanel, BorderLayout.SOUTH);
        add(ortaPanel, BorderLayout.CENTER);
        add(createAltPanel(), BorderLayout.SOUTH);
    }

    private JPanel createAltPanel() {
        JPanel p = new JPanel();
        p.add(new JLabel("Masa Paneli Buraya"));
        return p;
    }

    /**
     * Python duygu sunucusundan gelen çerçeveleri okur.
     * Protokol: [JSON_SIZE:4] + [JSON] + [IMAGE_SIZE:4] + [JPEG]
     */
    private void startCameraConnection() {
        Thread connectionThread = new Thread(() -> {
            while (isRunning) {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    while (isRunning && socket.isConnected()) {
                        int jsonLen = in.readInt();
                        byte[] jsonData = new byte[jsonLen];
                        in.readFully(jsonData);
                        String jsonString = new String(jsonData, "UTF-8");

                        int imgLen = in.readInt();
                        byte[] imgData = new byte[imgLen];
                        in.readFully(imgData);

                        ByteArrayInputStream bais = new ByteArrayInputStream(imgData);
                        BufferedImage frame = ImageIO.read(bais);

                        String emotion = "Nötr";
                        if (jsonString.contains("primary_emotion")) {
                            int start = jsonString.indexOf("primary_emotion") + 18;
                            int end = jsonString.indexOf("\"", start);
                            if (start > 0 && end > 0) {
                                emotion = jsonString.substring(start, end);
                            }
                        }

                        String finalEmotion = emotion;
                        SwingUtilities.invokeLater(() -> {
                            currentFrame = frame;
                            videoPanel.repaint();
                            lblAnlikDuygu.setText(finalEmotion);

                            if (finalEmotion.contains("Mutlu")) {
                                lblAnlikDuygu.setForeground(new Color(40, 167, 69));
                            } else if (finalEmotion.contains("Kizgin")) {
                                lblAnlikDuygu.setForeground(new Color(220, 53, 69));
                            } else {
                                lblAnlikDuygu.setForeground(new Color(0, 123, 255));
                            }
                        });
                    }
                } catch (Exception e) {
                    String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    System.err.println("Duygu sunucusuna bağlanılamadı, yeniden deneniyor: " + reason);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
