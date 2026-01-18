package powermonitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private final String homewizardHost;
    private final String slackUrl;
    private final String slackChannel;
    private final int port;

    public Config(String path) throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(path)) {
            props.load(input);
        }

        this.port = Integer.parseInt(props.getProperty("port", "8080"));
        this.homewizardHost = props.getProperty("homewizard.host");
        this.slackUrl = props.getProperty("slack.url");
        this.slackChannel = props.getProperty("slack.channel");
    }

    public String getHomewizardHost() { return homewizardHost; }

    public String getSlackUrl() { return slackUrl; }

    public String getSlackChannel() { return slackChannel; }

    public int getPort() {
        return port;
    }
}