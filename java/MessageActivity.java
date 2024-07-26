package org.udp.servun;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class MessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // Consider using storage instances (SQLite, SharedPreferences, EncryptedSharedPreferences etc.), especially if you're developing any app with real-world users.
        // This is a simple app developed by floatbar.

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        ImageView imageView = findViewById(R.id.imageView);
        EditText editText = findViewById(R.id.editText);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        ArrayList<String> messages = new ArrayList<>();
        MessageAdapter[] adapter = {null};

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress inetAddress = InetAddress.getByName(getString(R.string.udp_server_ipv4));
                int port = Integer.parseInt(getString(R.string.udp_server_port));

                String body = "{\"type\": \"Receive All Messages\"}";
                byte[] sendData = body.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
                socket.send(sendPacket);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                JSONArray jsonArray = new JSONObject(receivedData).getJSONArray("messages");
                for (int position = 0; position < jsonArray.length(); position++) {
                    String receivedMessage = jsonArray.getJSONObject(position).getString("message");
                    messages.add(receivedMessage);
                }

                runOnUiThread(() -> {
                    adapter[0] = new MessageAdapter(messages);
                    recyclerView.setAdapter(adapter[0]);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setHasFixedSize(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (true) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    InetAddress inetAddress = InetAddress.getByName(getString(R.string.udp_server_ipv4));
                    int port = Integer.parseInt(getString(R.string.udp_server_port));

                    String body = "{\"type\": \"Receive Single Message\"}";
                    byte[] sendData = body.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
                    socket.send(sendPacket);

                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    JSONObject jsonObject = new JSONObject(receivedData);
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        messages.add(jsonObject.getString("message"));
                        runOnUiThread(() -> {
                            adapter[0] = new MessageAdapter(messages);
                            recyclerView.setAdapter(adapter[0]);
                            recyclerView.setLayoutManager(new LinearLayoutManager(this));
                            recyclerView.setHasFixedSize(true);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        imageView.setOnClickListener(view -> {
            String message = editText.getText().toString().trim();
            if (!message.isEmpty()) {
                new Thread(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        InetAddress ipAddress = InetAddress.getByName(getString(R.string.udp_server_ipv4));
                        int PORT = Integer.parseInt(getString(R.string.udp_server_port));

                        String body = "{\"type\": \"Send Any Message\", \"message\": \"" + message + "\"}";
                        byte[] sendData = body.getBytes();

                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, PORT);
                        socket.send(sendPacket);

                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);

                        String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        JSONObject jsonObject = new JSONObject(receivedData);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) runOnUiThread(() -> editText.setText(""));
                        else runOnUiThread(() -> Toast.makeText(MessageActivity.this, getString(R.string.error_label), Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }
}
