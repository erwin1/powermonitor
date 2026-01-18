# Power Monitor

A lightweight server that monitors power consumption data from a Homewizard device. It groups statistics in a dashboard and sends Slack notifications on peak usage.

## Zero Dependency Approach

This project is built with a "zero dependency" philosophy. It does not require any external libraries for its core functionality and relies only on the standard JDK. This approach makes the application lightweight, easy to build, and simple to maintain.

## Requirements

*   Java 25
*   Maven

## How to Run

1.  **Build the project:**
    ```bash
    mvn clean install
    ```

2.  **Run the application:**
    ```bash
    java -cp target/powermonitor-1.0-SNAPSHOT.jar powermonitor.PowerMonitorApp /path/to/config.properties
    ```

## `config.properties`

The `config.properties` file contains the following settings:

*   `homewizard.host`: The IP address or hostname of your Homewizard device.
*   `slack.url`: The webhook URL for Slack notifications.
*   `slack.channel`: The Slack channel where notifications will be sent.
*   `port` (optional): The port on which the server will run. Defaults to `8080`.

Example:
```properties
homewizard.host=192.168.1.10
slack.url=https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
slack.channel=#power-alerts
port=8080
```
