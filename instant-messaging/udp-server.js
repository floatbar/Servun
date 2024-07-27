// You had better consider implementing authentication and also encryption mechanisms before any message operation, especially if you're developing an app that has real-world users.
// This is a fast UDP server developed by floatbar.

const dgram = require("node:dgram");
const mysql2 = require("mysql2");
const udpServer = dgram.createSocket("udp4");
require("dotenv").config();

const udpClients = [];

udpServer.bind(process.env.UDP_SERVER_PORT, process.env.HOST);

udpServer.on("error", (err) => {
    console.error(err);
    udpServer.close();
});

udpServer.on("close", () => {
    for (let index = 0; index < udpClients.length; index++) udpClients.splice(index);
});

udpServer.on("listening", () => console.log("UDP server is running..."));

udpServer.on("message", (data, socket) => {
    udpClients.push(socket);
    const message = JSON.parse(data.toString());
    if (message.type === "Send Any Message") {
        const messagePlainText = message.message
        connection.query("INSERT INTO udp_messages (message) VALUES (?)", [messagePlainText], (err, results) => {
            var messageToBeSent;
            if (!err) {
                messageToBeSent = JSON.stringify({ success: true, message: messagePlainText });
                udpClients.forEach((udpClient) => udpServer.send(messageToBeSent, 0, messageToBeSent.length, udpClient.port, udpClient.address));
            }
            else {
                messageToBeSent = JSON.stringify({ success: false });
                udpServer.send(messageToBeSent, 0, messageToBeSent.length, socket.port, socket.address);
            }
        });
    }
    else if (message.type === "Receive All Messages") {
        connection.query("SELECT message FROM udp_messages", [], (err, results) => {
            if (!err && results.length > 0) {
                const messageToBeSent = JSON.stringify({ messages: results });
                udpServer.send(messageToBeSent, 0, messageToBeSent.length, socket.port, socket.address);
            }
        });
    }
});

const connection = mysql2.createPool({
    host: process.env.HOST,
    port: process.env.DATABASE_PORT,
    user: process.env.USER,
    password: process.env.PASSWORD,
    database: process.env.DATABASE
});

connection.getConnection((err) => {
    if (!err) console.log("MySQL server is running...");
});
