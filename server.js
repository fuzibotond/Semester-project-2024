const express = require("express");
const mqtt = require("mqtt");
const sqlite3 = require("sqlite3").verbose();
const app = express();

const mqttClient = mqtt.connect("mqtt://test.mosquitto.org");

app.use(express.json());

// Database initialization
const db = new sqlite3.Database("heartbeat_logs.db");

db.serialize(() => {
  db.run(
    "CREATE TABLE IF NOT EXISTS HeartbeatLogs (timestamp TEXT, message TEXT)"
  );
  db.run("CREATE TABLE IF NOT EXISTS StateLogs (timestamp TEXT, message TEXT)");
});

function verifyUser(pin) {
  return pin === "1234";
}

app.post("/sendCommand", (req, res) => {
  const { pin, command } = req.body;
  console.log(`Received command: ${command} with PIN: ${pin}`);
  if (verifyUser(pin)) {
    console.log(`Valid PIN. Publishing command: ${command}`);
    mqttClient.publish("smartLock/command", command, () => {
      console.log(
        `Command '${command}' published to MQTT topic 'smartLock/command'`
      );
      res.status(200).send("Command sent successfully");
    });
  } else {
    console.log(`Invalid PIN: ${pin}`);
    res.status(403).send("Invalid PIN");
  }
});

// Route to retrieve the most recent heartbeat log
app.get("/heartbeatLogs", (req, res) => {
  db.all(
    "SELECT * FROM HeartbeatLogs ORDER BY timestamp DESC LIMIT 1",
    [],
    (err, rows) => {
      if (err) {
        console.error("Error retrieving logs:", err.message);
        res.status(500).send("Error retrieving logs");
      } else {
        res.json(rows);
      }
    }
  );
});

// Route to retrieve all state logs
app.get("/stateLogs", (req, res) => {
  db.all("SELECT * FROM StateLogs", [], (err, rows) => {
    if (err) {
      console.error("Error retrieving logs:", err.message);
      res.status(500).send("Error retrieving logs");
    } else {
      res.json(rows);
    }
  });
});

mqttClient.on("message", (topic, message) => {
  if (topic === "smartLock/heartbeat") {
    storeLog("HeartbeatLogs", message.toString());
  } else if (topic === "smartLock/status") {
    storeLog("StateLogs", message.toString());
  } else {
    console.log(
      `Received message on unknown topic ${topic}: ${message.toString()}`
    );
  }
});

function storeLog(tableName, message) {
  const timestamp = new Date().toISOString();
  const messageString = JSON.stringify(message);
  db.run(
    `INSERT INTO ${tableName} (timestamp, message) VALUES (?, ?)`,
    [timestamp, messageString],
    function (err) {
      if (err) {
        console.error(`Error storing log in ${tableName}:`, err.message);
      } else {
        console.log(`${tableName} log stored successfully`);
      }
    }
  );
}

mqttClient.on("connect", () => {
  console.log("Connected to MQTT broker");
  mqttClient.subscribe("smartLock/status");
  mqttClient.subscribe("smartLock/heartbeat");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
