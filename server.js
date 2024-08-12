const express = require('express');
const mqtt = require('mqtt');
const sqlite3 = require('sqlite3').verbose();
const app = express();

const mqttClient = mqtt.connect('mqtt://test.mosquitto.org'); // Using a public MQTT broker for testing

// Middleware to parse JSON requests
app.use(express.json());

// Initialize SQLite database (using a file-based database for persistence)
const db = new sqlite3.Database('heartbeat_logs.db');

// Create a table for heartbeat logs if it doesn't exist
db.serialize(() => {
    db.run("CREATE TABLE IF NOT EXISTS HeartbeatLogs (timestamp TEXT, message TEXT)");
});

// Verify User Function
function verifyUser(pin) {
    return pin === '1234'; // Example pin
}

// Route to handle incoming commands
app.post('/sendCommand', (req, res) => {
    const { pin, command } = req.body;

    console.log(`Received command: ${command} with PIN: ${pin}`); // Log the received request

    if (verifyUser(pin)) {
        console.log(`Valid PIN. Publishing command: ${command}`);
        mqttClient.publish('smartLock/command', command, () => {
            console.log(`Command '${command}' published to MQTT topic 'smartLock/command'`);
        });
        res.status(200).send('Command sent successfully');
    } else {
        console.log(`Invalid PIN: ${pin}`);
        res.status(403).send('Invalid PIN');
    }
});

// Route to retrieve heartbeat logs
app.get('/heartbeatLogs', (req, res) => {
    db.all("SELECT * FROM HeartbeatLogs", [], (err, rows) => {
        if (err) {
            console.error("Error retrieving logs:", err.message);
            res.status(500).send("Error retrieving logs");
        } else {
            res.json(rows);
        }
    });
});

// Handle MQTT messages (status updates, heartbeats)
mqttClient.on('message', (topic, message) => {
    const msg = message.toString();
    if (topic === 'smartLock/heartbeat') {
        console.log(`Received heartbeat: ${msg}`);
        storeHeartbeatLog(msg);
    } else {
        console.log(`Received message from ${topic}: ${msg}`);
    }
});

// Store heartbeat log in SQLite database
function storeHeartbeatLog(message) {
    const timestamp = new Date().toISOString();
    db.run("INSERT INTO HeartbeatLogs (timestamp, message) VALUES (?, ?)", [timestamp, message], function(err) {
        if (err) {
            console.error("Error storing heartbeat log:", err.message);
        } else {
            console.log("Heartbeat log stored successfully");
        }
    });
}

// Subscribe to necessary MQTT topics
mqttClient.on('connect', () => {
    console.log('Connected to MQTT broker');
    mqttClient.subscribe('smartLock/status');
    mqttClient.subscribe('smartLock/heartbeat');
});

// Start Express server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
