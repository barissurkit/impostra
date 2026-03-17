package com.impostra.client;

import com.esotericsoftware.kryonet.Client;
import com.impostra.common.JoinRequest;
import java.io.IOException;

public class NetworkClient {
    private Client client;
    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;
    private final String SERVER_IP = "127.0.0.1";

    public NetworkClient() {
        client = new Client();
        client.start();
        client.getKryo().register(JoinRequest.class);

        try {
            client.connect(5000, SERVER_IP, TCP_PORT, UDP_PORT);
            System.out.println("Sunucuya bağlandık!");

            JoinRequest request = new JoinRequest();
            request.username = "Ali Babo";
            client.sendTCP(request);

        } catch (IOException e) {
            System.out.println("Bağlantı hatası: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new NetworkClient();
    }
}