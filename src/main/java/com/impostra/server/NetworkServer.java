package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.JoinRequest;
import java.io.IOException;

public class NetworkServer {
    private Server server;
    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;

    public NetworkServer() {
        server = new Server();
        server.start();

        // Kargo paketini sunucuya tanıtıyoruz
        server.getKryo().register(JoinRequest.class);

        // Gelen mesajları havada yakalayan dinleyici
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                // Eğer gelen obje bir JoinRequest paketiyse içini aç:
                if (object instanceof JoinRequest) {
                    JoinRequest request = (JoinRequest) object;
                    System.out.println(">>> SİSTEM MESAJI: Sunucuya bir paket düştü!");
                    System.out.println(">>> GÖNDEREN KİŞİ: " + request.username);
                }
            }
        });

        try {
            server.bind(TCP_PORT, UDP_PORT);
            System.out.println("Sunucu dinliyor...");
        } catch (IOException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new NetworkServer();
    }
}