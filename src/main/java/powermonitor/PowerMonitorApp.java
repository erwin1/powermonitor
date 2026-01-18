package powermonitor;

import java.io.IOException;
import java.util.logging.Logger;

import powermonitor.homewizard.HomewizardClient;

public class PowerMonitorApp {

    private static final Logger logger = Logger.getLogger(PowerMonitorApp.class.getName());

    private static Server server;
    private static HomewizardClient homewizardClient;
    private static PowerPeakService powerPeakService;
    private static SlackService slackService;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -jar powermonitor.jar <path/to/config.properties>\n" +
                    "\n" +
                    "config.properties must contain\n" +
                    "homewizard.host=\n" +
                    "slack.url=\n" +
                    "slack.channel=\n" +
                    "port=(optional, default 8080)");
            System.exit(1);
        }

        Config config = new Config(args[0]);

        homewizardClient = new HomewizardClient(config.getHomewizardHost());
        slackService = new SlackService(config.getSlackUrl(), config.getSlackChannel());

        powerPeakService = new PowerPeakService(homewizardClient, slackService);
        powerPeakService.start();

        server = new Server(homewizardClient, powerPeakService);
        server.start(config.getPort());

        logger.info("PowerMonitorApp started successfully.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping server...");
            server.stop();
            powerPeakService.stop();
        }));
    }
}