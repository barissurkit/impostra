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
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Set;

public class ImpostraGUI extends Application {

    // ============================================================
    //  EKRAN BOYUTLARI
    // ============================================================
    private double SW = 1280, SH = 720;

    // ============================================================
    //  RENK PALETİ
    // ============================================================
    private static final Color NEON_CYAN   = Color.web("#00FBFF");
    private static final Color NEON_PURPLE = Color.web("#B14EFF");
    private static final Color NEON_GOLD   = Color.web("#FFD700");
    private static final Color NEON_PINK   = Color.web("#FF2E93");
    private static final Color NEON_RED    = Color.web("#FF0055");
    private static final Color NEON_GREEN  = Color.web("#00FF9C");
    private static final Color BG_DEEP     = Color.web("#0a0a0c");

    private static final Color COLOR_SELF           = NEON_CYAN;
    private static final Color COLOR_NEUTRAL        = Color.web("#c0d8e8");
    private static final Color COLOR_ALLY_EVIL      = Color.web("#ff3366");
    private static final Color COLOR_DEAD           = Color.web("#3a3a4a");
    private static final Color COLOR_CONFIRMED_GOOD = Color.web("#00FF9C");
    private static final Color COLOR_CONFIRMED_EVIL = Color.web("#ff3366");

    private static final Color NIGHT_TOP = Color.web("#0f0720"), NIGHT_MID = Color.web("#16213e"), NIGHT_BOT = Color.web("#0a0a12");
    private static final Color DAY_TOP   = Color.web("#2980b9"), DAY_MID   = Color.web("#74b9ff"), DAY_BOT   = Color.web("#a8e6cf");
    private static final Color GROUND_NIGHT = Color.web("#0d1f0d"), GROUND_DAY = Color.web("#3da35d");
    private static final Color TREE_NIGHT   = Color.web("#081208"), TREE_DAY   = Color.web("#1a6b35");

    private static final String FONT_MONO = "Consolas";

    // ============================================================
    //  SAHNE / AĞ
    // ============================================================
    private Stage  primaryStage;
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
    private String      myUsername     = "";
    private String[]    currentPlayers;
    private Set<String> deadPlayers    = new HashSet<>();
    private String      myRole         = "";
    private String      myRoleDesc     = "";
    private boolean     amIEvil        = false;
    private String[]    evilTeammates  = null;
    private int         currentRound   = 1;
    private String      syncPartnerName = "";  // Senkronize Düğüm eşi

    // Gece sırası
    private boolean     isMyNightTurn   = false;   // Şu an benim sıram mı?
    private boolean     nightActionSent = false;   // Aksiyonumu gönderdim mi?
    private Label       nightTimerLabel = null;    // Zamanlayıcı etiketi
    private Timeline    nightTimerAnim  = null;    // Zamanlayıcı animasyonu
    private String      activeNightRole = "";      // Şu an hangi rolün sırası
    private String[]    nightTargetOptions = new String[0]; // Seçilebilecek hedefler
    private String      blockedTargetName = "";    // Engellenen hedef (Güv. Müh. için son korunan)

    private final java.util.Map<String, Boolean> analystFindings = new java.util.HashMap<>();
    private Node analystOverlay = null;

    // Sohbet
    private VBox       chatMessagesBox = null;   // Mesajların listelendiği container
    private javafx.scene.control.ScrollPane chatScroll = null;
    private TextField  chatInputField  = null;
    private final java.util.List<Network.ChatBroadcastPacket> chatHistory = new java.util.ArrayList<>();

    // Tartışma & oylama zamanlayıcısı
    private Label      phaseTimerLabel = null;
    private Timeline   phaseTimerAnim  = null;
    private String     currentPhaseLabel = "GECE";   // GECE / TARTIŞMA / OYLAMA

    // Son sözler
    private boolean    lastWordsActive    = false;
    private int        lastWordsRemaining = 0;

    // ============================================================
    //  SES EFEKTLERİ
    //  JavaFX AudioClip — WAV/MP3 asset olmadan da basit URL ile çalışır.
    //  Asset yoksa ses sessizce atlanır.
    // ============================================================
    private AudioClip soundNight, soundDeath, soundVote, soundWin, soundLose;

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

        // Pencere kapatılınca temiz kapanma
        primaryStage.setOnCloseRequest(e -> {
            try {
                if (nightTimerAnim != null) nightTimerAnim.stop();
                if (phaseTimerAnim != null) phaseTimerAnim.stop();
                if (client != null && client.isConnected()) {
                    client.close();
                    client.stop();
                }
            } catch (Exception ignored) {}
            Platform.exit();
        });
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        SW = screen.getWidth(); SH = screen.getHeight();
        loadAssets();
        loadSounds();
        showLoginScreen();
        primaryStage.setTitle("Impostra :: Digital Shift");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void loadAssets() {
        try { bgPattern     = new Image(getClass().getResourceAsStream("/assets/square.png"));        } catch (Exception e) { bgPattern = null; }
        try { penguinAvatar = new Image(getClass().getResourceAsStream("/assets/penguin_agent.png")); } catch (Exception e) { penguinAvatar = null; }
    }

    /**
     * Ses dosyalarını yükler.
     * Dosyalar src/main/resources/assets/sounds/ altında olmalı:
     *   night.wav, death.wav, vote.wav, win.wav, lose.wav
     * Dosya yoksa sessizce atlanır — oyun çökmez.
     */
    private void loadSounds() {
        soundNight = loadClip("/assets/sounds/night.wav");
        soundDeath = loadClip("/assets/sounds/death.wav");
        soundVote  = loadClip("/assets/sounds/vote.wav");
        soundWin   = loadClip("/assets/sounds/win.wav");
        soundLose  = loadClip("/assets/sounds/lose.wav");
    }

    private AudioClip loadClip(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) return new AudioClip(url.toExternalForm());
        } catch (Exception ignored) {}
        return null;
    }

    private void playSound(AudioClip clip) {
        if (clip != null) {
            try { clip.play(); } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) { launch(args); }

    // ================================================================
    //  ÖLÇEK YARDIMCILARI
    // ================================================================
    private double sx(double b) { return b * (SW / 1920.0); }
    private double sy(double b) { return b * (SH / 1080.0); }
    private double sf(double b) { return b * Math.min(SW / 1920.0, SH / 1080.0); }

    private void switchScene(StackPane root) {
        Scene scene = new Scene(root, SW, SH);
        primaryStage.setScene(scene);
        root.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(500), root);
        f.setFromValue(0); f.setToValue(1); f.play();
    }

    // ================================================================
    //  TUR SAYACI WIDGET — sağ üst köşe
    // ================================================================
    private VBox buildRoundIndicator(int round, String phase) {
        VBox box = new VBox(sy(2));
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(sy(12), sx(18), 0, 0));
        box.setMouseTransparent(true);

        Color phaseColor = phase.contains("GECE") ? NEON_PURPLE : NEON_GOLD;

        Label roundLbl = new Label("TUR " + round);
        roundLbl.setTextFill(phaseColor);
        roundLbl.setFont(Font.font(FONT_MONO, FontWeight.BOLD, sf(14)));
        DropShadow g = new DropShadow(sf(10), phaseColor); g.setSpread(0.3);
        roundLbl.setEffect(g);

        Label phaseLbl = new Label(phase);
        phaseLbl.setTextFill(Color.web("#808090"));
        phaseLbl.setFont(Font.font(FONT_MONO, sf(11)));

        box.getChildren().addAll(roundLbl, phaseLbl);
        return box;
    }

    // ================================================================
    //  ÖLÜM ANİMASYONU
    //  Belirtilen oyuncunun avatar kartı üzerinde tetiklenir.
    //  1. Kırmızı flash
    //  2. "SİSTEM HATASI" yazısı
    //  3. Blur efekti
    //  4. X işaretine geçiş
    // ================================================================
    private void triggerDeathAnimation(StackPane rootScene, String deadPlayerName) {
        // Tam ekran kırmızı flash overlay
        Rectangle flash = new Rectangle(SW, SH, Color.web("#FF0000", 0.18));
        flash.setMouseTransparent(true);
        rootScene.getChildren().add(flash);

        // "SİSTEM HATASI" merkez uyarısı
        Label errorLbl = new Label("⚠  SİSTEM HATASI: " + deadPlayerName.toUpperCase() + "  ⚠");
        errorLbl.setTextFill(NEON_RED);
        errorLbl.setFont(Font.font(FONT_MONO, FontWeight.BOLD, sf(28)));
        DropShadow eg = new DropShadow(sf(30), NEON_RED); eg.setSpread(0.4);
        errorLbl.setEffect(eg);
        StackPane.setAlignment(errorLbl, Pos.CENTER);
        rootScene.getChildren().add(errorLbl);

        // Flash animasyonu
        FadeTransition flashFade = new FadeTransition(Duration.millis(800), flash);
        flashFade.setFromValue(1); flashFade.setToValue(0);
        flashFade.setOnFinished(e -> rootScene.getChildren().remove(flash));
        flashFade.play();

        // Hata yazısı animasyonu
        ScaleTransition errScale = new ScaleTransition(Duration.millis(200), errorLbl);
        errScale.setFromX(0.5); errScale.setFromY(0.5);
        errScale.setToX(1.0);   errScale.setToY(1.0);
        errScale.play();

        PauseTransition errPause = new PauseTransition(Duration.seconds(2));
        errPause.setOnFinished(e -> {
            FadeTransition errFade = new FadeTransition(Duration.millis(500), errorLbl);
            errFade.setFromValue(1); errFade.setToValue(0);
            errFade.setOnFinished(ev -> rootScene.getChildren().remove(errorLbl));
            errFade.play();
        });
        errPause.play();

        // Ses efekti
        playSound(soundDeath);
    }

    // ================================================================
    //  ATMOSFERİK ARKA PLANLAR
    // ================================================================

    private StackPane createThemedBackground() {
        StackPane root = new StackPane(); root.setPrefSize(SW, SH);
        root.setBackground(new Background(new BackgroundFill(BG_DEEP, CornerRadii.EMPTY, Insets.EMPTY)));
        if (bgPattern != null) {
            ImageView pv = new ImageView(bgPattern); pv.setOpacity(0.06); pv.setPreserveRatio(false);
            pv.setFitWidth(SW); pv.setFitHeight(SH); pv.setMouseTransparent(true);
            root.getChildren().add(pv);
        }
        return root;
    }

    private StackPane createNightBackground() {
        StackPane root = new StackPane(); root.setPrefSize(SW, SH);
        Pane sc = new Pane(); sc.setPrefSize(SW, SH); sc.setMouseTransparent(true);
        Rectangle sky = new Rectangle(0,0,SW,SH);
        sky.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,NIGHT_TOP),new Stop(0.5,NIGHT_MID),new Stop(1,NIGHT_BOT)));
        sc.getChildren().add(sky);
        java.util.Random rng = new java.util.Random(42);
        for (int i=0;i<120;i++) {
            Circle star=new Circle(rng.nextDouble()*SW,rng.nextDouble()*SH*0.6,sf(0.8+rng.nextDouble()*2.0),Color.WHITE);
            star.setOpacity(0.4+rng.nextDouble()*0.6);
            FadeTransition tw=new FadeTransition(Duration.seconds(1.2+rng.nextDouble()*3),star);
            tw.setFromValue(star.getOpacity());tw.setToValue(0.1);tw.setCycleCount(Animation.INDEFINITE);tw.setAutoReverse(true);tw.play();
            sc.getChildren().add(star);
        }
        double moonX=sx(1720),moonY=sy(130),moonR=sf(50);
        Circle moon=new Circle(moonX,moonY,moonR,Color.web("#f5f3e0"));
        DropShadow mg=new DropShadow(sf(55),Color.web("#c8d0ff"));mg.setSpread(0.3);moon.setEffect(mg);
        sc.getChildren().addAll(moon,
                new Circle(moonX-sf(12),moonY-sf(12),sf(6),Color.web("#dddcc8")),
                new Circle(moonX+sf(18),moonY+sf(15),sf(5),Color.web("#dddcc8")),
                new Circle(moonX+sf(6),moonY+sf(8),sf(4),Color.web("#dddcc8")));
        Rectangle mist=new Rectangle(0,SH*0.55,SW,SH*0.2);
        mist.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,Color.TRANSPARENT),new Stop(0.5,Color.web("#1a2340",0.3)),new Stop(1,Color.TRANSPARENT)));
        sc.getChildren().add(mist);
        Polygon fh=new Polygon(0,SH*0.76,sx(120),SH*0.68,sx(280),SH*0.72,sx(460),SH*0.65,sx(650),SH*0.70,sx(820),SH*0.67,sx(1100),SH*0.71,sx(1400),SH*0.68,sx(1700),SH*0.72,SW,SH*0.70,SW,SH,0,SH);
        fh.setFill(Color.web("#0c1420"));fh.setOpacity(0.8);sc.getChildren().add(fh);
        Polygon nh=new Polygon(0,SH*0.82,sx(200),SH*0.76,sx(400),SH*0.80,sx(650),SH*0.74,sx(900),SH*0.78,sx(1200),SH*0.75,sx(1500),SH*0.79,SW,SH*0.80,SW,SH,0,SH);
        nh.setFill(Color.web("#0a1610"));sc.getChildren().add(nh);
        double[] txr={0.02,0.07,0.13,0.18,0.23,0.55,0.62,0.68,0.74,0.80,0.86,0.92,0.97};
        for (double rx:txr) sc.getChildren().add(buildTree(SW*rx,SH*0.80,TREE_NIGHT,sf(0.85+rng.nextDouble()*0.5)));
        sc.getChildren().add(new Rectangle(0,SH*0.92,SW,SH*0.08){{setFill(GROUND_NIGHT);}});
        sc.getChildren().add(new Rectangle(0,SH*0.85,SW,SH*0.08){{setFill(Color.web("#101830",0.35));}});
        root.getChildren().add(sc); return root;
    }

    private StackPane createDayBackground() {
        StackPane root=new StackPane();root.setPrefSize(SW,SH);
        Pane sc=new Pane();sc.setPrefSize(SW,SH);sc.setMouseTransparent(true);
        Rectangle sky=new Rectangle(0,0,SW,SH);
        sky.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,DAY_TOP),new Stop(0.5,DAY_MID),new Stop(0.85,DAY_BOT)));
        sc.getChildren().add(sky);
        double sunX=sx(1720),sunY=sy(130),sunR=sf(60);
        Circle sun=new Circle(sunX,sunY,sunR,Color.web("#fff7b0"));
        DropShadow sg=new DropShadow(sf(70),Color.web("#ffe066"));sg.setSpread(0.45);sun.setEffect(sg);
        sc.getChildren().add(sun);
        ScaleTransition sp=new ScaleTransition(Duration.seconds(3),sun);
        sp.setFromX(1);sp.setFromY(1);sp.setToX(1.06);sp.setToY(1.06);sp.setCycleCount(Animation.INDEFINITE);sp.setAutoReverse(true);sp.play();
        for (int i=1;i<=3;i++){Circle r=new Circle(sunX,sunY,sunR+sf(i*24));r.setFill(Color.TRANSPARENT);r.setStroke(Color.web("#fff7b0",0.12-i*0.03));r.setStrokeWidth(sf(1.5));sc.getChildren().add(r);}
        sc.getChildren().addAll(buildCloud(sx(120),sy(100),sf(1.0)),buildCloud(sx(400),sy(70),sf(0.85)),buildCloud(sx(750),sy(130),sf(1.15)),buildCloud(sx(1100),sy(80),sf(0.9)),buildCloud(sx(1450),sy(110),sf(1.0)),buildCloud(sx(200),sy(220),sf(0.65)));
        Polygon fh=new Polygon(0,SH*0.70,sx(160),SH*0.63,sx(320),SH*0.67,sx(500),SH*0.60,sx(750),SH*0.65,sx(1000),SH*0.62,sx(1300),SH*0.66,sx(1600),SH*0.63,SW,SH*0.68,SW,SH,0,SH);
        fh.setFill(Color.web("#6aad8a"));fh.setOpacity(0.5);sc.getChildren().add(fh);
        Polygon nh2=new Polygon(0,SH*0.80,sx(200),SH*0.74,sx(450),SH*0.78,sx(700),SH*0.72,sx(950),SH*0.77,sx(1200),SH*0.73,sx(1500),SH*0.76,SW,SH*0.79,SW,SH,0,SH);
        nh2.setFill(Color.web("#4a8a52"));sc.getChildren().add(nh2);
        java.util.Random rng=new java.util.Random(99);
        double[] txr={0.02,0.07,0.12,0.17,0.22,0.56,0.62,0.68,0.74,0.80,0.86,0.92,0.97};
        for (double rx:txr) sc.getChildren().add(buildTree(SW*rx,SH*0.82,TREE_DAY,sf(0.85+rng.nextDouble()*0.5)));
        for (int i=0;i<35;i++){double fx=rng.nextDouble()*SW,fy=SH*0.88+rng.nextDouble()*SH*0.05;sc.getChildren().add(new Circle(fx,fy,sf(3),rng.nextBoolean()?Color.web("#ff6b9d"):Color.web("#fff36e")){{setOpacity(0.8);}});}
        sc.getChildren().add(new Rectangle(0,SH*0.92,SW,SH*0.08){{setFill(GROUND_DAY);}});
        root.getChildren().add(sc);return root;
    }

    private Pane buildTree(double bx,double by,Color lc,double s){
        Pane t=new Pane();t.setMouseTransparent(true);
        double tw=8*s,th=32*s,cw=50*s,ch=75*s;
        Rectangle trunk=new Rectangle(bx-tw/2,by-th,tw,th);trunk.setFill(Color.web("#3d2817"));
        Polygon c1=new Polygon(bx-cw/2,by-th,bx+cw/2,by-th,bx,by-th-ch*0.45);
        Polygon c2=new Polygon(bx-cw*0.4,by-th-ch*0.25,bx+cw*0.4,by-th-ch*0.25,bx,by-th-ch*0.70);
        Polygon c3=new Polygon(bx-cw*0.3,by-th-ch*0.50,bx+cw*0.3,by-th-ch*0.50,bx,by-th-ch);
        c1.setFill(lc);c2.setFill(lc);c3.setFill(lc);
        t.getChildren().addAll(trunk,c1,c2,c3);return t;
    }

    private Pane buildCloud(double cx,double cy,double s){
        Pane c=new Pane();c.setMouseTransparent(true);
        Color cc=Color.web("#ffffff",0.7);
        Circle c1=new Circle(cx-28*s,cy,24*s,cc),c2=new Circle(cx,cy-10*s,30*s,cc),c3=new Circle(cx+28*s,cy,26*s,cc);
        c2.setEffect(new DropShadow(sf(12),Color.web("#ffffff",0.3)));
        c.getChildren().addAll(c1,c2,c3);return c;
    }

    // ================================================================
    //  UI FACTORY
    // ================================================================

    private VBox createGlassPanel(Color accent){
        VBox p=new VBox();p.setPadding(new Insets(sy(28)));p.setSpacing(sy(16));p.setAlignment(Pos.CENTER);
        p.setStyle("-fx-background-color:rgba(10,10,18,0.85);-fx-background-radius:18;-fx-border-color:"+toHex(accent)+";-fx-border-width:1.5;-fx-border-radius:18;");
        DropShadow g=new DropShadow(sf(30),accent);g.setSpread(0.06);p.setEffect(g);return p;
    }

    private Label createNeonTitle(String text,Color color,double baseSize){
        Label l=new Label(text);l.setTextFill(color);l.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(baseSize)));
        DropShadow g=new DropShadow(sf(22),color);g.setSpread(0.35);l.setEffect(g);return l;
    }

    private Button createNeonButton(String text,Color base,Color hover){
        Button b=new Button(text);b.setMinHeight(sy(50));b.setMinWidth(sx(280));b.setCursor(Cursor.HAND);b.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(16)));
        String bh=toHex(base),hh=toHex(hover);
        String idle="-fx-background-color:transparent;-fx-text-fill:"+bh+";-fx-border-color:"+bh+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;";
        String hot="-fx-background-color:"+hh+";-fx-text-fill:#0a0a0c;-fx-border-color:"+hh+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;";
        b.setStyle(idle);DropShadow glow=new DropShadow(sf(15),base);b.setEffect(glow);
        ScaleTransition su=new ScaleTransition(Duration.millis(120),b),sd=new ScaleTransition(Duration.millis(100),b);
        b.setOnMouseEntered(e->{b.setStyle(hot);glow.setColor(hover);glow.setRadius(sf(28));su.setToX(1.05);su.setToY(1.05);su.playFromStart();});
        b.setOnMouseExited(e->{b.setStyle(idle);glow.setColor(base);glow.setRadius(sf(15));su.setToX(1.0);su.setToY(1.0);su.playFromStart();});
        b.setOnMousePressed(e->{sd.setToX(0.96);sd.setToY(0.96);sd.playFromStart();});
        b.setOnMouseReleased(e->{sd.setToX(1.0);sd.setToY(1.0);sd.playFromStart();});return b;
    }

    private TextField createNeonField(String prompt){
        TextField tf=new TextField();tf.setPromptText(prompt);tf.setMinHeight(sy(48));tf.setMinWidth(sx(320));tf.setFont(Font.font(FONT_MONO,sf(15)));
        tf.setStyle("-fx-background-color:rgba(0,0,0,0.55);-fx-text-fill:#00FBFF;-fx-prompt-text-fill:#4d6e70;-fx-border-color:#00FBFF;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:0 14;");
        tf.setEffect(new DropShadow(sf(10),NEON_CYAN));return tf;
    }

    private VBox createAvatarCard(String username,Color glowColor,boolean showHalo,boolean isAlly,boolean isDead){
        VBox card=new VBox(sf(6));card.setAlignment(Pos.CENTER);card.setPadding(new Insets(sf(6)));
        double sz=sf(92);StackPane holder=new StackPane();holder.setMinSize(sz,sz);holder.setMaxSize(sz,sz);
        Color ac=isDead?COLOR_DEAD:glowColor;
        Circle halo=new Circle(sz/2.0);halo.setFill(Color.TRANSPARENT);halo.setStroke(ac);
        halo.setStrokeWidth(isDead?sf(1):(showHalo?sf(3.5):sf(1.2)));halo.setOpacity(isDead?0.25:(showHalo?1.0:0.45));
        if(!isDead&&showHalo){DropShadow hg=new DropShadow(sf(28),glowColor);hg.setSpread(0.15);halo.setEffect(hg);}
        Node av;
        if(penguinAvatar!=null){ImageView iv=new ImageView(penguinAvatar);iv.setFitWidth(sf(68));iv.setFitHeight(sf(68));iv.setPreserveRatio(true);if(isDead)iv.setOpacity(0.25);av=iv;}
        else{Circle fb=new Circle(sf(33),Color.web("#141420",isDead?0.4:0.9));fb.setStroke(ac);fb.setStrokeWidth(sf(2));av=fb;}
        holder.getChildren().addAll(halo,av);
        if(isDead){
            Label x=new Label("✕");x.setTextFill(Color.web("#ff4444",0.7));x.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(28)));
            // Ölüye blur efekti
            GaussianBlur blur=new GaussianBlur(sf(1.5));av.setEffect(blur);
            holder.getChildren().add(x);
        }
        Label name=new Label(username);name.setTextFill(isDead?COLOR_DEAD:glowColor);name.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));name.setOpacity(isDead?0.4:1.0);
        DropShadow ns=new DropShadow(sf(5),Color.BLACK);ns.setSpread(0.7);name.setEffect(ns);
        card.getChildren().addAll(holder,name);
        if(isDead){Label dl=new Label("[ SİLİNDİ ]");dl.setTextFill(Color.web("#ff4444",0.6));dl.setFont(Font.font(FONT_MONO,sf(9)));card.getChildren().add(dl);}
        if(isAlly&&!isDead){Rectangle ind=new Rectangle(sf(40),sf(3));ind.setFill(COLOR_ALLY_EVIL);ind.setArcWidth(6);ind.setArcHeight(6);ind.setOpacity(0.8);ind.setEffect(new DropShadow(sf(8),COLOR_ALLY_EVIL){{setSpread(0.3);}});card.getChildren().add(ind);}
        if(isDead)card.setOpacity(0.5);
        return card;
    }

    private VBox createAvatarCard(String u,Color c,boolean h,boolean a){return createAvatarCard(u,c,h,a,false);}
    private VBox createAvatarCard(String u,Color c,boolean h){return createAvatarCard(u,c,h,false,false);}

    private VBox createLobbyAvatarCard(String username,boolean isReady){
        Color color=isReady?NEON_GREEN:Color.web("#606070");
        VBox card=new VBox(sf(6));card.setAlignment(Pos.CENTER);card.setPadding(new Insets(sf(6)));
        double sz=sf(88);StackPane holder=new StackPane();holder.setMinSize(sz,sz);holder.setMaxSize(sz,sz);
        Circle halo=new Circle(sz/2.0);halo.setFill(Color.TRANSPARENT);halo.setStroke(color);halo.setStrokeWidth(isReady?sf(3):sf(1.2));halo.setOpacity(isReady?1.0:0.4);
        if(isReady){DropShadow hg=new DropShadow(sf(20),NEON_GREEN);hg.setSpread(0.15);halo.setEffect(hg);}
        Node av;
        if(penguinAvatar!=null){ImageView iv=new ImageView(penguinAvatar);iv.setFitWidth(sf(64));iv.setFitHeight(sf(64));iv.setPreserveRatio(true);av=iv;}
        else{Circle fb=new Circle(sf(30),Color.web("#141420",0.9));fb.setStroke(color);fb.setStrokeWidth(sf(2));av=fb;}
        holder.getChildren().addAll(halo,av);
        Label name=new Label(username);name.setTextFill(color);name.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(12)));
        Label status=new Label(isReady?"✓ HAZIR":"bekleniyor...");status.setTextFill(isReady?NEON_GREEN:Color.web("#505060"));status.setFont(Font.font(FONT_MONO,sf(10)));
        card.getChildren().addAll(holder,name,status);return card;
    }

    private VBox buildRoleDescriptionPanel(String roleName,String description,boolean isEvil){
        Color tc=isEvil?NEON_RED:NEON_CYAN;
        VBox panel=new VBox(sy(7));panel.setPadding(new Insets(sy(12),sx(14),sy(12),sx(14)));
        panel.setMaxWidth(sx(280));panel.setMinWidth(sx(200));
        panel.setStyle("-fx-background-color:rgba(8,8,16,0.88);-fx-background-radius:14;-fx-border-color:"+toHex(tc)+";-fx-border-width:1.2;-fx-border-radius:14;");
        DropShadow g=new DropShadow(sf(18),tc);g.setSpread(0.04);panel.setEffect(g);
        Label roleHeader=new Label("ROL BİLGİSİ");roleHeader.setTextFill(Color.web("#606070"));roleHeader.setFont(Font.font(FONT_MONO,sf(9)));
        Label roleLbl=new Label(roleName);roleLbl.setTextFill(tc);roleLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(15)));roleLbl.setEffect(new DropShadow(sf(10),tc));
        Rectangle sep=new Rectangle(sx(200),1);sep.setFill(tc);sep.setOpacity(0.3);
        VBox descBox=new VBox(sy(3));
        if(description!=null){for(String line:description.split("\n")){Label l=new Label(line);l.setTextFill(Color.web("#b0b8c0"));l.setFont(Font.font(FONT_MONO,sf(10)));l.setWrapText(true);l.setMaxWidth(sx(240));descBox.getChildren().add(l);}}
        panel.getChildren().addAll(roleHeader,roleLbl,sep,descBox);
        panel.setMouseTransparent(true);return panel;
    }

    private VBox buildAnalystResultOverlay(String targetName,boolean isEvil){
        Color tc=isEvil?COLOR_CONFIRMED_EVIL:COLOR_CONFIRMED_GOOD;
        String verdict=isEvil?"KÖTÜ — ROGUE SİSTEM":"İYİ — GÜVENLİ SİSTEM";
        String icon=isEvil?"⚠":"✓";
        VBox panel=new VBox(sy(8));panel.setPadding(new Insets(sy(14),sx(18),sy(14),sx(18)));panel.setMaxWidth(sx(320));
        panel.setStyle("-fx-background-color:rgba(8,8,16,0.92);-fx-background-radius:14;-fx-border-color:"+toHex(tc)+";-fx-border-width:2;-fx-border-radius:14;");
        DropShadow g=new DropShadow(sf(25),tc);g.setSpread(0.1);panel.setEffect(g);
        Label header=new Label("SİBER ANALİZ SONUCU");header.setTextFill(Color.web("#606070"));header.setFont(Font.font(FONT_MONO,sf(9)));
        Label iconLbl=new Label(icon+"  "+targetName.toUpperCase());iconLbl.setTextFill(tc);iconLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(18)));iconLbl.setEffect(new DropShadow(sf(12),tc));
        Label verdictLbl=new Label(verdict);verdictLbl.setTextFill(tc);verdictLbl.setFont(Font.font(FONT_MONO,sf(13)));verdictLbl.setOpacity(0.85);
        panel.getChildren().addAll(header,iconLbl,verdictLbl);panel.setMouseTransparent(true);return panel;
    }

    private void showAnalystResultOnScreen(StackPane root,String targetName,boolean isEvil){
        if(analystOverlay!=null)root.getChildren().remove(analystOverlay);
        VBox overlay=buildAnalystResultOverlay(targetName,isEvil);
        StackPane.setAlignment(overlay,Pos.TOP_RIGHT);StackPane.setMargin(overlay,new Insets(sy(80),sx(20),0,0));
        overlay.setOpacity(0);root.getChildren().add(overlay);analystOverlay=overlay;
        FadeTransition fi=new FadeTransition(Duration.millis(400),overlay);fi.setFromValue(0);fi.setToValue(1);fi.play();
        PauseTransition pause=new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e->{FadeTransition fo=new FadeTransition(Duration.millis(600),overlay);fo.setFromValue(1);fo.setToValue(0);fo.setOnFinished(ev->{root.getChildren().remove(overlay);analystOverlay=null;});fo.play();});
        pause.play();
    }

    private String toHex(Color c){return String.format("#%02X%02X%02X",(int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));}

    // ================================================================
    //  EKRAN 1: LOGIN
    // ================================================================
    public void showLoginScreen(){
        StackPane root=createThemedBackground();
        VBox panel=createGlassPanel(NEON_CYAN);panel.setMaxWidth(sx(500));panel.setSpacing(sy(18));
        Label title=createNeonTitle("IMPOSTRA",NEON_CYAN,80);
        Label sub=new Label("> DIGITAL SHIFT // ACCESS TERMINAL");sub.setTextFill(NEON_PURPLE);sub.setFont(Font.font(FONT_MONO,sf(14)));
        Rectangle sep=new Rectangle(sx(300),1);sep.setFill(NEON_CYAN);sep.setOpacity(0.4);
        TextField nameField=createNeonField("ACCESS_CODE  ::  type your nickname");
        TextField ipField=createNeonField("HOST  ::  127.0.0.1");
        ipField.setText("127.0.0.1");
        Button connectBtn=createNeonButton("» ENTER THE GRID «",NEON_CYAN,NEON_PINK);
        statusLabel=new Label("waiting for authorization...");statusLabel.setTextFill(Color.web("#7a8a8c"));statusLabel.setFont(Font.font(FONT_MONO,sf(12)));
        panel.getChildren().addAll(title,sub,sep,nameField,ipField,connectBtn,statusLabel);
        root.getChildren().add(panel);StackPane.setAlignment(panel,Pos.CENTER);
        ScaleTransition pulse=new ScaleTransition(Duration.seconds(1.4),title);
        pulse.setFromX(1);pulse.setFromY(1);pulse.setToX(1.04);pulse.setToY(1.04);pulse.setCycleCount(Animation.INDEFINITE);pulse.setAutoReverse(true);pulse.play();
        connectBtn.setOnAction(e->handleConnect(nameField,ipField,connectBtn));
        nameField.setOnAction(e->handleConnect(nameField,ipField,connectBtn));
        ipField.setOnAction(e->handleConnect(nameField,ipField,connectBtn));
        switchScene(root);
    }

    private void handleConnect(TextField nameField, TextField ipField, Button connectBtn){
        String nick=nameField.getText().trim();
        String host=ipField.getText().trim();
        if(host.isEmpty()) host="127.0.0.1";
        if(nick.isEmpty()){statusLabel.setText("× ERROR :: ACCESS_CODE BOŞ OLAMAZ");statusLabel.setTextFill(NEON_RED);return;}
        statusLabel.setText("» connecting to "+host+" ...");statusLabel.setTextFill(NEON_CYAN);connectBtn.setDisable(true);
        final String finalHost=host;
        new Thread(()->{
            try{
                if(client!=null)client.stop();
                client=new Client();Network.register(client);client.start();
                client.connect(5000,finalHost,54555,54777);
                client.addListener(buildNetworkListener(connectBtn));
                Network.JoinRequest req=new Network.JoinRequest();req.username=nick;myUsername=nick;
                client.sendTCP(req);
            }catch(Exception ex){Platform.runLater(()->{statusLabel.setText("× CONNECTION FAILED :: "+finalHost);statusLabel.setTextFill(NEON_RED);connectBtn.setDisable(false);});}
        }).start();
    }

    // ================================================================
    //  NETWORK LISTENER
    // ================================================================
    private Listener buildNetworkListener(Button connectBtn){
        return new Listener(){
            @Override
            public void received(Connection connection,Object object){
                if(object instanceof Network.JoinResponse){
                    Network.JoinResponse r=(Network.JoinResponse)object;
                    Platform.runLater(()->{if(r.isAccepted)showLobbyScreen();else{statusLabel.setText("× "+r.message);statusLabel.setTextFill(NEON_RED);connectBtn.setDisable(false);}});
                }
                if(object instanceof Network.LobbyUpdatePacket){
                    Network.LobbyUpdatePacket p=(Network.LobbyUpdatePacket)object;
                    Platform.runLater(()->{if(lobbyCounterLabel!=null)lobbyCounterLabel.setText("[ "+p.connectedPlayers.length+" / 14 ] connected");});
                }
                if(object instanceof Network.ReadyStatusPacket){
                    Network.ReadyStatusPacket p=(Network.ReadyStatusPacket)object;
                    Platform.runLater(()->updateLobbyWithReadyStatus(p.connectedPlayers,p.readyFlags));
                }
                if(object instanceof Network.GameStartedPacket){
                    Network.GameStartedPacket p=(Network.GameStartedPacket)object;
                    Platform.runLater(()->{
                        myRole=p.assignedRole;myRoleDesc=p.roleDescription!=null?p.roleDescription:"";
                        amIEvil=p.isEvil;currentPlayers=p.playerList;evilTeammates=p.evilTeammates;
                        syncPartnerName=p.syncPartnerName!=null?p.syncPartnerName:"";
                        deadPlayers.clear();analystFindings.clear();currentRound=1;
                        nightActionSent=false;
                        playSound(soundNight);
                        showGameScreen(p.assignedRole,p.isEvil,p.playerList);
                    });
                }
                if(object instanceof Network.MorningPacket){
                    Network.MorningPacket p=(Network.MorningPacket)object;
                    Platform.runLater(()->{
                        if(p.roundNumber>0)currentRound=p.roundNumber;
                        if(p.killedPlayer!=null&&!p.killedPlayer.isEmpty()){
                            deadPlayers.add(p.killedPlayer);
                            System.out.println("[CLIENT] Gece öldürüldü: "+p.killedPlayer);
                            Scene scene=primaryStage.getScene();
                            if(scene!=null&&scene.getRoot() instanceof StackPane){
                                triggerDeathAnimation((StackPane)scene.getRoot(),p.killedPlayer);
                            }
                            PauseTransition delay=new PauseTransition(Duration.seconds(2.5));
                            delay.setOnFinished(e->showVotingScreen(p.morningMessage));
                            delay.play();
                        } else {
                            showVotingScreen(p.morningMessage);
                        }
                    });
                }
                // YENİ: Gündüz tartışma fazı başladı
                if(object instanceof Network.DiscussionPhasePacket){
                    Network.DiscussionPhasePacket p=(Network.DiscussionPhasePacket)object;
                    Platform.runLater(()->{
                        if(p.roundNumber>0)currentRound=p.roundNumber;
                        if(p.killedPlayer!=null&&!p.killedPlayer.isEmpty()){
                            deadPlayers.add(p.killedPlayer);
                            playSound(soundDeath);
                            Scene scene=primaryStage.getScene();
                            if(scene!=null&&scene.getRoot() instanceof StackPane){
                                triggerDeathAnimation((StackPane)scene.getRoot(),p.killedPlayer);
                            }
                            PauseTransition delay=new PauseTransition(Duration.seconds(2.5));
                            delay.setOnFinished(e->showDiscussionScreen(p.morningMessage,p.durationSeconds));
                            delay.play();
                        } else {
                            showDiscussionScreen(p.morningMessage,p.durationSeconds);
                        }
                    });
                }
                // YENİ: Oylama fazı başladı (tartışma bitti)
                if(object instanceof Network.VotingPhaseStartPacket){
                    Network.VotingPhaseStartPacket p=(Network.VotingPhaseStartPacket)object;
                    Platform.runLater(()->showVotingScreenWithTimer(p.durationSeconds));
                }
                // YENİ: Sohbet mesajı
                if(object instanceof Network.ChatBroadcastPacket){
                    Network.ChatBroadcastPacket p=(Network.ChatBroadcastPacket)object;
                    Platform.runLater(()->appendChatMessage(p));
                }
                // YENİ: Son sözler
                if(object instanceof Network.LastWordsPacket){
                    Network.LastWordsPacket p=(Network.LastWordsPacket)object;
                    Platform.runLater(()->{
                        boolean iAmDying = p.playerName.equals(myUsername);
                        lastWordsActive = iAmDying;
                        lastWordsRemaining = p.durationSeconds;
                        if(iAmDying){
                            showLastWordsBanner(p.durationSeconds);
                        }
                        // Chat input'u yeniden değerlendir
                        refreshChatInputState();
                    });
                }
                if(object instanceof Network.VoteResultPacket){
                    Network.VoteResultPacket p=(Network.VoteResultPacket)object;
                    Platform.runLater(()->{
                        if(p.roundNumber>0)currentRound=p.roundNumber;
                        if(p.executedPlayer!=null&&!p.executedPlayer.isEmpty()){
                            deadPlayers.add(p.executedPlayer);
                            System.out.println("[CLIENT] Oylama ile silindi: "+p.executedPlayer);
                            playSound(soundVote);
                        }
                        showVoteResultThenNight(p.resultMessage);
                    });
                }
                if(object instanceof Network.GameOverPacket){
                    Network.GameOverPacket p=(Network.GameOverPacket)object;
                    Platform.runLater(()->showGameOverScreen(p.winnerMessage,p.playerNames,p.playerRoles,p.playerEvil,p.playerAlive));
                }
                if(object instanceof Network.AnalystResultPacket){
                    Network.AnalystResultPacket p=(Network.AnalystResultPacket)object;
                    Platform.runLater(()->{
                        analystFindings.put(p.targetName,p.isEvil);
                        Scene scene=primaryStage.getScene();
                        if(scene!=null&&scene.getRoot() instanceof StackPane)
                            showAnalystResultOnScreen((StackPane)scene.getRoot(),p.targetName,p.isEvil);
                    });
                }
                // Sunucu sıfırladı — lobiye dön
                if(object instanceof Network.ResetPacket){
                    Platform.runLater(()->{
                        deadPlayers.clear();analystFindings.clear();currentRound=1;amReady=false;
                        syncPartnerName="";nightActionSent=false;
                        if(nightTimerAnim!=null){nightTimerAnim.stop();nightTimerAnim=null;}
                        showLobbyScreen();
                    });
                }
                // Gece sırası bildirimi
                if(object instanceof Network.NightPhasePacket){
                    Network.NightPhasePacket p=(Network.NightPhasePacket)object;
                    Platform.runLater(()->{
                        activeNightRole=p.activeRole;
                        isMyNightTurn=p.isYourTurn;
                        String[] opts=p.targetOptions!=null?p.targetOptions:new String[0];
                        // Engellenen hedefi listeden çıkar (Güvenlik Mühendisi'nin son korumasını)
                        if(p.blockedTarget!=null&&!p.blockedTarget.isEmpty()){
                            java.util.List<String> filtered=new java.util.ArrayList<>();
                            for(String t:opts) if(!t.equals(p.blockedTarget)) filtered.add(t);
                            opts=filtered.toArray(new String[0]);
                            blockedTargetName=p.blockedTarget;
                        } else {
                            blockedTargetName="";
                        }
                        nightTargetOptions=opts;
                        if(p.isYourTurn) nightActionSent=false;
                        updateNightPhaseUI(p.activeRole,p.timeoutSeconds,p.isYourTurn);
                        if(currentPlayers!=null)
                            showGameScreen(myRole,amIEvil,currentPlayers);
                    });
                }
                // Rol değişti (Uyuyan Bot hacklendi)
                if(object instanceof Network.RoleChangedPacket){
                    Network.RoleChangedPacket p=(Network.RoleChangedPacket)object;
                    Platform.runLater(()->{
                        myRole=p.newRole;amIEvil=true;
                        showRoleChangedNotification(p.message);
                    });
                }
                // Gece aksiyon sonuçları (LOCKED, PROTECTED, RESTORED vb.)
                if(object instanceof Network.NightResultPacket){
                    Network.NightResultPacket p=(Network.NightResultPacket)object;
                    Platform.runLater(()->{
                        showNightResultNotification(p.resultType,p.message);
                        // Kilit bildirimi gelince otomatik pas gönder (oyun donmasın)
                        if("LOCKED".equals(p.resultType)&&client!=null&&client.isConnected()){
                            Network.VotePacket pass=new Network.VotePacket();
                            pass.votedPlayerName="";  // boş = pas
                            client.sendTCP(pass);
                        }
                    });
                }
                // Log Okuyucu sonucu
                if(object instanceof Network.LogReaderResultPacket){
                    Network.LogReaderResultPacket p=(Network.LogReaderResultPacket)object;
                    Platform.runLater(()->{
                        Scene scene=primaryStage.getScene();
                        if(scene!=null&&scene.getRoot() instanceof StackPane)
                            showLogReaderResult((StackPane)scene.getRoot(),p.targetName,p.roleName,p.wasEvil);
                    });
                }
                // Kötü takım listesi güncellendi (Uyuyan Bot hacklendi)
                if(object instanceof Network.EvilTeamUpdatePacket){
                    Network.EvilTeamUpdatePacket p=(Network.EvilTeamUpdatePacket)object;
                    Platform.runLater(()->{
                        evilTeammates=p.evilTeammates!=null?p.evilTeammates:new String[0];
                        amIEvil=true;
                        System.out.println("[CLIENT] Kötü takım güncellendi: "+java.util.Arrays.toString(evilTeammates));
                    });
                }
            }
        };
    }

    // ================================================================
    //  EKRAN 2: LOBBY
    // ================================================================
    public void showLobbyScreen(){
        amReady=false;
        StackPane root=createThemedBackground();
        VBox content=new VBox(sy(20));content.setAlignment(Pos.CENTER);content.setPadding(new Insets(sy(30)));
        Label title=createNeonTitle("// LOBBY",NEON_PURPLE,52);
        Label sub=new Label("> bağlanan ajanlar ve hazır durumları");sub.setTextFill(Color.web("#9ca3af"));sub.setFont(Font.font(FONT_MONO,sf(14)));
        VBox listPanel=createGlassPanel(NEON_PURPLE);listPanel.setMinWidth(sx(700));listPanel.setMinHeight(sy(340));listPanel.setMaxHeight(sy(400));
        Label hdr=new Label("AĞDAKİ AKTİF AJANLAR");hdr.setTextFill(NEON_PURPLE);hdr.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
        lobbyAvatarFlow=new FlowPane(sx(18),sy(14));lobbyAvatarFlow.setAlignment(Pos.CENTER);lobbyAvatarFlow.setMaxWidth(sx(660));
        listPanel.getChildren().addAll(hdr,lobbyAvatarFlow);
        readyButton=createNeonButton("» HAZIR «",NEON_GREEN,NEON_GREEN);
        readyButton.setOnAction(e->{
            if(client!=null&&client.isConnected()){
                client.sendTCP(new Network.ReadyPacket());amReady=!amReady;
                if(amReady){readyButton.setText("✓ HAZIRIM — iptal için tekrar tıkla");readyButton.setStyle("-fx-background-color:#00FF9C;-fx-text-fill:#0a0a0c;-fx-border-color:#00FF9C;-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;");}
                else{readyButton.setText("» HAZIR «");String gh=toHex(NEON_GREEN);readyButton.setStyle("-fx-background-color:transparent;-fx-text-fill:"+gh+";-fx-border-color:"+gh+";-fx-border-width:1.8;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:8 28;");}
            }
        });
        lobbyCounterLabel=new Label("[ 0 / 14 ] connected");lobbyCounterLabel.setTextFill(NEON_CYAN);lobbyCounterLabel.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
        Label minInfo=new Label("minimum 2 ajan gerekli — herkes hazır olunca oyun başlar");minInfo.setTextFill(Color.web("#606070"));minInfo.setFont(Font.font(FONT_MONO,sf(11)));
        content.getChildren().addAll(title,sub,listPanel,readyButton,lobbyCounterLabel,minInfo);
        root.getChildren().add(content);
        FadeTransition tp=new FadeTransition(Duration.seconds(1.6),sub);tp.setFromValue(0.4);tp.setToValue(1);tp.setCycleCount(Animation.INDEFINITE);tp.setAutoReverse(true);tp.play();
        switchScene(root);
    }

    private void updateLobbyWithReadyStatus(String[] players,boolean[] flags){
        if(lobbyAvatarFlow==null)return;
        lobbyAvatarFlow.getChildren().clear();int readyCount=0;
        for(int i=0;i<players.length;i++){
            boolean isReady=flags!=null&&i<flags.length&&flags[i];if(isReady)readyCount++;
            VBox card=createLobbyAvatarCard(players[i],isReady);card.setOpacity(0);lobbyAvatarFlow.getChildren().add(card);
            FadeTransition fi=new FadeTransition(Duration.millis(300),card);fi.setFromValue(0);fi.setToValue(1);fi.play();
        }
        if(lobbyCounterLabel!=null)lobbyCounterLabel.setText("[ "+players.length+" / 14 ] connected  —  "+readyCount+" hazır");
    }

    // ================================================================
    //  EKRAN 3: GAME (NIGHT)
    // ================================================================
    public void showGameScreen(String role,boolean isEvil,String[] playerList){
        currentPlayers=playerList;
        nightActionSent=false;
        currentPhaseLabel="GECE";
        StackPane root=createNightBackground();
        playSound(soundNight);

        // Üst bilgi
        VBox topBar=new VBox(sy(8));topBar.setAlignment(Pos.CENTER);topBar.setPadding(new Insets(sy(22),0,0,0));
        Label phase=createNeonTitle("// NIGHT PHASE",NEON_PURPLE,34);

        // Gece sırası göstergesi
        Label nightOrderHint=buildNightOrderHint(role);
        nightOrderHint.setMouseTransparent(true);

        topBar.getChildren().addAll(phase,nightOrderHint);StackPane.setAlignment(topBar,Pos.TOP_CENTER);
        topBar.setPickOnBounds(false);topBar.setMouseTransparent(true);

        // Zamanlayıcı etiketi — sağ üst altı
        nightTimerLabel=new Label("");nightTimerLabel.setTextFill(NEON_PURPLE);
        nightTimerLabel.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(18)));
        nightTimerLabel.setMouseTransparent(true);
        StackPane.setAlignment(nightTimerLabel,Pos.TOP_RIGHT);
        StackPane.setMargin(nightTimerLabel,new Insets(sy(70),sx(20),0,0));

        // Tur sayacı — sağ üst
        VBox roundBox=buildRoundIndicator(currentRound,"GECE");
        StackPane.setAlignment(roundBox,Pos.TOP_RIGHT);

        Pane arena=buildPlayerArena(playerList,isEvil,root);
        VBox roleCard=buildMyRoleCard(role,isEvil);
        StackPane.setAlignment(roleCard,Pos.BOTTOM_CENTER);StackPane.setMargin(roleCard,new Insets(0,0,sy(24),0));roleCard.setMouseTransparent(true);
        VBox descPanel=buildRoleDescriptionPanel(role,myRoleDesc,isEvil);
        StackPane.setAlignment(descPanel,Pos.BOTTOM_LEFT);StackPane.setMargin(descPanel,new Insets(0,0,sy(24),sx(18)));

        // Sync partner bilgisi (Senkronize Düğüm için)
        if(!syncPartnerName.isEmpty()){
            Label syncLbl=new Label("⟷ EŞİN: "+syncPartnerName);
            syncLbl.setTextFill(NEON_CYAN);syncLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
            syncLbl.setEffect(new DropShadow(sf(10),NEON_CYAN));syncLbl.setMouseTransparent(true);
            StackPane.setAlignment(syncLbl,Pos.BOTTOM_RIGHT);
            // Sync etiketi ile chat paneli çakışmasın diye yukarı al
            StackPane.setMargin(syncLbl,new Insets(0,sx(18),sy(320),0));
            root.getChildren().add(syncLbl);
        }

        // Chat paneli — sağ alt (kötüler ve ölüler için, iyilere readonly)
        VBox chatPanel=buildChatPanel();
        StackPane.setAlignment(chatPanel,Pos.BOTTOM_RIGHT);StackPane.setMargin(chatPanel,new Insets(0,sx(18),sy(24),0));

        root.getChildren().addAll(arena,topBar,roundBox,nightTimerLabel,roleCard,descPanel,chatPanel);
        switchScene(root);
    }

    private Pane buildPlayerArena(String[] playerList,boolean isEvil,StackPane root){
        Pane arena=new Pane();arena.setPrefSize(SW,SH);arena.setMaxSize(SW,SH);arena.setPickOnBounds(false);
        double cx=SW/2.0,cy=SH*0.46,r=Math.min(SW,SH)*0.22;
        Circle ring=new Circle(cx,cy,r+sf(30));ring.setFill(Color.TRANSPARENT);ring.setStroke(Color.web("#ffffff",0.1));ring.setStrokeWidth(sf(1));ring.setMouseTransparent(true);
        Circle inner=new Circle(cx,cy,r-sf(50));inner.setFill(Color.TRANSPARENT);inner.setStroke(Color.web("#ffffff",0.06));inner.setStrokeWidth(sf(1));inner.getStrokeDashArray().addAll(sf(4),sf(8));inner.setMouseTransparent(true);
        arena.getChildren().addAll(ring,inner);
        int n=playerList.length;double off=-Math.PI/2;double cardOffset=sf(60),cardYOffset=sf(65);

        // Sunucunun gönderdiği hedef listesini set'e çevir (hızlı arama için)
        java.util.Set<String> validTargets = new java.util.HashSet<>(java.util.Arrays.asList(nightTargetOptions));

        for(int i=0;i<n;i++){
            double angle=off+(2*Math.PI*i)/n;double px=cx+r*Math.cos(angle),py=cy+r*Math.sin(angle);
            String player=playerList[i];boolean isMe=player.equals(myUsername),isDead=deadPlayers.contains(player);
            Color cardColor;boolean showHalo;boolean allyFlag=false;
            boolean isSyncPartner = !syncPartnerName.isEmpty() && player.equals(syncPartnerName);
            if(isDead){cardColor=COLOR_DEAD;showHalo=false;}
            else if(isMe){cardColor=COLOR_SELF;showHalo=true;}
            else if(isSyncPartner){cardColor=NEON_CYAN;showHalo=true;allyFlag=true;}
            else if(amIEvil&&isEvilTeammate(player)){cardColor=COLOR_ALLY_EVIL;showHalo=true;allyFlag=true;}
            else if(analystFindings.containsKey(player)){boolean fe=analystFindings.get(player);cardColor=fe?COLOR_CONFIRMED_EVIL:COLOR_CONFIRMED_GOOD;showHalo=true;}
            else{cardColor=COLOR_NEUTRAL;showHalo=false;}
            VBox card=createAvatarCard(player,cardColor,showHalo,allyFlag,isDead);
            card.setLayoutX(px-cardOffset);card.setLayoutY(py-cardYOffset);

            // Güvenlik Mühendisi için "geçen gece koruduğun" işareti
            boolean isBlockedForMe = !blockedTargetName.isEmpty()
                    && player.equals(blockedTargetName)
                    && myRole.equals("Güvenlik Mühendisi")
                    && isMyNightTurn;
            if(isBlockedForMe){
                Label lockIcon=new Label("🚫");
                lockIcon.setTextFill(Color.web("#ff6060"));
                lockIcon.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(22)));
                lockIcon.setEffect(new DropShadow(sf(10),Color.web("#ff6060")));
                lockIcon.setMouseTransparent(true);
                StackPane iconWrap=(StackPane)card.getChildren().get(0);
                iconWrap.getChildren().add(lockIcon);
                card.setOpacity(0.45);
            }

            // Tıklanabilirlik: sunucunun gönderdiği validTargets listesinde varsa tıklanabilir.
            boolean isValidTarget;
            if(nightTargetOptions.length > 0){
                isValidTarget = validTargets.contains(player);
            } else {
                boolean canTargetSelf = myRole.equals("Güvenlik Mühendisi");
                isValidTarget = !isDead && (!isMe || canTargetSelf);
            }

            if(isValidTarget && !isBlockedForMe) attachTargetingBehavior(card,player,cardColor,root);

            card.setOpacity(0);card.setScaleX(0.5);card.setScaleY(0.5);
            FadeTransition fp=new FadeTransition(Duration.millis(500),card);fp.setFromValue(0);fp.setToValue(1);
            ScaleTransition spt=new ScaleTransition(Duration.millis(500),card);spt.setFromX(0.5);spt.setFromY(0.5);spt.setToX(1);spt.setToY(1);
            ParallelTransition entry=new ParallelTransition(fp,spt);entry.setDelay(Duration.millis(i*80));entry.play();
            arena.getChildren().add(card);
        }
        return arena;
    }

    private boolean isEvilTeammate(String name){if(evilTeammates==null)return false;for(String m:evilTeammates)if(m.equals(name))return true;return false;}

    private void attachTargetingBehavior(VBox card,String target,Color baseColor,StackPane root){
        card.setCursor(Cursor.HAND);
        ScaleTransition hi=new ScaleTransition(Duration.millis(180),card);hi.setToX(1.15);hi.setToY(1.15);
        ScaleTransition ho=new ScaleTransition(Duration.millis(180),card);ho.setToX(1.0);ho.setToY(1.0);
        card.setOnMouseEntered(e->{if(!card.isDisabled())hi.playFromStart();});
        card.setOnMouseExited(e->ho.playFromStart());
        card.setOnMouseClicked(e->{
            // Sıram değilse veya zaten aksiyon yaptıysam tıklama işlevsiz
            if(!isMyNightTurn||nightActionSent){
                showNightTurnNotification(activeNightRole.isEmpty()?"başka rol":activeNightRole);
                return;
            }
            Network.NightActionPacket a=new Network.NightActionPacket();a.targetPlayerName=target;client.sendTCP(a);
            nightActionSent=true;
            card.setDisable(true);
            Circle sr=new Circle(sf(55),Color.TRANSPARENT);sr.setStroke(NEON_PINK);sr.setStrokeWidth(sf(3));sr.setEffect(new DropShadow(sf(35),NEON_PINK));
            ((StackPane)card.getChildren().get(0)).getChildren().add(0,sr);
            ScaleTransition lk=new ScaleTransition(Duration.millis(250),card);lk.setToX(1.1);lk.setToY(1.1);lk.play();
            Pane parent=(Pane)card.getParent();
            for(Node nd:parent.getChildren()){if(nd!=card&&nd instanceof VBox){FadeTransition dm=new FadeTransition(Duration.millis(300),nd);dm.setToValue(0.3);dm.play();nd.setDisable(true);}}
        });
    }

    private VBox buildMyRoleCard(String role,boolean isEvil){
        Color tc=isEvil?NEON_RED:NEON_CYAN;String tl=isEvil?"[ ROGUE FACTION ]":"[ SYSTEM FACTION ]";
        VBox c=createGlassPanel(tc);c.setMaxWidth(sx(440));c.setMaxHeight(sy(110));c.setPadding(new Insets(sy(12),sx(22),sy(12),sx(22)));c.setSpacing(sy(3));
        Label h=new Label("YOUR IDENTITY");h.setTextFill(Color.web("#9ca3af"));h.setFont(Font.font(FONT_MONO,sf(10)));
        Label rn=new Label(role);rn.setTextFill(tc);rn.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(24)));rn.setEffect(new DropShadow(sf(14),tc));
        Label tm=new Label(tl);tm.setTextFill(tc);tm.setFont(Font.font(FONT_MONO,sf(11)));tm.setOpacity(0.85);
        c.getChildren().addAll(h,rn,tm);return c;
    }

    // ================================================================
    //  EKRAN 4: VOTING (DAY)
    // ================================================================
    public void showVotingScreen(String message){
        currentPhaseLabel="OYLAMA";
        StackPane root=createDayBackground();
        VBox topBar=new VBox(sy(10));topBar.setAlignment(Pos.CENTER);topBar.setPadding(new Insets(sy(22),0,0,0));
        Label phase=createNeonTitle("// DAY :: VOTING",NEON_GOLD,34);
        Label log=new Label("> "+message);log.setTextFill(Color.web("#fff8c4"));log.setFont(Font.font(FONT_MONO,sf(13)));
        DropShadow ls=new DropShadow(sf(5),Color.BLACK);ls.setSpread(0.5);log.setEffect(ls);
        boolean imDead=deadPlayers.contains(myUsername);
        Label instr=imDead?new Label("[ SİLİNDİN ] — sadece izleyebilirsin"):new Label("> şüpheli ajanın avatarına tıkla");
        instr.setTextFill(imDead?Color.web("#ff4444",0.8):Color.web("#1a3020"));instr.setFont(Font.font(FONT_MONO,sf(12)));
        topBar.getChildren().addAll(phase,log,instr);StackPane.setAlignment(topBar,Pos.TOP_CENTER);
        topBar.setPickOnBounds(false);topBar.setMouseTransparent(true);

        // Tur sayacı — sağ üst
        VBox roundBox=buildRoundIndicator(currentRound,"GÜNDÜZ");
        StackPane.setAlignment(roundBox,Pos.TOP_RIGHT);

        Pane arena=buildVotingArena(currentPlayers);
        VBox roleCard=buildMyRoleCard(myRole,amIEvil);
        StackPane.setAlignment(roleCard,Pos.BOTTOM_CENTER);StackPane.setMargin(roleCard,new Insets(0,0,sy(24),0));roleCard.setMouseTransparent(true);
        VBox descPanel=buildRoleDescriptionPanel(myRole,myRoleDesc,amIEvil);
        StackPane.setAlignment(descPanel,Pos.BOTTOM_LEFT);StackPane.setMargin(descPanel,new Insets(0,0,sy(24),sx(18)));

        root.getChildren().addAll(arena,topBar,roundBox,roleCard,descPanel);
        switchScene(root);
    }

    private Pane buildVotingArena(String[] playerList){
        Pane arena=new Pane();arena.setPrefSize(SW,SH);arena.setPickOnBounds(false);
        double cx=SW/2.0,cy=SH*0.46,r=Math.min(SW,SH)*0.22;
        Circle ring=new Circle(cx,cy,r+sf(30));ring.setFill(Color.TRANSPARENT);ring.setStroke(NEON_GOLD);ring.setStrokeWidth(sf(1.5));ring.setOpacity(0.4);ring.setMouseTransparent(true);arena.getChildren().add(ring);
        int n=playerList.length;double off=-Math.PI/2;boolean imDead=deadPlayers.contains(myUsername);
        double cardOffset=sf(60),cardYOffset=sf(65);
        for(int i=0;i<n;i++){
            double angle=off+(2*Math.PI*i)/n;double px=cx+r*Math.cos(angle),py=cy+r*Math.sin(angle);
            String player=playerList[i];boolean isMe=player.equals(myUsername),isDead=deadPlayers.contains(player);
            Color cc;
            if(isDead)cc=COLOR_DEAD;
            else if(isMe)cc=COLOR_SELF;
            else if(analystFindings.containsKey(player))cc=analystFindings.get(player)?COLOR_CONFIRMED_EVIL:COLOR_CONFIRMED_GOOD;
            else cc=NEON_GOLD;
            VBox card=createAvatarCard(player,cc,isMe,false,isDead);
            card.setLayoutX(px-cardOffset);card.setLayoutY(py-cardYOffset);
            if(!isMe&&!isDead&&!imDead)attachVotingBehavior(card,player);
            card.setOpacity(0);FadeTransition f=new FadeTransition(Duration.millis(500),card);f.setFromValue(0);f.setToValue(1);f.setDelay(Duration.millis(i*70));f.play();
            arena.getChildren().add(card);
        }
        return arena;
    }

    private void attachVotingBehavior(VBox card,String target){
        card.setCursor(Cursor.HAND);
        ScaleTransition hi=new ScaleTransition(Duration.millis(180),card);hi.setToX(1.15);hi.setToY(1.15);
        ScaleTransition ho=new ScaleTransition(Duration.millis(180),card);ho.setToX(1.0);ho.setToY(1.0);
        card.setOnMouseEntered(e->hi.playFromStart());card.setOnMouseExited(e->ho.playFromStart());
        card.setOnMouseClicked(e->{
            Network.VotePacket v=new Network.VotePacket();v.votedPlayerName=target;client.sendTCP(v);
            card.setDisable(true);
            Circle vr=new Circle(sf(55),Color.TRANSPARENT);vr.setStroke(NEON_GOLD);vr.setStrokeWidth(sf(3));vr.setEffect(new DropShadow(sf(35),NEON_GOLD));
            ((StackPane)card.getChildren().get(0)).getChildren().add(0,vr);
            Pane parent=(Pane)card.getParent();
            for(Node nd:parent.getChildren()){if(nd!=card&&nd instanceof VBox){FadeTransition dm=new FadeTransition(Duration.millis(300),nd);dm.setToValue(0.3);dm.play();nd.setDisable(true);}}
        });
    }

    // ================================================================
    //  GECE SIRASI UI
    // ================================================================

    /** Gece sırası ipucu etiketi — "Sıran: X saniye" veya "Bekliyorsun..." */
    private Label buildNightOrderHint(String myRole){
        String txt="> sıranı bekle... sunucu gece aksiyonlarını yönetiyor";
        Label l=new Label(txt);l.setTextFill(Color.web("#c8c8d0"));l.setFont(Font.font(FONT_MONO,sf(13)));
        DropShadow s=new DropShadow(sf(5),Color.BLACK);s.setSpread(0.6);l.setEffect(s);return l;
    }

    /**
     * NightPhasePacket geldiğinde çağrılır.
     * Ekrandaki hint metnini ve zamanlayıcıyı günceller.
     * Eğer sıra bende değilse avatar'lar disable edilir.
     */
    private void updateNightPhaseUI(String activeRole, int timeoutSeconds, boolean isMyTurn){
        // Zamanlayıcıyı güncelle
        if(nightTimerAnim!=null)nightTimerAnim.stop();
        if(nightTimerLabel==null)return;

        if(isMyTurn){
            nightTimerLabel.setTextFill(NEON_PINK);
            nightTimerLabel.setEffect(new DropShadow(sf(15),NEON_PINK));
            // Geri sayım animasyonu
            int[] remaining={timeoutSeconds};
            nightTimerLabel.setText("⏱ "+remaining[0]+"s");
            nightTimerAnim=new Timeline(new KeyFrame(Duration.seconds(1),e->{
                remaining[0]--;
                if(remaining[0]>0) nightTimerLabel.setText("⏱ "+remaining[0]+"s");
                else nightTimerLabel.setText("⏱ 0s");
            }));
            nightTimerAnim.setCycleCount(timeoutSeconds);
            nightTimerAnim.play();

            // Kısa overlay — "SIRAN GELDİ"
            showNightTurnNotification(activeRole);
        } else {
            nightTimerLabel.setText("[ "+activeRole+" aksiyonu... ]");
            nightTimerLabel.setTextFill(Color.web("#605070"));
            nightTimerLabel.setEffect(null);
        }
    }

    /** "SIRAN GELDİ" bildirimi — ekranın üst ortasına geçici overlay */
    private void showNightTurnNotification(String role){
        Scene scene=primaryStage.getScene();
        if(scene==null||!(scene.getRoot() instanceof StackPane))return;
        StackPane root=(StackPane)scene.getRoot();

        VBox notif=new VBox(sy(6));notif.setAlignment(Pos.CENTER);
        notif.setPadding(new Insets(sy(10),sx(20),sy(10),sx(20)));
        notif.setStyle("-fx-background-color:rgba(177,78,255,0.18);-fx-background-radius:12;-fx-border-color:#B14EFF;-fx-border-width:1.5;-fx-border-radius:12;");
        notif.setEffect(new DropShadow(sf(20),NEON_PURPLE));

        Label lbl=new Label("▶  SIRAN GELDİ — "+role.toUpperCase());
        lbl.setTextFill(NEON_PURPLE);lbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(16)));
        notif.getChildren().add(lbl);
        notif.setMouseTransparent(true);
        StackPane.setAlignment(notif,Pos.TOP_CENTER);
        StackPane.setMargin(notif,new Insets(sy(110),0,0,0));
        root.getChildren().add(notif);

        FadeTransition fi=new FadeTransition(Duration.millis(300),notif);fi.setFromValue(0);fi.setToValue(1);fi.play();
        PauseTransition pause=new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e->{
            FadeTransition fo=new FadeTransition(Duration.millis(500),notif);fo.setFromValue(1);fo.setToValue(0);
            fo.setOnFinished(ev->root.getChildren().remove(notif));fo.play();
        });
        pause.play();
    }

    /** Son sözler banner'ı — ölmek üzere olan oyuncuya gösterilir */
    private void showLastWordsBanner(int durationSeconds){
        Scene scene=primaryStage.getScene();
        if(scene==null||!(scene.getRoot() instanceof StackPane)) return;
        StackPane root=(StackPane)scene.getRoot();

        VBox banner=new VBox(sy(6));banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(sy(12),sx(20),sy(12),sx(20)));banner.setMaxWidth(sx(420));
        banner.setStyle("-fx-background-color:rgba(40,0,0,0.92);-fx-background-radius:14;-fx-border-color:#FF0055;-fx-border-width:2;-fx-border-radius:14;");
        banner.setEffect(new DropShadow(sf(25),NEON_RED));

        Label title=new Label("💀  SON SÖZLERİN");
        title.setTextFill(NEON_RED);title.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(16)));
        Label[] timeLbl={new Label("("+durationSeconds+" saniye kaldı)")};
        timeLbl[0].setTextFill(Color.web("#ffb0b0"));timeLbl[0].setFont(Font.font(FONT_MONO,sf(12)));
        Label hint=new Label("Herkes seni duyuyor. Konuş.");
        hint.setTextFill(Color.web("#ffd0d0"));hint.setFont(Font.font(FONT_MONO,sf(11)));

        banner.getChildren().addAll(title,timeLbl[0],hint);banner.setMouseTransparent(true);
        StackPane.setAlignment(banner,Pos.CENTER);
        StackPane.setMargin(banner,new Insets(0,0,sy(150),0));
        root.getChildren().add(banner);

        ScaleTransition st=new ScaleTransition(Duration.millis(400),banner);
        st.setFromX(0.6);st.setFromY(0.6);st.setToX(1);st.setToY(1);st.play();

        // Geri sayım
        int[] remaining={durationSeconds};
        Timeline countdown=new Timeline(new KeyFrame(Duration.seconds(1),e->{
            remaining[0]--;
            lastWordsRemaining=remaining[0];
            if(remaining[0]>=0) timeLbl[0].setText("("+remaining[0]+" saniye kaldı)");
            refreshChatInputState();
        }));
        countdown.setCycleCount(durationSeconds);
        countdown.setOnFinished(e->{
            lastWordsActive=false;
            lastWordsRemaining=0;
            FadeTransition fo=new FadeTransition(Duration.millis(600),banner);
            fo.setFromValue(1);fo.setToValue(0);
            fo.setOnFinished(ev->root.getChildren().remove(banner));
            fo.play();
            refreshChatInputState();
        });
        countdown.play();
    }

    /** Genel gece sonuç bildirimi — LOCKED, RESTORED, vb. */
    private void showNightResultNotification(String resultType, String message){
        Scene scene=primaryStage.getScene();
        if(scene==null||!(scene.getRoot() instanceof StackPane))return;
        StackPane root=(StackPane)scene.getRoot();

        // Tipe göre renk seç
        Color tc; String icon;
        switch(resultType){
            case "LOCKED":   tc=NEON_RED;    icon="🔒"; break;
            case "PROTECTED":tc=NEON_GREEN;  icon="🛡"; break;
            case "RESTORED": tc=NEON_CYAN;   icon="↻"; break;
            default:         tc=NEON_GOLD;   icon="ℹ"; break;
        }

        VBox notif=new VBox(sy(8));notif.setAlignment(Pos.CENTER);
        notif.setPadding(new Insets(sy(14),sx(22),sy(14),sx(22)));notif.setMaxWidth(sx(420));
        notif.setStyle("-fx-background-color:rgba(8,8,16,0.92);-fx-background-radius:14;-fx-border-color:"+toHex(tc)+";-fx-border-width:2;-fx-border-radius:14;");
        DropShadow g=new DropShadow(sf(22),tc);g.setSpread(0.1);notif.setEffect(g);

        Label iconLbl=new Label(icon);iconLbl.setTextFill(tc);iconLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(26)));
        Label msgLbl=new Label(message);msgLbl.setTextFill(tc);msgLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
        msgLbl.setWrapText(true);msgLbl.setMaxWidth(sx(380));
        notif.getChildren().addAll(iconLbl,msgLbl);notif.setMouseTransparent(true);

        StackPane.setAlignment(notif,Pos.CENTER);root.getChildren().add(notif);
        ScaleTransition st=new ScaleTransition(Duration.millis(300),notif);st.setFromX(0.6);st.setFromY(0.6);st.setToX(1);st.setToY(1);st.play();
        PauseTransition pause=new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e->{FadeTransition fo=new FadeTransition(Duration.millis(500),notif);fo.setFromValue(1);fo.setToValue(0);fo.setOnFinished(ev->root.getChildren().remove(notif));fo.play();});
        pause.play();
    }

    /** Uyuyan Bot rol değişim bildirimi */
    private void showRoleChangedNotification(String message){
        Scene scene=primaryStage.getScene();
        if(scene==null||!(scene.getRoot() instanceof StackPane))return;
        StackPane root=(StackPane)scene.getRoot();

        VBox notif=new VBox(sy(10));notif.setAlignment(Pos.CENTER);
        notif.setPadding(new Insets(sy(20),sx(30),sy(20),sx(30)));
        notif.setMaxWidth(sx(520));
        notif.setStyle("-fx-background-color:rgba(255,0,85,0.15);-fx-background-radius:16;-fx-border-color:#FF0055;-fx-border-width:2;-fx-border-radius:16;");
        notif.setEffect(new DropShadow(sf(25),NEON_RED));

        Label icon=new Label("⚠");icon.setTextFill(NEON_RED);icon.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(36)));
        Label title=new Label("SİSTEMİNE SIZMAK!");title.setTextFill(NEON_RED);title.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(20)));
        Label msg=new Label(message);msg.setTextFill(Color.web("#ffb0b0"));msg.setFont(Font.font(FONT_MONO,sf(13)));msg.setWrapText(true);msg.setMaxWidth(sx(460));
        notif.getChildren().addAll(icon,title,msg);

        StackPane.setAlignment(notif,Pos.CENTER);root.getChildren().add(notif);

        ScaleTransition st=new ScaleTransition(Duration.millis(300),notif);st.setFromX(0.5);st.setFromY(0.5);st.setToX(1);st.setToY(1);st.play();
        PauseTransition pause=new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e->{
            FadeTransition fo=new FadeTransition(Duration.millis(600),notif);fo.setFromValue(1);fo.setToValue(0);
            fo.setOnFinished(ev->root.getChildren().remove(notif));fo.play();
        });
        pause.play();
    }

    /** Log Okuyucu sonuç overlay */
    private void showLogReaderResult(StackPane root, String targetName, String roleName, boolean wasEvil){
        Color tc=wasEvil?COLOR_CONFIRMED_EVIL:COLOR_CONFIRMED_GOOD;
        VBox panel=new VBox(sy(8));panel.setPadding(new Insets(sy(14),sx(18),sy(14),sx(18)));panel.setMaxWidth(sx(340));
        panel.setStyle("-fx-background-color:rgba(8,8,16,0.92);-fx-background-radius:14;-fx-border-color:"+toHex(tc)+";-fx-border-width:2;-fx-border-radius:14;");
        DropShadow g=new DropShadow(sf(25),tc);g.setSpread(0.1);panel.setEffect(g);

        Label header=new Label("LOG OKUYUCU SONUCU");header.setTextFill(Color.web("#606070"));header.setFont(Font.font(FONT_MONO,sf(9)));
        Label nameLbl=new Label("📋  "+targetName.toUpperCase());nameLbl.setTextFill(tc);nameLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(17)));nameLbl.setEffect(new DropShadow(sf(10),tc));
        Label roleLbl=new Label("ROL: "+roleName);roleLbl.setTextFill(tc);roleLbl.setFont(Font.font(FONT_MONO,sf(13)));roleLbl.setOpacity(0.85);
        Label verdict=new Label(wasEvil?"⚠ KÖTÜ — ROGUE SİSTEM":"✓ İYİ — GÜVENLİ SİSTEM");verdict.setTextFill(tc);verdict.setFont(Font.font(FONT_MONO,sf(12)));
        panel.getChildren().addAll(header,nameLbl,roleLbl,verdict);panel.setMouseTransparent(true);

        StackPane.setAlignment(panel,Pos.TOP_RIGHT);StackPane.setMargin(panel,new Insets(sy(80),sx(20),0,0));
        panel.setOpacity(0);root.getChildren().add(panel);
        FadeTransition fi=new FadeTransition(Duration.millis(400),panel);fi.setFromValue(0);fi.setToValue(1);fi.play();
        PauseTransition pause=new PauseTransition(Duration.seconds(6));
        pause.setOnFinished(e->{FadeTransition fo=new FadeTransition(Duration.millis(600),panel);fo.setFromValue(1);fo.setToValue(0);fo.setOnFinished(ev->root.getChildren().remove(panel));fo.play();});
        pause.play();
    }

    // ================================================================
    //  EKRAN 4.5: GÜNDÜZ TARTIŞMA FAZI
    //  Oylama öncesi serbest tartışma — herkes sohbet edebilir.
    // ================================================================
    public void showDiscussionScreen(String morningMessage, int durationSeconds){
        currentPhaseLabel="TARTIŞMA";
        StackPane root=createDayBackground();

        // Üst başlık
        VBox topBar=new VBox(sy(8));topBar.setAlignment(Pos.CENTER);topBar.setPadding(new Insets(sy(22),0,0,0));
        Label phase=createNeonTitle("// DAY :: DISCUSSION",NEON_GOLD,32);
        Label log=new Label("> "+morningMessage);log.setTextFill(Color.web("#fff8c4"));log.setFont(Font.font(FONT_MONO,sf(13)));log.setWrapText(true);log.setMaxWidth(sx(900));
        DropShadow ls=new DropShadow(sf(5),Color.BLACK);ls.setSpread(0.5);log.setEffect(ls);
        Label hint=new Label("> tartışın :: birbirinizle konuşun, şüphelileri tespit edin");
        hint.setTextFill(Color.web("#1a3020"));hint.setFont(Font.font(FONT_MONO,sf(12)));
        topBar.getChildren().addAll(phase,log,hint);
        StackPane.setAlignment(topBar,Pos.TOP_CENTER);topBar.setPickOnBounds(false);topBar.setMouseTransparent(true);

        // Tur ve zamanlayıcı
        VBox roundBox=buildRoundIndicator(currentRound,"GÜNDÜZ");
        StackPane.setAlignment(roundBox,Pos.TOP_RIGHT);

        // Arena (avatarlar — tıklanamaz, sadece görsel)
        Pane arena=buildDiscussionArena(currentPlayers);

        // Rol kartları
        VBox roleCard=buildMyRoleCard(myRole,amIEvil);
        StackPane.setAlignment(roleCard,Pos.BOTTOM_CENTER);StackPane.setMargin(roleCard,new Insets(0,0,sy(24),0));roleCard.setMouseTransparent(true);
        VBox descPanel=buildRoleDescriptionPanel(myRole,myRoleDesc,amIEvil);
        StackPane.setAlignment(descPanel,Pos.BOTTOM_LEFT);StackPane.setMargin(descPanel,new Insets(0,0,sy(24),sx(18)));

        // Chat paneli — sağ alt
        VBox chatPanel=buildChatPanel();
        StackPane.setAlignment(chatPanel,Pos.BOTTOM_RIGHT);StackPane.setMargin(chatPanel,new Insets(0,sx(18),sy(24),0));

        // Zamanlayıcı etiketi
        phaseTimerLabel=buildPhaseTimerLabel(durationSeconds,NEON_GOLD);
        StackPane.setAlignment(phaseTimerLabel,Pos.TOP_RIGHT);
        StackPane.setMargin(phaseTimerLabel,new Insets(sy(70),sx(20),0,0));

        root.getChildren().addAll(arena,topBar,roundBox,phaseTimerLabel,roleCard,descPanel,chatPanel);
        switchScene(root);
        startPhaseTimer(durationSeconds,NEON_GOLD);
    }

    /** Oylama ekranını zamanlayıcıyla aç (yeni paket akışı için) */
    public void showVotingScreenWithTimer(int durationSeconds){
        currentPhaseLabel="OYLAMA";
        showVotingScreen("> şüpheli ajanın avatarına tıkla");
        // showVotingScreen kendi scene'ini kurar, üstüne timer ve chat ekleyelim
        Scene scene=primaryStage.getScene();
        if(scene!=null&&scene.getRoot() instanceof StackPane){
            StackPane root=(StackPane)scene.getRoot();

            // Chat paneli
            VBox chatPanel=buildChatPanel();
            StackPane.setAlignment(chatPanel,Pos.BOTTOM_RIGHT);StackPane.setMargin(chatPanel,new Insets(0,sx(18),sy(24),0));
            root.getChildren().add(chatPanel);

            // Zamanlayıcı
            if(durationSeconds>0){
                phaseTimerLabel=buildPhaseTimerLabel(durationSeconds,NEON_PINK);
                StackPane.setAlignment(phaseTimerLabel,Pos.TOP_RIGHT);
                StackPane.setMargin(phaseTimerLabel,new Insets(sy(70),sx(20),0,0));
                root.getChildren().add(phaseTimerLabel);
                startPhaseTimer(durationSeconds,NEON_PINK);
            }

            // SKIP VOTE butonu — sol alt
            boolean imDead=deadPlayers.contains(myUsername);
            if(!imDead){
                Button skipBtn=new Button("» PAS GEÇ");
                skipBtn.setCursor(Cursor.HAND);
                skipBtn.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(12)));
                String baseStyle="-fx-background-color:rgba(8,8,16,0.85);-fx-text-fill:#808090;-fx-border-color:#808090;-fx-border-width:1.5;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:6 14;";
                String hoverStyle="-fx-background-color:rgba(255,215,0,0.15);-fx-text-fill:#FFD700;-fx-border-color:#FFD700;-fx-border-width:1.5;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:6 14;";
                skipBtn.setStyle(baseStyle);
                skipBtn.setOnMouseEntered(ev->skipBtn.setStyle(hoverStyle));
                skipBtn.setOnMouseExited(ev->skipBtn.setStyle(baseStyle));
                skipBtn.setOnAction(ev->{
                    if(client!=null&&client.isConnected()){
                        Network.VotePacket pass=new Network.VotePacket();
                        pass.votedPlayerName="";  // boş = pas
                        client.sendTCP(pass);
                        skipBtn.setText("» PAS GEÇİLDİ");
                        skipBtn.setDisable(true);
                        // Avatarları da disable et
                        disableAllAvatars(root);
                    }
                });
                StackPane.setAlignment(skipBtn,Pos.BOTTOM_LEFT);
                StackPane.setMargin(skipBtn,new Insets(0,0,sy(24),sx(280)));  // rol kartının yanına
                root.getChildren().add(skipBtn);
            }
        }
    }

    /** Tüm avatar tıklamasını kapat (oy verildikten sonra) */
    private void disableAllAvatars(StackPane root){
        for(Node n:root.getChildren()){
            if(n instanceof Pane){
                for(Node c:((Pane)n).getChildren()){
                    if(c instanceof VBox){
                        c.setDisable(true);
                        FadeTransition dm=new FadeTransition(Duration.millis(300),c);
                        dm.setToValue(0.4);dm.play();
                    }
                }
            }
        }
    }

    /** Tartışma ekranındaki arena — avatarlar tıklanamaz, sadece görsel */
    private Pane buildDiscussionArena(String[] playerList){
        Pane arena=new Pane();arena.setPrefSize(SW,SH);arena.setPickOnBounds(false);
        double cx=SW/2.0,cy=SH*0.46,r=Math.min(SW,SH)*0.22;
        Circle ring=new Circle(cx,cy,r+sf(30));ring.setFill(Color.TRANSPARENT);ring.setStroke(NEON_GOLD);ring.setStrokeWidth(sf(1.5));ring.setOpacity(0.4);ring.setMouseTransparent(true);arena.getChildren().add(ring);
        int n=playerList.length;double off=-Math.PI/2;double cardOffset=sf(60),cardYOffset=sf(65);
        for(int i=0;i<n;i++){
            double angle=off+(2*Math.PI*i)/n;double px=cx+r*Math.cos(angle),py=cy+r*Math.sin(angle);
            String player=playerList[i];boolean isMe=player.equals(myUsername),isDead=deadPlayers.contains(player);
            Color cc;
            if(isDead)cc=COLOR_DEAD;
            else if(isMe)cc=COLOR_SELF;
            else if(analystFindings.containsKey(player))cc=analystFindings.get(player)?COLOR_CONFIRMED_EVIL:COLOR_CONFIRMED_GOOD;
            else cc=NEON_GOLD;
            VBox card=createAvatarCard(player,cc,isMe,false,isDead);
            card.setLayoutX(px-cardOffset);card.setLayoutY(py-cardYOffset);
            card.setMouseTransparent(true);  // tartışmada tıklanamaz
            arena.getChildren().add(card);
        }
        return arena;
    }

    // ================================================================
    //  CHAT PANELİ
    // ================================================================

    /** Sağ alt köşedeki kayan sohbet paneli. Tartışma + oylama + gece (kötüler) için kullanılır. */
    private VBox buildChatPanel(){
        // Kanal bilgisi: hangi kanaldayım?
        String channelLabel;
        Color channelColor;
        boolean canChat = canPlayerChatNow();
        if(deadPlayers.contains(myUsername)){
            channelLabel="[ ÖLÜ KANALI ]";channelColor=Color.web("#909090");
        } else if(currentPhaseLabel.equals("GECE")){
            channelLabel=amIEvil?"[ KÖTÜ TAKIM ]":"[ GECE — SESSİZLİK ]";
            channelColor=amIEvil?NEON_RED:COLOR_DEAD;
        } else {
            channelLabel="[ GÜNDÜZ — HERKES ]";channelColor=NEON_GOLD;
        }

        VBox panel=new VBox(sy(6));panel.setPadding(new Insets(sy(10),sx(12),sy(10),sx(12)));
        panel.setMinWidth(sx(340));panel.setMaxWidth(sx(380));panel.setMaxHeight(sy(280));
        panel.setStyle("-fx-background-color:rgba(8,8,16,0.88);-fx-background-radius:12;-fx-border-color:"+toHex(channelColor)+";-fx-border-width:1.2;-fx-border-radius:12;");
        DropShadow g=new DropShadow(sf(15),channelColor);g.setSpread(0.05);panel.setEffect(g);

        // Başlık
        Label header=new Label("◆ SOHBET  "+channelLabel);
        header.setTextFill(channelColor);header.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(11)));

        // Mesaj listesi
        chatMessagesBox=new VBox(sy(3));
        chatMessagesBox.setPadding(new Insets(sy(4)));
        chatScroll=new javafx.scene.control.ScrollPane(chatMessagesBox);
        chatScroll.setFitToWidth(true);chatScroll.setPrefHeight(sy(180));
        chatScroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-border-color:transparent;");
        chatScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        // Mevcut geçmişi göster
        for(Network.ChatBroadcastPacket msg : chatHistory){
            chatMessagesBox.getChildren().add(buildChatRow(msg));
        }

        // Input
        chatInputField=new TextField();
        chatInputField.setPromptText(canChat?"mesaj yaz...":"konuşamazsın");
        chatInputField.setFont(Font.font(FONT_MONO,sf(11)));
        chatInputField.setStyle("-fx-background-color:rgba(0,0,0,0.5);-fx-text-fill:#e0e8e8;-fx-prompt-text-fill:#505060;-fx-border-color:"+toHex(channelColor)+";-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:0 8;");
        chatInputField.setDisable(!canChat);
        chatInputField.setOnAction(e->sendChatMessage());

        panel.getChildren().addAll(header,chatScroll,chatInputField);

        // En altta scroll'u tut
        chatScroll.setVvalue(1.0);
        return panel;
    }

    /** Bu oyuncu şu an konuşabilir mi? */
    private boolean canPlayerChatNow(){
        boolean imDead = deadPlayers.contains(myUsername);
        if(lastWordsActive) return true;  // Son sözler süresi — DAY kanalına yazabilir
        if(imDead) return true; // ölü kanalı
        if(currentPhaseLabel.equals("GECE")) return amIEvil; // gece sadece kötüler
        return true; // gündüz herkes
    }

    /** Chat input'unun aktif/pasif durumunu yeniden hesapla (last words için) */
    private void refreshChatInputState(){
        if(chatInputField==null) return;
        boolean canChat = canPlayerChatNow();
        chatInputField.setDisable(!canChat);
        if(lastWordsActive){
            chatInputField.setPromptText("SON SÖZLERİN... ("+lastWordsRemaining+"sn)");
        } else if(canChat){
            chatInputField.setPromptText("mesaj yaz...");
        } else {
            chatInputField.setPromptText("konuşamazsın");
        }
    }

    /** Yeni mesaj geldiğinde çağrılır */
    private void appendChatMessage(Network.ChatBroadcastPacket msg){
        chatHistory.add(msg);
        // En fazla 100 mesaj tut (memory leak engeli)
        if(chatHistory.size()>100) chatHistory.remove(0);

        if(chatMessagesBox==null) return;
        chatMessagesBox.getChildren().add(buildChatRow(msg));
        if(chatMessagesBox.getChildren().size()>100)
            chatMessagesBox.getChildren().remove(0);

        // Otomatik scroll en alta
        if(chatScroll!=null){
            // Bir frame sonra scroll yap ki layout güncellensin
            Platform.runLater(()->chatScroll.setVvalue(1.0));
        }
    }

    /** Tek mesaj satırı */
    private javafx.scene.layout.HBox buildChatRow(Network.ChatBroadcastPacket msg){
        javafx.scene.layout.HBox row=new javafx.scene.layout.HBox(sx(6));
        row.setAlignment(Pos.TOP_LEFT);

        Color senderColor;
        switch(msg.channel){
            case "SYSTEM": senderColor=NEON_GOLD; break;
            case "EVIL":   senderColor=NEON_RED;  break;
            case "DEAD":   senderColor=Color.web("#909090"); break;
            default:       senderColor=msg.fromDead?Color.web("#909090"):NEON_CYAN; break;
        }
        // Kendi mesajım vurgu
        if(msg.sender.equals(myUsername)){
            senderColor=NEON_GREEN;
        }

        Label senderLbl=new Label(msg.sender+":");senderLbl.setTextFill(senderColor);
        senderLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(11)));
        senderLbl.setMinWidth(sx(80));senderLbl.setMaxWidth(sx(100));

        Label msgLbl=new Label(msg.message);msgLbl.setTextFill(Color.web("#d8e0e8"));
        msgLbl.setFont(Font.font(FONT_MONO,sf(11)));msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(sx(230));

        row.getChildren().addAll(senderLbl,msgLbl);
        return row;
    }

    /** Mesaj gönder */
    private void sendChatMessage(){
        if(chatInputField==null||client==null||!client.isConnected()) return;
        String text=chatInputField.getText().trim();
        if(text.isEmpty()) return;

        Network.ChatMessagePacket pkt=new Network.ChatMessagePacket();
        pkt.message=text;
        // Kanal: son sözler aktifse DAY (sunucu zaten kontrol ediyor), diğerleri normal
        if(lastWordsActive)                       pkt.channel="DAY";
        else if(deadPlayers.contains(myUsername)) pkt.channel="DEAD";
        else if(currentPhaseLabel.equals("GECE")) pkt.channel="EVIL";
        else                                       pkt.channel="DAY";

        client.sendTCP(pkt);
        chatInputField.clear();
    }

    // ================================================================
    //  FAZ ZAMANLAYICISI
    // ================================================================

    private Label buildPhaseTimerLabel(int seconds, Color color){
        Label lbl=new Label("⏱ "+seconds+"s");
        lbl.setTextFill(color);lbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(18)));
        DropShadow g=new DropShadow(sf(12),color);g.setSpread(0.2);lbl.setEffect(g);
        lbl.setMouseTransparent(true);
        return lbl;
    }

    private void startPhaseTimer(int totalSeconds, Color color){
        if(phaseTimerAnim!=null) phaseTimerAnim.stop();
        if(phaseTimerLabel==null) return;
        int[] remaining={totalSeconds};
        phaseTimerLabel.setText("⏱ "+remaining[0]+"s");
        phaseTimerAnim=new Timeline(new KeyFrame(Duration.seconds(1),e->{
            remaining[0]--;
            if(remaining[0]>=0) phaseTimerLabel.setText("⏱ "+remaining[0]+"s");
            // Son 10 saniye kırmızı yanıp sönsün
            if(remaining[0]<=10&&remaining[0]>0){
                phaseTimerLabel.setTextFill(NEON_RED);
                phaseTimerLabel.setEffect(new DropShadow(sf(15),NEON_RED));
            }
        }));
        phaseTimerAnim.setCycleCount(totalSeconds);
        phaseTimerAnim.play();
    }

    private void showVoteResultThenNight(String resultMessage){
        StackPane root=createDayBackground();
        VBox content=new VBox(sy(24));content.setAlignment(Pos.CENTER);
        Label title=createNeonTitle("// OY SONUCU",NEON_GOLD,40);
        VBox resultBox=createGlassPanel(NEON_GOLD);resultBox.setMaxWidth(sx(640));
        Label msg=new Label(resultMessage);msg.setTextFill(Color.web("#fff8c4"));msg.setFont(Font.font(FONT_MONO,sf(16)));msg.setWrapText(true);msg.setMaxWidth(sx(580));resultBox.getChildren().add(msg);
        Label countdown=new Label("gece başlıyor...");countdown.setTextFill(Color.web("#606070"));countdown.setFont(Font.font(FONT_MONO,sf(13)));
        content.getChildren().addAll(title,resultBox,countdown);root.getChildren().add(content);
        switchScene(root);
        PauseTransition pause=new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e->showGameScreen(myRole,amIEvil,currentPlayers));pause.play();
    }

    // ================================================================
    //  EKRAN 5: GAME OVER — Yeniden başlatma butonu ile
    // ================================================================
    // Eski imza için backward compat (kullanılmıyor ama dursun)
    public void showGameOverScreen(String winnerMessage){
        showGameOverScreen(winnerMessage,new String[0],new String[0],new boolean[0],new boolean[0]);
    }

    public void showGameOverScreen(String winnerMessage,String[] playerNames,String[] playerRoles,boolean[] playerEvil,boolean[] playerAlive){
        boolean goodWon=winnerMessage.contains("İYİLER")||winnerMessage.contains("GÜVENDE");
        Color themeColor=goodWon?NEON_CYAN:NEON_RED;
        playSound(goodWon?soundWin:soundLose);

        StackPane root=goodWon?createDayBackground():createNightBackground();
        VBox content=new VBox(sy(18));content.setAlignment(Pos.CENTER);content.setPadding(new Insets(sy(20)));

        Label icon=new Label(goodWon?"✓":"✕");icon.setTextFill(themeColor);icon.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(70)));
        DropShadow ig=new DropShadow(sf(40),themeColor);ig.setSpread(0.4);icon.setEffect(ig);
        Label title=createNeonTitle("// OYUN BİTTİ",themeColor,38);
        VBox resultBox=createGlassPanel(themeColor);resultBox.setMaxWidth(sx(660));
        Label msg=new Label(winnerMessage);msg.setTextFill(themeColor);msg.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(18)));msg.setWrapText(true);msg.setMaxWidth(sx(600));resultBox.getChildren().add(msg);

        // Rol listesi paneli
        VBox roleListBox=null;
        if(playerNames!=null&&playerNames.length>0){
            roleListBox=createGlassPanel(Color.web("#808090"));
            roleListBox.setMaxWidth(sx(560));roleListBox.setSpacing(sy(6));
            Label header=new Label("ROL AÇIKLAMASI");header.setTextFill(Color.web("#a0a0b0"));header.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
            roleListBox.getChildren().add(header);
            Rectangle sep=new Rectangle(sx(400),1);sep.setFill(Color.web("#606070"));sep.setOpacity(0.5);
            roleListBox.getChildren().add(sep);

            for(int i=0;i<playerNames.length;i++){
                HBox row=new HBox(sx(12));row.setAlignment(Pos.CENTER_LEFT);
                Color rowColor=playerEvil[i]?NEON_RED:NEON_CYAN;
                String status=playerAlive[i]?"✓ HAYATTA":"✕ SİLİNDİ";

                Label nameLbl=new Label(playerNames[i]);nameLbl.setTextFill(rowColor);
                nameLbl.setFont(Font.font(FONT_MONO,FontWeight.BOLD,sf(13)));
                nameLbl.setMinWidth(sx(120));

                Label roleLbl=new Label("→ "+playerRoles[i]);roleLbl.setTextFill(rowColor);
                roleLbl.setFont(Font.font(FONT_MONO,sf(12)));roleLbl.setOpacity(0.9);
                roleLbl.setMinWidth(sx(200));

                Label statusLbl=new Label(status);
                statusLbl.setTextFill(playerAlive[i]?NEON_GREEN:COLOR_DEAD);
                statusLbl.setFont(Font.font(FONT_MONO,sf(11)));

                row.getChildren().addAll(nameLbl,roleLbl,statusLbl);
                roleListBox.getChildren().add(row);
            }
        }

        Label roundInfo=new Label("Toplam " + currentRound + " turda tamamlandı.");
        roundInfo.setTextFill(Color.web("#808090"));roundInfo.setFont(Font.font(FONT_MONO,sf(12)));

        Button restartBtn=createNeonButton("» YENİ OYUN «",NEON_GREEN,NEON_GREEN);
        restartBtn.setOnAction(e->{
            if(client!=null&&client.isConnected()){
                client.sendTCP(new Network.RestartRequestPacket());
            }
        });

        ScaleTransition pulse=new ScaleTransition(Duration.seconds(1),icon);
        pulse.setFromX(1);pulse.setFromY(1);pulse.setToX(1.08);pulse.setToY(1.08);pulse.setCycleCount(Animation.INDEFINITE);pulse.setAutoReverse(true);pulse.play();

        content.getChildren().addAll(icon,title,resultBox);
        if(roleListBox!=null) content.getChildren().add(roleListBox);
        content.getChildren().addAll(roundInfo,restartBtn);
        root.getChildren().add(content);
        switchScene(root);
    }
}