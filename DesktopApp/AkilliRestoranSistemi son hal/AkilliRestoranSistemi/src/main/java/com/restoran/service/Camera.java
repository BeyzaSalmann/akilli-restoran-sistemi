package com.restoran.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.restoran.util.Env;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Camera {

    private static final String API_URL = Env.get("CAMERA_API_URL", "http://localhost:5000/predict");
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static Process pythonServerProcess;

    public static void main(String[] args) {
        try {
            startPythonServer();
            Thread.sleep(10000);
        } catch (IOException | InterruptedException e) {
            System.err.println("Python sunucusu başlatılamadı: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Python sunucusu başlatılamadı.\n"
                    + "Python ve gerekli kütüphanelerin (tensorflow, flask, opencv) kurulu olduğundan emin olun.",
                "Başlatma Hatası", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("Kamera bulunamadı.");
            return;
        }
        webcam.setViewSize(new Dimension(640, 480));

        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setMirrored(true);

        JFrame window = new JFrame("Java Mutluluk Analizi");
        window.add(panel);
        window.setResizable(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        new Thread(() -> startAnalysisLoop(webcam)).start();
    }

    private static void startPythonServer() throws IOException {
        File pythonDir = new File("python_server");
        String scriptPath = new File(pythonDir, "api_sunucusu.py").getAbsolutePath();
        String pythonCommand = Env.get("PYTHON_COMMAND", "python");

        ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptPath);
        pb.directory(pythonDir);
        pb.redirectErrorStream(true);

        pythonServerProcess = pb.start();

        StreamGobbler streamGobbler = new StreamGobbler(
            pythonServerProcess.getInputStream(), System.out::println
        );
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonServerProcess != null) {
                pythonServerProcess.destroy();
            }
        }));
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

    private static void startAnalysisLoop(Webcam webcam) {
        while (true) {
            try {
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    continue;
                }
                String base64Image = encodeImageToBase64(image);
                JSONObject payload = new JSONObject();
                payload.put("image", base64Image);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("API hatası: " + response.body());
                }

                Thread.sleep(100);
            } catch (Exception e) {
                if (e instanceof java.net.http.HttpConnectTimeoutException || e instanceof java.net.ConnectException) {
                    System.err.println("Analiz sunucusuna bağlanılamadı, yeniden denenecek.");
                } else {
                    System.err.println("Analiz hatası: " + e.getMessage());
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
