package com.impostra.client.ui;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.impostra.common.Network;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ImpostraGUI extends Application {

    // ============================================================
    //  RENK PALETİ
    // ============================================================
    private static final Color NEON_CYAN    = Color.web("#00FBFF");
    private static final Color NEON_PURPLE  = Color.web("#B14EFF");
    private static final Color NEON_GOLD    = Color.web("#FFD700");
    private static final Color NEON_PINK    = Color.web("#FF2E93");
    private static final Color NEON_RED     = Color.web("#FF0055");
    private static final Color NEON_GREEN   = Color.web("#00FF9C");
    private static final Color BG_DEEP      = Color.web("#0a0a0c");
    private static final Color BG_PANEL     = Color.web("#12121a");

    private static final Color COLOR_SELF       = NEON_CYAN;
    private static final Color COLOR_NEUTRAL    = Color.web("#c0d8e8");
    private static final Color COLOR_ALLY_EVIL  = Color.web("#ff3366");

    // Atmosferik renkler
    private static final Color NIGHT_TOP = Color.web("#0f0720"), NIGHT_MID = Color.web("#16213e"), NIGHT_BOT = Color.web("#0a0a12");
    private static final Color DAY_TOP = Color.web("#2980b9"), DAY_MID = Color.web("#74b9ff"), DAY_BOT = Color.web("#a8e6cf");
    private static final Color GROUND_NIGHT = Color.web("#0d1f0d"), GROUND_DAY = Color.web("#3da35d");
    private static final Color TREE_NIGHT = Color.web("#081208"), TREE_DAY = Color.web("#1a6b35");

    private static final String FONT_MONO = "Consolas";

    // ============================================================
    //  SAHNE / AĞ
    // ============================================================
    private Stage primaryStage;
    private Scene currentScene;
    private Client client;

    // ============================================================
    //  LOBİ
    // ============================================================
    private FlowPane lobbyAvatarFlow;
    private Label    lobbyCounterLabel;
    private Button   readyButton;
    private boolean  amReady = false;

    // ============================================================
    //  OYUN DURUMU
    // ============================================================
    private String   myUsername     = "";
    private String[] currentPlayers;
    private String   myRole         = "";
    private boolean  amIEvil        = false;
    private String[] evilTeammates  = null;

    // ============================================================
    //  CACHE
    // ============================================================
    private Image bgPattern, penguinAvatar;
    private Label statusLabel;

    // ================================================================
    //  GİRİŞ NOKTASI
    // ================================================================
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(true);
        loadAssets();
        showLoginScreen();
        primaryStage.setTitle("Impostra :: Digital Shift");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void loadAssets() {
        try { bgPattern     = new Image(getClass().getResourceAsStream("/assets/square.png"));        } catch (Exception ignored) { bgPattern = null; }
        try { penguinAvatar = new Image(getClass().getResourceAsStream("/assets/penguin_agent.png")); } catch (Exception ignored) { penguinAvatar = null; }
    }

    public static void main(String[] args) { launch(args); }

    // ================================================================
    //  ATMOSFERİK ARKA PLANLAR
    // ================================================================

    private StackPane createThemedBackground() {
        StackPane root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(BG_DEEP, CornerRadii.EMPTY, Insets.EMPTY)));
        if (bgPattern != null) {
            ImageView pv = new ImageView(bgPattern); pv.setOpacity(0.06); pv.setPreserveRatio(false);
            pv.fitWidthProperty().bind(root.widthProperty()); pv.fitHeightProperty().bind(root.heightProperty());
            pv.setMouseTransparent(true); root.getChildren().add(pv);
        }
        return root;
    }

    private StackPane createNightBackground() {
        StackPane root = new StackPane(); double W = 1024, H = 768;
        Pane sc = new Pane(); sc.setPrefSize(W, H); sc.setMouseTransparent(true);
        Rectangle sky = new Rectangle(0, 0, W, H);
        sky.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE, new Stop(0,NIGHT_TOP), new Stop(0.5,NIGHT_MID), new Stop(1,NIGHT_BOT)));
        sc.getChildren().add(sky);
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 90; i++) {
            Circle star = new Circle(rng.nextDouble()*W, rng.nextDouble()*H*0.6, 0.6+rng.nextDouble()*1.8, Color.WHITE);
            star.setOpacity(0.4+rng.nextDouble()*0.6);
            FadeTransition tw = new FadeTransition(Duration.seconds(1.2+rng.nextDouble()*3), star);
            tw.setFromValue(star.getOpacity()); tw.setToValue(0.1); tw.setCycleCount(Animation.INDEFINITE); tw.setAutoReverse(true); tw.play();
            sc.getChildren().add(star);
        }
        Circle moon = new Circle(880,120,44,Color.web("#f5f3e0"));
        DropShadow mg = new DropShadow(50, Color.web("#c8d0ff")); mg.setSpread(0.3); moon.setEffect(mg);
        sc.getChildren().addAll(moon, new Circle(870,110,5,Color.web("#dddcc8")), new Circle(896,135,4,Color.web("#dddcc8")), new Circle(884,128,3,Color.web("#dddcc8")));
        Rectangle mist = new Rectangle(0,H*0.55,W,H*0.2);
        mist.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE, new Stop(0,Color.TRANSPARENT), new Stop(0.5,Color.web("#1a2340",0.3)), new Stop(1,Color.TRANSPARENT)));
        sc.getChildren().add(mist);
        Polygon fh = new Polygon(0,H*0.76,120,H*0.68,280,H*0.72,460,H*0.65,650,H*0.70,820,H*0.67,W,H*0.73,W,H,0,H);
        fh.setFill(Color.web("#0c1420")); fh.setOpacity(0.8); sc.getChildren().add(fh);
        Polygon nh = new Polygon(0,H*0.82,200,H*0.76,400,H*0.80,600,H*0.74,800,H*0.78,W,H*0.80,W,H,0,H);
        nh.setFill(Color.web("#0a1610")); sc.getChildren().add(nh);
        for (double tx : new double[]{30,100,170,240,310,720,790,860,940,1000})
            sc.getChildren().add(buildTree(tx, H*0.80, TREE_NIGHT, 0.85+rng.nextDouble()*0.4));
        sc.getChildren().add(new Rectangle(0,H*0.92,W,H*0.08) {{ setFill(GROUND_NIGHT); }});
        sc.getChildren().add(new Rectangle(0,H*0.85,W,H*0.08) {{ setFill(Color.web("#101830",0.35)); }});
        root.getChildren().add(sc); return root;
    }

    private StackPane createDayBackground() {
        StackPane root = new StackPane(); double W = 1024, H = 768;
        Pane sc = new Pane(); sc.setPrefSize(W, H); sc.setMouseTransparent(true);
        Rectangle sky = new Rectangle(0,0,W,H);
        sky.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE, new Stop(0,DAY_TOP), new Stop(0.5,DAY_MID), new Stop(0.85,DAY_BOT)));
        sc.getChildren().add(sky);
        Circle sun = new Circle(880,120,52,Color.web("#fff7b0"));
        DropShadow sg = new DropShadow(65,Color.web("#ffe066")); sg.setSpread(0.45); sun.setEffect(sg);
        sc.getChildren().add(sun);
        ScaleTransition sp = new ScaleTransition(Duration.seconds(3),sun);
        sp.setFromX(1);sp.setFromY(1);sp.setToX(1.06);sp.setToY(1.06);sp.setCycleCount(Animation.INDEFINITE);sp.setAutoReverse(true);sp.play();
        for (int i=1;i<=3;i++){Circle r=new Circle(880,120,52+i*22);r.setFill(Color.TRANSPARENT);r.setStroke(Color.web("#fff7b0",0.12-i*0.03));r.setStrokeWidth(1.5);sc.getChildren().add(r);}
        sc.getChildren().addAll(buildCloud(100,100,1.0),buildCloud(350,70,0.85),buildCloud(600,130,1.15),buildCloud(160,210,0.65),buildCloud(820,90,0.9));
        Polygon fh = new Polygon(0,H*0.70,160,H*0.63,320,H*0.67,500,H*0.60,680,H*0.65,860,H*0.62,W,H*0.68,W,H,0,H);
        fh.setFill(Color.web("#6aad8a"));fh.setOpacity(0.5);sc.getChildren().add(fh);
        Polygon nh2 = new Polygon(0,H*0.80,200,H*0.74,400,H*0.78,600,H*0.72,800,H*0.77,W,H*0.79,W,H,0,H);
        nh2.setFill(Color.web("#4a8a52"));sc.getChildren().add(nh2);
        java.util.Random rng = new java.util.Random(99);
        for (double tx:new double[]{40,120,200,280,360,700,780,860,940,1010})
            sc.getChildren().add(buildTree(tx,H*0.82,TREE_DAY,0.85+rng.nextDouble()*0.4));
        for(int i=0;i<25;i++){double fx=rng.nextDouble()*W,fy=H*0.88+rng.nextDouble()*(H*0.05);
            sc.getChildren().add(new Circle(fx,fy,2.5,rng.nextBoolean()?Color.web("#ff6b9d"):Color.web("#fff36e")){{setOpacity(0.8);}});}
        sc.getChildren().add(new Rectangle(0,H*0.92,W,H*0.08){{setFill(GROUND_DAY);}});
        root.getChildren().add(sc); return root;
    }

    private Pane buildTree(double bx, double by, Color lc, double s) {
        Pane t = new Pane(); t.setMouseTransparent(true);
        double tw=8*s,th=30*s,cw=48*s,ch=70*s;
        Rectangle trunk = new Rectangle(bx-tw/2,by-th,tw,th); trunk.setFill(Color.web("#3d2817"));
        Polygon c1=new Polygon(bx-cw/2,by-th,bx+cw/2,by-th,bx,by-th-ch*0.45);
        Polygon c2=new Polygon(bx-cw*0.4,by-th-ch*0.25,bx+cw*0.4,by-th-ch*0.25,bx,by-th-ch*0.7);
        Polygon c3=new Polygon(bx-cw*0.3,by-th-ch*0.5,bx+cw*0.3,by-th-ch*0.5,bx,by-th-ch);
        c1.setFill(lc);c2.setFill(lc);c3.setFill(lc);
        t.getChildren().addAll(trunk,c1,c2,c3); return t;
    }

    private Pane buildCloud(double cx, double cy, double s) {
        Pane c = new Pane(); c.setMouseTransparent(true);
        Color cc = Color.web("#ffffff",0.7);
        Circle c1=new Circle(cx-28*s,cy,24*s,cc),c2=new Circle(cx,cy-10*s,30*s,cc),c3=new Circle(cx+28*s,cy,26*s,cc);
        c2.setEffect(new DropShadow(12,Color.web("#ffffff",0.3)));
        c.getChildren().addAll(c1,c2,c3); return c;
    }

    // ================================================================
    //  UI FACTORY
    // ================================================================

    private VBox createGlassPanel(Color accent) {
        VBox p = new VBox(); p.setPadding(new Insets(30)); p.setSpacing(18); p.setAlignment(Pos.CENTER);
        p.setStyle("-fx-background-color:rgba(10,10,18,0.82);-fx-background-radius:18;-fx-border-color:"+toHex(accent)+";-fx-border-width:1.5;-fx-border-radius:18;");
        DropShadow g = new DropShadow(30,accent); g.setSpread(0.06); p.setEffect(g); return p;
    }

    private Label createNeonTitle(String text, Color color, double size) {
        Label l = new Label(text); l.setTextFill(color); l.setFont(Font.font(FONT_MONO,FontWeight.BOLD,size));
        DropShadow g = new DropShadow(22,color); g.setSpread(0.35); l.setEffect(g); return l;
    }

    private Button createNeonButton(String text, Color base, Color hover) {
        Button b = new Button(text); b.setMinHeight(50); b.setMinWidth(280); b.setCursor(Cursor.HAND);
        b.setFont(Font.font(FONT_MONO,FontWeight.BOLD,16));
        String bh=toHex(base),hh=toHex(hover);
        String idle="-fx-background-color:transparent;-fx-text-fill:"+bh+";-fx-border-color:"+bh+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;";
        String hot="-fx-background-color:"+hh+";-fx-text-fill:#0a0a0c;-fx-border-color:"+hh+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;";
        b.setStyle(idle); DropShadow glow = new DropShadow(15,base); b.setEffect(glow);
        ScaleTransition su=new ScaleTransition(Duration.millis(120),b), sd=new ScaleTransition(Duration.millis(100),b);
        b.setOnMouseEntered(e->{b.setStyle(hot);glow.setColor(hover);glow.setRadius(28);su.setToX(1.05);su.setToY(1.05);su.playFromStart();});
        b.setOnMouseExited(e->{b.setStyle(idle);glow.setColor(base);glow.setRadius(15);su.setToX(1.0);su.setToY(1.0);su.playFromStart();});
        b.setOnMousePressed(e->{sd.setToX(0.96);sd.setToY(0.96);sd.playFromStart();});
        b.setOnMouseReleased(e->{sd.setToX(1.0);sd.setToY(1.0);sd.playFromStart();}); return b;
    }

    private TextField createNeonField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.setMinHeight(48); tf.setMinWidth(300);
        tf.setFont(Font.font(FONT_MONO,15));
        tf.setStyle("-fx-background-color:rgba(0,0,0,0.55);-fx-text-fill:#00FBFF;-fx-prompt-text-fill:#4d6e70;-fx-border-color:#00FBFF;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:0 14;");
        tf.setEffect(new DropShadow(10,NEON_CYAN)); return tf;
    }

    private VBox createAvatarCard(String username, Color glowColor, boolean showHalo, boolean isAlly) {
        VBox card = new VBox(6); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(6));
        StackPane holder = new StackPane(); holder.setMinSize(92,92); holder.setMaxSize(92,92);
        Circle halo = new Circle(46); halo.setFill(Color.TRANSPARENT); halo.setStroke(glowColor);
        halo.setStrokeWidth(showHalo?3.5:1.2); halo.setOpacity(showHalo?1.0:0.45);
        DropShadow hg = new DropShadow(showHalo?28:10,glowColor); hg.setSpread(showHalo?0.15:0); halo.setEffect(hg);
        Node av;
        if (penguinAvatar != null) { ImageView iv = new ImageView(penguinAvatar); iv.setFitWidth(68); iv.setFitHeight(68); iv.setPreserveRatio(true); av = iv; }
        else { Circle fb = new Circle(33,Color.web("#141420",0.9)); fb.setStroke(glowColor); fb.setStrokeWidth(2); av = fb; }
        holder.getChildren().addAll(halo, av);
        Label name = new Label(username); name.setTextFill(glowColor); name.setFont(Font.font(FONT_MONO,FontWeight.BOLD,13));
        DropShadow ns = new DropShadow(5,Color.BLACK); ns.setSpread(0.7); name.setEffect(ns);
        card.getChildren().addAll(holder, name);
        if (isAlly) {
            Rectangle ind = new Rectangle(40,3); ind.setFill(COLOR_ALLY_EVIL); ind.setArcWidth(6); ind.setArcHeight(6); ind.setOpacity(0.8);
            ind.setEffect(new DropShadow(8,COLOR_ALLY_EVIL){{setSpread(0.3);}}); card.getChildren().add(ind);
        }
        return card;
    }

    private VBox createAvatarCard(String username, Color glowColor, boolean showHalo) {
        return createAvatarCard(username, glowColor, showHalo, false);
    }

    /**
     * LOBİ AVATAR KARTI — hazır durumunu gösteren versiyon.
     * Hazır → yeşil parlak halka + ✓ etiketi, Değil → gri soluk halka + "..." etiketi
     */
    private VBox createLobbyAvatarCard(String username, boolean isReady) {
        Color color = isReady ? NEON_GREEN : Color.web("#606070");
        VBox card = new VBox(6); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(6));

        StackPane holder = new StackPane(); holder.setMinSize(88,88); holder.setMaxSize(88,88);
        Circle halo = new Circle(44); halo.setFill(Color.TRANSPARENT); halo.setStroke(color);
        halo.setStrokeWidth(isReady ? 3 : 1.2); halo.setOpacity(isReady ? 1.0 : 0.4);
        if (isReady) { DropShadow hg = new DropShadow(20, NEON_GREEN); hg.setSpread(0.15); halo.setEffect(hg); }

        Node av;
        if (penguinAvatar != null) { ImageView iv = new ImageView(penguinAvatar); iv.setFitWidth(64); iv.setFitHeight(64); iv.setPreserveRatio(true); av = iv; }
        else { Circle fb = new Circle(30, Color.web("#141420",0.9)); fb.setStroke(color); fb.setStrokeWidth(2); av = fb; }
        holder.getChildren().addAll(halo, av);

        Label name = new Label(username); name.setTextFill(color); name.setFont(Font.font(FONT_MONO,FontWeight.BOLD,12));
        Label status = new Label(isReady ? "✓ HAZIR" : "bekleniyor...");
        status.setTextFill(isReady ? NEON_GREEN : Color.web("#505060")); status.setFont(Font.font(FONT_MONO, 10));
        card.getChildren().addAll(holder, name, status);
        return card;
    }

    private String toHex(Color c) { return String.format("#%02X%02X%02X",(int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255)); }

    private void switchScene(Scene s) {
        primaryStage.setScene(s); Node r = s.getRoot(); r.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(500),r); f.setFromValue(0); f.setToValue(1); f.play(); currentScene = s;
    }

    // ================================================================
    //  EKRAN 1: LOGIN
    // ================================================================
    public void showLoginScreen() {
        StackPane root = createThemedBackground();
        VBox panel = createGlassPanel(NEON_CYAN); panel.setMaxWidth(460); panel.setMaxHeight(560); panel.setSpacing(22);
        Label title = createNeonTitle("IMPOSTRA", NEON_CYAN, 72);
        Label sub = new Label("> DIGITAL SHIFT // ACCESS TERMINAL"); sub.setTextFill(NEON_PURPLE); sub.setFont(Font.font(FONT_MONO,14));
        Rectangle sep = new Rectangle(280,1); sep.setFill(NEON_CYAN); sep.setOpacity(0.4);
        TextField nameField = createNeonField("ACCESS_CODE  ::  type your nickname");
        Button connectBtn = createNeonButton("» ENTER THE GRID «", NEON_CYAN, NEON_PINK);
        statusLabel = new Label("waiting for authorization..."); statusLabel.setTextFill(Color.web("#7a8a8c")); statusLabel.setFont(Font.font(FONT_MONO,12));
        panel.getChildren().addAll(title,sub,sep,nameField,connectBtn,statusLabel);
        root.getChildren().add(panel); StackPane.setAlignment(panel,Pos.CENTER);
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.4),title);
        pulse.setFromX(1);pulse.setFromY(1);pulse.setToX(1.04);pulse.setToY(1.04);pulse.setCycleCount(Animation.INDEFINITE);pulse.setAutoReverse(true);pulse.play();
        connectBtn.setOnAction(e -> handleConnect(nameField, connectBtn));
        nameField.setOnAction(e -> handleConnect(nameField, connectBtn));
        switchScene(new Scene(root, 1024, 768));
    }

    private void handleConnect(TextField nameField, Button connectBtn) {
        String nick = nameField.getText().trim();
        if (nick.isEmpty()) { statusLabel.setText("× ERROR :: ACCESS_CODE BOŞ OLAMAZ"); statusLabel.setTextFill(NEON_RED); return; }
        statusLabel.setText("» connecting..."); statusLabel.setTextFill(NEON_CYAN); connectBtn.setDisable(true);
        new Thread(() -> {
            try {
                if (client != null) client.stop();
                client = new Client(); Network.register(client); client.start();
                client.connect(5000, "127.0.0.1", 54555, 54777);
                client.addListener(buildNetworkListener(connectBtn));
                Network.JoinRequest req = new Network.JoinRequest(); req.username = nick; myUsername = nick;
                client.sendTCP(req);
            } catch (Exception ex) {
                Platform.runLater(() -> { statusLabel.setText("× CONNECTION FAILED"); statusLabel.setTextFill(NEON_RED); connectBtn.setDisable(false); });
            }
        }).start();
    }

    private Listener buildNetworkListener(Button connectBtn) {
        return new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                if (object instanceof Network.JoinResponse) {
                    Network.JoinResponse r = (Network.JoinResponse) object;
                    Platform.runLater(() -> {
                        if (r.isAccepted) showLobbyScreen();
                        else { statusLabel.setText("× " + r.message); statusLabel.setTextFill(NEON_RED); connectBtn.setDisable(false); }
                    });
                }

                if (object instanceof Network.LobbyUpdatePacket) {
                    // LobbyUpdate artık sadece counter günceller, asıl UI ReadyStatusPacket ile güncelleniyor
                    Network.LobbyUpdatePacket p = (Network.LobbyUpdatePacket) object;
                    Platform.runLater(() -> {
                        if (lobbyCounterLabel != null)
                            lobbyCounterLabel.setText("[ " + p.connectedPlayers.length + " / 14 ] connected");
                    });
                }

                // YENİ: Hazır durumu güncellemesi
                if (object instanceof Network.ReadyStatusPacket) {
                    Network.ReadyStatusPacket p = (Network.ReadyStatusPacket) object;
                    Platform.runLater(() -> updateLobbyWithReadyStatus(p.connectedPlayers, p.readyFlags));
                }

                if (object instanceof Network.GameStartedPacket) {
                    Network.GameStartedPacket p = (Network.GameStartedPacket) object;
                    Platform.runLater(() -> { myRole = p.assignedRole; amIEvil = p.isEvil; showGameScreen(p.assignedRole, p.isEvil, p.playerList); });
                }

                if (object instanceof Network.MorningPacket) {
                    Network.MorningPacket p = (Network.MorningPacket) object;
                    Platform.runLater(() -> showVotingScreen(p.morningMessage));
                }

                if (object instanceof Network.VoteResultPacket) {
                    Network.VoteResultPacket p = (Network.VoteResultPacket) object;
                    Platform.runLater(() -> {
                        showGameScreen(myRole, amIEvil, currentPlayers);
                    });
                }
            }
        };
    }

    // ================================================================
    //  EKRAN 2: LOBBY + HAZIR SİSTEMİ
    // ================================================================
    public void showLobbyScreen() {
        amReady = false;  // Her lobi girişinde sıfırla

        StackPane root = createThemedBackground();
        VBox content = new VBox(22); content.setAlignment(Pos.CENTER); content.setPadding(new Insets(30));

        Label title = createNeonTitle("// LOBBY", NEON_PURPLE, 50);
        Label sub = new Label("> bağlanan ajanlar ve hazır durumları");
        sub.setTextFill(Color.web("#9ca3af")); sub.setFont(Font.font(FONT_MONO,14));

        // Oyuncu listesi paneli
        VBox listPanel = createGlassPanel(NEON_PURPLE);
        listPanel.setMinWidth(620); listPanel.setMinHeight(340); listPanel.setMaxHeight(380);

        Label hdr = new Label("AĞDAKİ AKTİF AJANLAR");
        hdr.setTextFill(NEON_PURPLE); hdr.setFont(Font.font(FONT_MONO,FontWeight.BOLD,13));

        lobbyAvatarFlow = new FlowPane(18, 14);
        lobbyAvatarFlow.setAlignment(Pos.CENTER); lobbyAvatarFlow.setMaxWidth(580);

        listPanel.getChildren().addAll(hdr, lobbyAvatarFlow);

        // Hazır butonu
        readyButton = createNeonButton("» HAZIR «", NEON_GREEN, NEON_GREEN);
        readyButton.setOnAction(e -> {
            if (client != null && client.isConnected()) {
                client.sendTCP(new Network.ReadyPacket());
                amReady = !amReady;

                if (amReady) {
                    readyButton.setText("✓ HAZIRIM — iptal etmek için tıkla");
                    readyButton.setStyle("-fx-background-color:#00FF9C;-fx-text-fill:#0a0a0c;-fx-border-color:#00FF9C;-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;");
                } else {
                    readyButton.setText("» HAZIR «");
                    String greenHex = toHex(NEON_GREEN);
                    readyButton.setStyle("-fx-background-color:transparent;-fx-text-fill:"+greenHex+";-fx-border-color:"+greenHex+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;");
                }
            }
        });

        lobbyCounterLabel = new Label("[ 0 / 14 ] connected");
        lobbyCounterLabel.setTextFill(NEON_CYAN); lobbyCounterLabel.setFont(Font.font(FONT_MONO,FontWeight.BOLD,13));

        Label minInfo = new Label("minimum 2 ajan gerekli — herkes hazır olunca oyun başlar");
        minInfo.setTextFill(Color.web("#606070")); minInfo.setFont(Font.font(FONT_MONO,11));

        content.getChildren().addAll(title, sub, listPanel, readyButton, lobbyCounterLabel, minInfo);
        root.getChildren().add(content);

        FadeTransition tp = new FadeTransition(Duration.seconds(1.6),sub);
        tp.setFromValue(0.4);tp.setToValue(1);tp.setCycleCount(Animation.INDEFINITE);tp.setAutoReverse(true);tp.play();

        switchScene(new Scene(root, 1024, 768));
    }

    /** Lobi avatar listesini hazır durumuyla birlikte günceller */
    private void updateLobbyWithReadyStatus(String[] players, boolean[] readyFlags) {
        if (lobbyAvatarFlow == null) return;
        lobbyAvatarFlow.getChildren().clear();

        int readyCount = 0;
        for (int i = 0; i < players.length; i++) {
            boolean isReady = (readyFlags != null && i < readyFlags.length) && readyFlags[i];
            if (isReady) readyCount++;

            VBox card = createLobbyAvatarCard(players[i], isReady);
            card.setOpacity(0);
            lobbyAvatarFlow.getChildren().add(card);

            FadeTransition fi = new FadeTransition(Duration.millis(300), card);
            fi.setFromValue(0); fi.setToValue(1); fi.play();
        }

        if (lobbyCounterLabel != null) {
            lobbyCounterLabel.setText("[ " + players.length + " / 14 ] connected  —  " + readyCount + " hazır");
        }
    }

    // ================================================================
    //  EKRAN 3: GAME / ARENA (NIGHT)
    // ================================================================
    public void showGameScreen(String role, boolean isEvil, String[] playerList) {
        this.currentPlayers = playerList;
        StackPane root = createNightBackground();

        VBox topBar = new VBox(8); topBar.setAlignment(Pos.CENTER); topBar.setPadding(new Insets(22,0,0,0));
        Label phase = createNeonTitle("// NIGHT PHASE", NEON_PURPLE, 32);
        Label hint = new Label("> hedef seç :: bir ajanın avatarına tıkla");
        hint.setTextFill(Color.web("#c8c8d0")); hint.setFont(Font.font(FONT_MONO,13));
        DropShadow hs = new DropShadow(5,Color.BLACK); hs.setSpread(0.6); hint.setEffect(hs);
        topBar.getChildren().addAll(phase,hint);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);
        topBar.setPickOnBounds(false); topBar.setMouseTransparent(true);

        Pane arena = buildPlayerArena(playerList, isEvil);

        VBox roleCard = buildMyRoleCard(role, isEvil);
        StackPane.setAlignment(roleCard, Pos.BOTTOM_CENTER);
        StackPane.setMargin(roleCard, new Insets(0,0,24,0));
        roleCard.setMouseTransparent(true);

        root.getChildren().addAll(arena, topBar, roleCard);
        switchScene(new Scene(root, 1024, 768));
    }

    private Pane buildPlayerArena(String[] playerList, boolean isEvil) {
        Pane arena = new Pane(); arena.setPrefSize(1024,768); arena.setMaxSize(1024,768); arena.setPickOnBounds(false);
        double cx=512, cy=370, r=220;

        Circle ring = new Circle(cx,cy,r+30); ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#ffffff",0.1)); ring.setStrokeWidth(1); ring.setMouseTransparent(true);
        Circle inner = new Circle(cx,cy,r-50); inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web("#ffffff",0.06)); inner.setStrokeWidth(1);
        inner.getStrokeDashArray().addAll(4d,8d); inner.setMouseTransparent(true);
        arena.getChildren().addAll(ring, inner);

        int n = playerList.length; double off = -Math.PI/2;
        for (int i = 0; i < n; i++) {
            double angle = off + (2*Math.PI*i)/n;
            double px = cx+r*Math.cos(angle), py = cy+r*Math.sin(angle);
            String player = playerList[i]; boolean isMe = player.equals(myUsername);

            Color cardColor; boolean showHalo; boolean allyFlag = false;
            if (isMe) { cardColor = COLOR_SELF; showHalo = true; }
            else if (amIEvil && isEvilTeammate(player)) { cardColor = COLOR_ALLY_EVIL; showHalo = true; allyFlag = true; }
            else { cardColor = COLOR_NEUTRAL; showHalo = false; }

            VBox card = createAvatarCard(player, cardColor, showHalo, allyFlag);
            card.setLayoutX(px-60); card.setLayoutY(py-65);
            if (!isMe) attachTargetingBehavior(card, player, cardColor);

            card.setOpacity(0); card.setScaleX(0.5); card.setScaleY(0.5);
            FadeTransition fp = new FadeTransition(Duration.millis(500),card); fp.setFromValue(0); fp.setToValue(1);
            ScaleTransition spt = new ScaleTransition(Duration.millis(500),card);
            spt.setFromX(0.5);spt.setFromY(0.5);spt.setToX(1);spt.setToY(1);
            ParallelTransition entry = new ParallelTransition(fp, spt);
            entry.setDelay(Duration.millis(i*80)); entry.play();
            arena.getChildren().add(card);
        }
        return arena;
    }

    private boolean isEvilTeammate(String playerName) {
        if (evilTeammates == null) return false;
        for (String m : evilTeammates) { if (m.equals(playerName)) return true; }
        return false;
    }

    private void attachTargetingBehavior(VBox card, String target, Color baseColor) {
        card.setCursor(Cursor.HAND);
        ScaleTransition hi = new ScaleTransition(Duration.millis(180),card); hi.setToX(1.15); hi.setToY(1.15);
        ScaleTransition ho = new ScaleTransition(Duration.millis(180),card); ho.setToX(1.0); ho.setToY(1.0);
        card.setOnMouseEntered(e->hi.playFromStart()); card.setOnMouseExited(e->ho.playFromStart());

        card.setOnMouseClicked(e -> {
            Network.NightActionPacket a = new Network.NightActionPacket(); a.targetPlayerName = target; client.sendTCP(a);
            card.setDisable(true);
            Circle sr = new Circle(55,Color.TRANSPARENT); sr.setStroke(NEON_PINK); sr.setStrokeWidth(3);
            sr.setEffect(new DropShadow(35,NEON_PINK));
            ((StackPane)card.getChildren().get(0)).getChildren().add(0,sr);
            ScaleTransition lk = new ScaleTransition(Duration.millis(250),card); lk.setToX(1.1); lk.setToY(1.1); lk.play();
            Pane parent = (Pane) card.getParent();
            for (Node nd:parent.getChildren()) { if (nd!=card && nd instanceof VBox) { FadeTransition dm=new FadeTransition(Duration.millis(300),nd); dm.setToValue(0.3); dm.play(); nd.setDisable(true); } }
        });
    }

    private VBox buildMyRoleCard(String role, boolean isEvil) {
        Color tc = isEvil ? NEON_RED : NEON_CYAN; String tl = isEvil ? "[ ROGUE FACTION ]" : "[ SYSTEM FACTION ]";
        VBox c = createGlassPanel(tc); c.setMaxWidth(420); c.setMaxHeight(105); c.setPadding(new Insets(12,22,12,22)); c.setSpacing(3);
        Label h = new Label("YOUR IDENTITY"); h.setTextFill(Color.web("#9ca3af")); h.setFont(Font.font(FONT_MONO,10));
        Label rn = new Label(role); rn.setTextFill(tc); rn.setFont(Font.font(FONT_MONO,FontWeight.BOLD,24)); rn.setEffect(new DropShadow(14,tc));
        Label tm = new Label(tl); tm.setTextFill(tc); tm.setFont(Font.font(FONT_MONO,11)); tm.setOpacity(0.85);
        c.getChildren().addAll(h,rn,tm); return c;
    }

    // ================================================================
    //  EKRAN 4: VOTING (DAY)
    // ================================================================
    public void showVotingScreen(String message) {
        StackPane root = createDayBackground();
        VBox topBar = new VBox(10); topBar.setAlignment(Pos.CENTER); topBar.setPadding(new Insets(22,0,0,0));
        Label phase = createNeonTitle("// DAY :: VOTING", NEON_GOLD, 32);
        Label log = new Label("> "+message); log.setTextFill(Color.web("#fff8c4")); log.setFont(Font.font(FONT_MONO,13));
        DropShadow ls = new DropShadow(5,Color.BLACK); ls.setSpread(0.5); log.setEffect(ls);
        Label instr = new Label("> şüpheli ajanın avatarına tıkla");
        instr.setTextFill(Color.web("#1a3020")); instr.setFont(Font.font(FONT_MONO,12));
        topBar.getChildren().addAll(phase,log,instr);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);
        topBar.setPickOnBounds(false); topBar.setMouseTransparent(true);

        Pane arena = buildVotingArena(currentPlayers);
        VBox roleCard = buildMyRoleCard(myRole, amIEvil);
        StackPane.setAlignment(roleCard, Pos.BOTTOM_CENTER);
        StackPane.setMargin(roleCard, new Insets(0,0,24,0));
        roleCard.setMouseTransparent(true);

        root.getChildren().addAll(arena, topBar, roleCard);
        switchScene(new Scene(root, 1024, 768));
    }

    private Pane buildVotingArena(String[] playerList) {
        Pane arena = new Pane(); arena.setPrefSize(1024,768); arena.setPickOnBounds(false);
        double cx=512,cy=370,r=220;
        Circle ring = new Circle(cx,cy,r+30); ring.setFill(Color.TRANSPARENT); ring.setStroke(NEON_GOLD);
        ring.setStrokeWidth(1.5); ring.setOpacity(0.4); ring.setMouseTransparent(true); arena.getChildren().add(ring);
        int n = playerList.length; double off = -Math.PI/2;
        for (int i = 0; i < n; i++) {
            double angle = off+(2*Math.PI*i)/n;
            double px=cx+r*Math.cos(angle), py=cy+r*Math.sin(angle);
            String player=playerList[i]; boolean isMe=player.equals(myUsername);
            VBox card = createAvatarCard(player, isMe?COLOR_SELF:NEON_GOLD, isMe);
            card.setLayoutX(px-60); card.setLayoutY(py-65);
            if (!isMe) attachVotingBehavior(card, player);
            card.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(500),card); f.setFromValue(0); f.setToValue(1); f.setDelay(Duration.millis(i*70)); f.play();
            arena.getChildren().add(card);
        }
        return arena;
    }

    private void attachVotingBehavior(VBox card, String target) {
        card.setCursor(Cursor.HAND);
        ScaleTransition hi = new ScaleTransition(Duration.millis(180),card); hi.setToX(1.15); hi.setToY(1.15);
        ScaleTransition ho = new ScaleTransition(Duration.millis(180),card); ho.setToX(1.0); ho.setToY(1.0);
        card.setOnMouseEntered(e->hi.playFromStart()); card.setOnMouseExited(e->ho.playFromStart());
        card.setOnMouseClicked(e -> {
            Network.VotePacket v = new Network.VotePacket(); v.votedPlayerName = target; client.sendTCP(v);
            card.setDisable(true);
            Circle vr = new Circle(55,Color.TRANSPARENT); vr.setStroke(NEON_GOLD); vr.setStrokeWidth(3); vr.setEffect(new DropShadow(35,NEON_GOLD));
            ((StackPane)card.getChildren().get(0)).getChildren().add(0,vr);
            Pane parent = (Pane)card.getParent();
            for (Node nd:parent.getChildren()) { if (nd!=card && nd instanceof VBox) { FadeTransition dm=new FadeTransition(Duration.millis(300),nd); dm.setToValue(0.3); dm.play(); nd.setDisable(true); } }
        });
    }
}