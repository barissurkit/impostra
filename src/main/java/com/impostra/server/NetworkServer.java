package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.JoinRequest;
import com.impostra.common.Player;
import java.io.IOException;

public class NetworkServer {
    private Server server;
    private GameManager gameManager;

    private final int TCP_PORT = 54555;
    private final int UDP_PORT = 54777;

    public NetworkServer() {
        server = new Server();
        gameManager = new GameManager();
        server.start();

        server.getKryo().register(JoinRequest.class);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof JoinRequest) {
                    JoinRequest request = (JoinRequest) object;
                    System.out.println(">>> AĞ MESAJI: " + request.username + " bağlanmak istiyor...");

                    Player yeniOyuncu = new Player(request.username);
                    // gameManager'a ekleme yapacağız ama Barış'ın kodu nasıl bilmiyoruz.
                    // Şimdilik sadece mesajı ekrana basalım:
                    System.out.println(">>> SİBER LOBİYE GİRİŞ: " + yeniOyuncu.getUsername());
                }
            }
        });

        try {
            server.bind(TCP_PORT, UDP_PORT);
            System.out.println("Siber Sunucu dinliyor...");
        } catch (IOException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new NetworkServer();
    }
}