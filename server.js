const express = require('express');
const mqtt = require('mqtt');
const app = express();

const mqttClient = mqtt.connect('mqtt://test.mosquitto.org'); // Using a public MQTT broker for testing

// Middleware to parse JSON requests
app.use(express.json());

// Verify User Function
function verifyUser(pin) {
    // Add logic to verify user based on the pin
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

// Handle MQTT messages (status updates, heartbeats)
mqttClient.on('message', (topic, message) => {
    if (topic === 'smartLock/heartbeat') {
        console.log(`Received heartbeat: ${message.toString()}`);
    } else {
        console.log(`Received message from ${topic}: ${message.toString()}`);
    }
});

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
