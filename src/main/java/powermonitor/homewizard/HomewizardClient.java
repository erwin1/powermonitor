package powermonitor.homewizard;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HomewizardClient {
    public static final Logger LOGGER = Logger.getLogger(HomewizardClient.class.getName());
    private static Pattern patternId = Pattern.compile("(.*?)\\(.*");
    private static Pattern patternValue = Pattern.compile("\\((.*?)\\)");

    private static final HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private String ip;

    public HomewizardClient(String ip) {
        this.ip = ip;
    }

    public Telegram getTelegram() throws IOException, InterruptedException {
        LOGGER.fine("Requesting data from HWEP1");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+ip+"/api/v1/telegram"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "text/plain")
                .GET()
                .build();

        // 2. Send the Request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return parseTelegram(response.body());
        } else {
            LOGGER.finest("unexpected response from homewizard: "+response.statusCode()+" "+response.body());
            throw new RuntimeException("error getting hwep1 data "+response.statusCode());

        }
    }

    Telegram parseTelegram(String body) {
        Map<String, List<String>> map = body.lines().map(l -> {
            String id = null;
            List<String> values = new LinkedList<>();
            Matcher matcherId = patternId.matcher(l);
            if (matcherId.find()) {
                id = matcherId.group(1);
            }
            Matcher matcher = patternValue.matcher(l);
            while (matcher.find()) {
                String v = matcher.group(1);
                values.add(v);
            }
            return new KeyValues(id, values);
        })
        .filter(r -> r.getKey() != null)
        .collect(Collectors.toMap(r -> r.getKey(), r -> r.getValues()));

        Telegram telegram = new Telegram();
        telegram.setTimestamp(map.get("0-0:1.0.0").get(0).replaceAll("W", "").replaceAll("S", ""));
        telegram.setActive_voltage_v(new BigDecimal(parseValue(map.get("1-0:32.7.0").get(0))));
        telegram.setTotal_power_import_t1_kwh(new BigDecimal(parseValue(map.get("1-0:1.8.1").get(0))));
        telegram.setTotal_power_import_t2_kwh(new BigDecimal(parseValue(map.get("1-0:1.8.2").get(0))));
        telegram.setTotal_power_export_t1_kwh(new BigDecimal(parseValue(map.get("1-0:2.8.1").get(0))));
        telegram.setTotal_power_export_t2_kwh(new BigDecimal(parseValue(map.get("1-0:2.8.2").get(0))));
        telegram.setTotal_power_import_kwh(telegram.getTotal_power_import_t1_kwh().add(telegram.getTotal_power_import_t2_kwh()));
        telegram.setTotal_power_export_kwh(telegram.getTotal_power_export_t1_kwh().add(telegram.getTotal_power_export_t2_kwh()));
        List<String> gas = map.get("0-1:24.2.3");
        if (gas != null && gas.size() > 1) {
            telegram.setTotal_gas_m3(new BigDecimal(parseValue(gas.get(1))));
        }
        telegram.setTotal_gas_m3(new BigDecimal(parseValue(map.get("0-1:24.2.3").get(1))));
        telegram.setActive_power_average_w(new BigDecimal(parseValue(map.get("1-0:1.4.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setActive_power_import_w(new BigDecimal(parseValue(map.get("1-0:1.7.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setActive_power_export_w(new BigDecimal(parseValue(map.get("1-0:2.7.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setMontly_power_peak_timestamp(map.get("1-0:1.6.0").get(0).replaceAll("W", "").replaceAll("S", ""));
        telegram.setMontly_power_peak_w(new BigDecimal(parseValue(map.get("1-0:1.6.0").get(1))).multiply(new BigDecimal(1000)).intValue());

        return telegram;
    }

    private String parseValue(String v) {
        int x = v.indexOf("*");
        if (x > 0) {
            return v.substring(0, x);
        }
        return v;
    }

    private static class KeyValues {
        private String key;
        private List<String> values;

        public KeyValues(String key, List<String> values) {
            this.key = key;
            this.values = values;
        }

        public String getKey() {
            return key;
        }

        public List<String> getValues() {
            return values;
        }
    }

}
