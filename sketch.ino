#include <WiFi.h>
#include <PubSubClient.h>
#include <ESP32Servo.h>

// Wi-Fi credentials
const char *ssid = "Wokwi-GUEST";
const char *password = "";

// MQTT broker details
const char *mqttServer = "test.mosquitto.org";
const int mqttPort = 1883;

WiFiClient espClient;
PubSubClient client(espClient);

const int ledPin = 2;        // GPIO pin for the LED (Door sensor indicator)
const int doorSensorPin = 4; // GPIO pin for the push button (simulating reed switch)
const int servoPin = 5;      // GPIO pin for the servo (Lock/Unlock control)

Servo lockServo;

bool isLocked = false; // Initially, the system is unlocked
String clientId;       // Declare a global variable for the client ID

bool previousDoorState = HIGH; // Initially assume the door is open

unsigned long lastStatusSent = 0;           // Timer for the status message
const unsigned long statusInterval = 10000; // 10 seconds interval

void setup()
{
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  pinMode(doorSensorPin, INPUT_PULLUP); // Configure the pushbutton as an input with a pull-up resistor
  lockServo.attach(servoPin);

  // Start with the door unlocked
  lockServo.write(90); // Servo at 90 degrees (unlocked position)

  setup_wifi();
  setup_mqtt();

  Serial.println("System Initialized in UNLOCKED state. Waiting for input...");
}

void loop()
{
  if (!client.connected())
  {
    Serial.println("Client is not connected!!!");
    reconnect_mqtt(); // Attempt to reconnect if not connected
  }

  client.loop();

  // Check the door sensor (pushbutton) state and update the LED
  bool isDoorClosed = digitalRead(doorSensorPin) == LOW; // LOW means the door is closed (button pressed)

  if (isDoorClosed != previousDoorState)
  {
    // Door state has changed, update the LED and send a heartbeat
    if (isDoorClosed)
    {
      digitalWrite(ledPin, HIGH); // Turn the LED on if the door is closed
      Serial.println("Door is closed. LED is ON.");
    }
    else
    {
      digitalWrite(ledPin, LOW); // Turn the LED off if the door is open
      Serial.println("Door is open. LED is OFF.");
    }

    // Send the updated door state and lock state as a heartbeat
    send_status(isDoorClosed ? "false" : "true", isLocked ? "false" : "true");

    // Check for alert condition
    check_for_alert(isDoorClosed);

    // Update the previous door state
    previousDoorState = isDoorClosed;
  }

  // Check if 10 seconds have passed since the last status message
  if (millis() - lastStatusSent >= statusInterval)
  {
    send_heartbeat();
    lastStatusSent = millis(); // Reset the timer
  }
}

void setup_wifi()
{
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println("WiFi connected");
}

void setup_mqtt()
{
  client.setServer(mqttServer, mqttPort);
  client.setCallback(callback);

  Serial.println("Connecting to MQTT...");

  // Generate a unique client ID based on the ESP32's MAC address
  clientId = "ESP32Client-";
  clientId += String(WiFi.macAddress());

  // Attempt to connect with the unique client ID
  if (client.connect(clientId.c_str()))
  {
    Serial.println("Connected to MQTT broker!");
    client.subscribe("smartLock/command");
  }
  else
  {
    Serial.print("Failed to connect to MQTT broker. State: ");
    Serial.println(client.state());
  }
}

void send_alert(String alertMsg)
{
  if (client.connected())
  {
    client.publish("smartLock/alert", alertMsg.c_str());
    Serial.println("ALERT sent: " + alertMsg);
  }
}

void unlockDoor()
{
  if (isLocked)
  {
    lockServo.write(90); // Move the servo to the unlock position
    isLocked = false;
    Serial.println("Unlocking the door...");
    send_status(previousDoorState == LOW ? "false" : "true", "true");
  }
}

void lockDoor()
{
  if (!isLocked)
  {
    lockServo.write(0); // Move the servo to the lock position
    isLocked = true;
    Serial.println("Locking the door...");
    send_status(previousDoorState == LOW ? "false" : "true", "false");
  }
}

void callback(char *topic, byte *payload, unsigned int length)
{
  String command = "";
  for (int i = 0; i < length; i++)
  {
    command += (char)payload[i];
  }
  Serial.print("Message received: ");
  Serial.println(command);

  if (command == "unlock")
  {
    unlockDoor();
  }
  else if (command == "lock")
  {
    lockDoor();
  }
  delay(10);
  // Check for alert condition after receiving command
  check_for_alert(digitalRead(doorSensorPin) == LOW);
}

void check_for_alert(bool isDoorClosed)
{
  // If the door is open but the lock is engaged, send an alert
  if (!isDoorClosed && isLocked)
  {
    String alertMsg = "ALERT: Door is open while lock is engaged!";
    send_alert(alertMsg);
  }
}

void reconnect_mqtt()
{
  static unsigned long lastReconnectAttempt = 0;
  unsigned long now = millis();

  // Only attempt to reconnect if enough time has passed since the last attempt
  if (now - lastReconnectAttempt > 5000)
  { // 5 seconds
    lastReconnectAttempt = now;

    Serial.print("Attempting MQTT connection...");
    if (client.connect(clientId.c_str()))
    { // Use the unique client ID
      Serial.println("Connected");
      client.subscribe("smartLock/command");
      // Reset the reconnect attempt time
      lastReconnectAttempt = 0;
    }
    else
    {
      Serial.print("Failed to connect. State: ");
      Serial.println(client.state());
      // Delay before the next reconnect attempt
      delay(5000);
    }
  }
}

void send_status(String doorStatus, String lockStatus)
{
  if (client.connected())
  {
    // Prepare the status message with door and lock status
    String statusMsg = "{door:" + doorStatus + ", lock:" + lockStatus + "}";

    client.publish("smartLock/status", statusMsg.c_str());
    Serial.println("status sent: " + statusMsg);
  }
}

void send_heartbeat()
{
  if (client.connected())
  {
    // Send "status: OK" to the "smartLock/heartbeat" topic
    client.publish("smartLock/heartbeat", "{status:OK}");
    Serial.println("Heartbeat message sent: {status: OK}");
  }
}
