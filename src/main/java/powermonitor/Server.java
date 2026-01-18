package powermonitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import powermonitor.homewizard.HomewizardClient;
import powermonitor.homewizard.Telegram;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public Server(HomewizardClient homewizardClient, PowerPeakService powerPeakService) {
        this.homewizardClient = homewizardClient;
        this.powerPeakService = powerPeakService;
    }

    HttpServer server;
    HomewizardClient homewizardClient;
    PowerPeakService powerPeakService;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoints
        server.createContext("/metrics/live", new LiveMetricsHandler());

        var staticHandler = com.sun.net.httpserver.SimpleFileServer.createFileHandler(
                java.nio.file.Path.of("./src/main/resources/www").toAbsolutePath()
        );
        server.createContext("/", staticHandler);

        server.setExecutor(Executors.newFixedThreadPool(5));

        server.start();
        LOGGER.info("Server started on port "+port);
    }

    public void stop() {
        server.stop(0);
    }

    class LiveMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            try {
                Telegram telegram = homewizardClient.getTelegram();

                LiveMetrics metrics = new LiveMetrics();
                metrics.setTimestamp(ZonedDateTime.now());
                metrics.setPeakEstimateW(new BigDecimal(powerPeakService.estimatePeakInCurrentPeriod(telegram)));

                if (telegram.getActive_power_import_w() > 0) {
                    metrics.setImportW(new BigDecimal(telegram.getActive_power_import_w()));
                }
                if (telegram.getActive_power_export_w() > 0) {
                    metrics.setExportW(new BigDecimal(telegram.getActive_power_export_w()));
                }
                metrics.setMonthlyPowerPeakW(new BigDecimal(telegram.getMontly_power_peak_w()));
                metrics.setMonthlyPowerPeakTimestamp(telegram.getMontlyPowerPeakTimestampAsDate());
                metrics.setImportAverageW(new BigDecimal(telegram.getActive_power_average_w()));

                String jsonResponse = toJson(metrics);
                sendResponse(exchange, 200, jsonResponse);
            } catch (Exception e) {
                sendResponse(exchange, 503, e.toString());
            }

        }

        private String toJson(LiveMetrics metrics) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            if (metrics.getTimestamp() != null) {
                sb.append("\"timestamp\":\"").append(formatter.format(metrics.getTimestamp())).append("\",");
            }
            if (metrics.getImportW() != null) {
                sb.append("\"importW\":").append(metrics.getImportW()).append(",");
            }
            if (metrics.getImportAverageW() != null) {
                sb.append("\"importAverageW\":").append(metrics.getImportAverageW()).append(",");
            }
            if (metrics.getExportW() != null) {
                sb.append("\"exportW\":").append(metrics.getExportW()).append(",");
            }
            if (metrics.getPeakEstimateW() != null) {
                sb.append("\"peakEstimateW\":").append(metrics.getPeakEstimateW()).append(",");
            }
            if (metrics.getMonthlyPowerPeakW() != null) {
                sb.append("\"monthlyPowerPeakW\":").append(metrics.getMonthlyPowerPeakW()).append(",");
            }
            if (metrics.getMonthlyPowerPeakTimestamp() != null) {
                sb.append("\"monthlyPowerPeakTimestamp\":\"").append(formatter.format(metrics.getMonthlyPowerPeakTimestamp())).append("\",");
            }

            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }

            sb.append("}");
            return sb.toString();
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] bytes = response.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
