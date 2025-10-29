import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class RunnerGame extends JPanel implements KeyListener {

    public enum Level { EARTH, PLANET }

    private static final int GROUND_Y = 450; // พื้นคงที่ (ไม่ขยับตาม resize)
    private static final int SPEED_BASE = 4;
    private static final double SPEED_UP_PER_SEC = 0.20;
    private static final int TARGET_FPS = 60;
    private static final long TARGET_FRAME_NANOS = 1_000_000_000L / TARGET_FPS;

    private int maxHp = 3;
    private int hp = maxHp;
    private static final long INVINCIBLE_NANOS = 1_000_000_000L;
    private long invincibleUntilNanos = 0;

    private final Level level;
    private Player player;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Coin> coins;
    private ArrayList<RollingRock> rocks;

    // Planet only
    private ArrayList<Meteor> meteors;
    private ArrayList<UFO> ufos;
    private int meteorCooldown = 0;
    private int ufoCooldown = 0;
    private int[][] stars;
    private Image planetGroundImg;

    private Random random;

    private int score = 0;
    private int gameSpeed = SPEED_BASE;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean won = false;

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private int obstacleCooldown = 0;
    private int coinCooldown = 0;
    private int rockCooldown = 0;

    private int coinsCollected = 0;
    private int nextSpeedUpAt = 10;

    private static final int MAX_OBS_ON_SCREEN = 6;
    private static final int MAX_COINS_ON_SCREEN = 10;
    private static final int MAX_ROCKS_ON_SCREEN = 4;

    private static final long SCORE_PENALTY_COOLDOWN_NANOS = 600_000_000L;
    private long scorePenaltyUntilNanos = 0;

    // Earth scene
    private Image cloudImg;
    private Image groundImg;

    // Game loop
    private volatile boolean running = false;
    private Thread gameThread;

    // ปุ่มกลับหน้าแรก (มุมขวาบน)
    private JButton backButton;

    public RunnerGame() { this(Level.EARTH); }

    public RunnerGame(Level level) {
        this.level = level;
        setPreferredSize(new Dimension(800, 600));
        setBackground(level == Level.EARTH ? new Color(140, 235, 255) : new Color(5, 10, 25));
        setFocusable(true);
        setDoubleBuffered(true);
        setLayout(null);
        addKeyListener(this);

        initGame();
        addBackButton();
        startGameLoop60();
    }
    private void drawWinOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        String text = "WIN!";
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (getWidth() - tw) / 2, 220);

        g2.dispose();
    }
    private void checkWin() {
        if (!won && score >= 100) {
            won = true;
        }
    }
    private void initGame() {
        player = new Player(100, GROUND_Y);
        player.setPlayArea(0, getGameWidth());

        obstacles = new ArrayList<>();
        coins     = new ArrayList<>();
        rocks     = new ArrayList<>();
        meteors   = new ArrayList<>();
        ufos      = new ArrayList<>();
        random    = new Random();

        score          = 0;
        coinsCollected = 0;
        nextSpeedUpAt  = 10;
        

        gameSpeed = SPEED_BASE;
        gameOver  = false;
        paused    = false;
        won = false;

        hp        = maxHp;
        invincibleUntilNanos = 0;

        obstacleCooldown = 30;
        coinCooldown     = 20;
        rockCooldown     = 40;
        meteorCooldown   = 30;
        ufoCooldown      = 50;
        scorePenaltyUntilNanos = 0;

        if (level == Level.EARTH) {
            cloudImg  = loadImageTwoWays("/sprites/sky/cloud.png",     "sprites/sky/cloud.png");
            groundImg = loadImageTwoWays("/sprites/ground/ground.png", "sprites/ground/ground.png");
            planetGroundImg = null;
            stars = null;
        } else {
            cloudImg = null; groundImg = null;
            planetGroundImg = loadImageTwoWays(
                    "/sprites/ground/planet_ground.png",
                    "sprites/ground/planet_ground.png"
            );
            stars = new int[120][2];
            for (int i = 0; i < stars.length; i++) {
                stars[i][0] = random.nextInt(Math.max(1, getGameWidth()));
                stars[i][1] = random.nextInt(Math.max(1, GROUND_Y - 40));
            }
        }
    }

    private Image loadImageTwoWays(String classpathPath, String filePath) {
        try { URL url = getClass().getResource(classpathPath); if (url != null) return ImageIO.read(url); }
        catch (IOException ignored) {}
        try { File f = new File(filePath); if (f.exists()) return ImageIO.read(f); }
        catch (IOException ignored) {}
        return null;
    }

    private void addBackButton() {
        backButton = new JButton("Back to Menu");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        backButton.setBackground(new Color(45, 45, 45));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusable(false);
        backButton.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));

        positionBackButton();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { positionBackButton(); }
        });

        backButton.addActionListener(ev -> {
            JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (top != null) {
                top.setContentPane(new MainMenu(top));
                top.revalidate();
                top.repaint();
            }
            stopGameLoop();
        });

        add(backButton);
    }

    private void positionBackButton() {
        int margin = 12;
        int w = 150, h = 32;
        int x = Math.max(margin, getWidth() - w - margin);
        int y = margin;
        backButton.setBounds(x, y, w, h);
        backButton.setVisible(getWidth() >= w + margin && getHeight() >= h + margin);
    }

    private void startGameLoop60() {
        if (running) return;
        running = true;
        gameThread = new Thread(() -> {
            long nextFrameTime = System.nanoTime();
            long lastTime      = nextFrameTime;
            while (running) {
                long now = System.nanoTime();
                if (now >= nextFrameTime) {
                    double dt = (now - lastTime) / 1_000_000_000.0;
                    lastTime = now;

                    if (!paused && !gameOver && !won) tick(dt);
                    repaint();

                    nextFrameTime += TARGET_FRAME_NANOS;
                    long sleepNanos = nextFrameTime - System.nanoTime();
                    if (sleepNanos > 2_000_000) {
                        try { Thread.sleep(sleepNanos / 1_000_000, (int)(sleepNanos % 1_000_000)); } catch (InterruptedException ignored) {}
                    }
                    while ((sleepNanos = nextFrameTime - System.nanoTime()) > 0) { Thread.onSpinWait(); }
                } else {
                    nextFrameTime = now;
                    lastTime = now;
                }
            }
        }, "GameLoop-60fps");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void stopGameLoop() { running = false; if (gameThread != null) gameThread.interrupt(); }

    private void tick(double dt) {
        player.setPlayArea(0, getGameWidth());

        double newSpeed = gameSpeed + SPEED_UP_PER_SEC * dt;
        gameSpeed = (int)Math.round(newSpeed);
        if (gameSpeed < SPEED_BASE) gameSpeed = SPEED_BASE;

        player.update(leftPressed, rightPressed);

        if (level == Level.EARTH) {
            if (obstacleCooldown > 0) obstacleCooldown--;
            if (coinCooldown > 0)     coinCooldown--;
            if (rockCooldown > 0)     rockCooldown--;

            if (obstacleCooldown <= 0 && obstacles.size() < MAX_OBS_ON_SCREEN) {
                if (random.nextInt(9) == 0) {
                    obstacles.add(new Obstacle(getGameWidth(), GROUND_Y));
                    obstacleCooldown = randomRange(45, 90);
                }
            }
            if (rockCooldown <= 0 && rocks.size() < MAX_ROCKS_ON_SCREEN) {
                if (random.nextInt(7) == 0) {
                    rocks.add(new RollingRock(getGameWidth(), GROUND_Y));
                    rockCooldown = randomRange(60, 120);
                }
            }
        } else { // PLANET
            if (meteorCooldown > 0) meteorCooldown--;
            if (ufoCooldown > 0)    ufoCooldown--;
            if (coinCooldown > 0)   coinCooldown--;

            if (meteorCooldown <= 0) {
                if (random.nextInt(8) == 0) {
                    int sx = random.nextInt(Math.max(1, getGameWidth() - 40));
                    meteors.add(new Meteor(sx, -60, gameSpeed));
                    meteorCooldown = randomRange(35, 60);
                }
            }
            if (ufoCooldown <= 0 && ufos.size() < 3) {
                if (random.nextInt(6) == 0) {
                    ufos.add(new UFO(getGameWidth() + 20, GROUND_Y));
                    ufoCooldown = randomRange(70, 120);
                }
            }
        }

        if (coinCooldown <= 0 && coins.size() < MAX_COINS_ON_SCREEN) {
            if (random.nextInt(5) == 0) {
                int y = (level == Level.PLANET)
                        ? (GROUND_Y - 26 - 8)
                        : (GROUND_Y - (60 + random.nextInt(120)));

                // ~5% โอกาสเกิด BonusCoin (1 ใน 20) — ปรับตัวเลขได้ตามต้องการ
                if (random.nextInt(10) == 0) {
                    coins.add(new BonusCoin(getGameWidth(), y));
                } else {
                    coins.add(new Coin(getGameWidth(), y));
                }
                checkWin();
                coinCooldown = randomRange(25, 60);
            }
        }

        // Update/Collide
        if (level == Level.EARTH) {
            for (Iterator<Obstacle> it = obstacles.iterator(); it.hasNext(); ) {
                Obstacle o = it.next();
                o.update(gameSpeed);
                if (o.getX() + o.getWidth() < 0) { it.remove(); continue; }
                if (checkPlayerHitObstacle(o)) {
                    long now = System.nanoTime();
                    if (now >= invincibleUntilNanos) {
                        hp--; invincibleUntilNanos = now + INVINCIBLE_NANOS;
                        if (hp <= 0) gameOver = true;
                    }
                }
            }
            for (Iterator<RollingRock> it = rocks.iterator(); it.hasNext(); ) {
                RollingRock r = it.next();
                r.update(gameSpeed);
                if (r.isOffscreenLeft(80)) { it.remove(); continue; }
                if (checkPlayerHitRock(r)) {
                    long now = System.nanoTime();
                    if (now >= scorePenaltyUntilNanos) {
                        score = Math.max(0, score - 50);
                        scorePenaltyUntilNanos = now + SCORE_PENALTY_COOLDOWN_NANOS;
                    }
                }
            }
        } else {
            for (Iterator<Meteor> it = meteors.iterator(); it.hasNext(); ) {
                Meteor m = it.next();
                m.update();
                if (m.isOffscreen(getGameWidth(), getGameHeight(), 80)) { it.remove(); continue; }
                if (collideCircle(player.getX()+25, player.getY()+25, 25, m.cx(), m.cy(), m.r())) {
                    long now = System.nanoTime();
                    if (now >= scorePenaltyUntilNanos) {
                        score = Math.max(0, score - 50);
                        scorePenaltyUntilNanos = now + SCORE_PENALTY_COOLDOWN_NANOS;
                    }
                }
            }
            for (Iterator<UFO> it = ufos.iterator(); it.hasNext(); ) {
                UFO u = it.next();
                u.update(gameSpeed);
                if (u.isOffscreenLeft(80)) { it.remove(); continue; }
                Rectangle pr = new Rectangle(player.getX(), player.getY(), 50, 50);
                if (pr.intersects(u.getBounds())) {
                    long now = System.nanoTime();
                    if (now >= invincibleUntilNanos) {
                        hp--; invincibleUntilNanos = now + INVINCIBLE_NANOS;
                        if (hp <= 0) gameOver = true;
                    }
                }
            }
        }

        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin c = coins.get(i);

            // อัปเดตตำแหน่งเหรียญ
            c.update(gameSpeed);

            // ถ้าออกซ้ายจอ มี margin 80 ก็ลบทิ้งแล้วข้ามตัวถัดไป
            if (c.isOffscreenLeft(80)) {
                coins.remove(i);
                continue;
            }

            // ชนกับผู้เล่นไหม
            if (checkPlayerTakeCoin(c)) {
                if (c instanceof BonusCoin) {
                    // โบนัส: +หัวใจ (ไม่เกิน max) และ +30 คะแนน
                    hp = Math.min(maxHp, hp + 1);
                    score += 30;
                } else {
                    // เหรียญปกติ: +10 คะแนน
                    score += 10;
                }

                // ลบเหรียญที่เก็บแล้ว
                coins.remove(i);

                // นับเหรียญและเร่งความเร็วเป็นช่วง ๆ
                coinsCollected++;
                if (coinsCollected >= nextSpeedUpAt) {
                    gameSpeed++;
                    nextSpeedUpAt += 10;
                }
            }
        }


    }

    private boolean checkPlayerHitObstacle(Obstacle o) {
        Rectangle pr = new Rectangle(player.getX(), player.getY(), 50, 50);
        Rectangle or = new Rectangle(o.getX(), o.getY(), o.getWidth(), o.getHeight());
        return pr.intersects(or);
    }
    private boolean checkPlayerTakeCoin(Coin c) {
        return collideCircle(player.getX()+25, player.getY()+25, 25,
                             c.getCenterX(), c.getCenterY(), c.getRadius());
    }
    private boolean checkPlayerHitRock(RollingRock r) {
        return collideCircle(player.getX()+25, player.getY()+25, 25,
                             r.getCenterX(), r.getCenterY(), r.getRadius());
    }
    private boolean collideCircle(double ax, double ay, double ar, double bx, double by, double br) {
        double dx = ax - bx, dy = ay - by, rr = ar + br;
        return dx*dx + dy*dy <= rr*rr;
    }
    private int randomRange(int min, int maxInclusive) {
        return min + random.nextInt(maxInclusive - min + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        if (level == Level.EARTH) {
            if (cloudImg != null) {
                drawResponsiveClouds(g2);
            } else {
                g2.setColor(new Color(140, 235, 255));
                g2.fillRect(0, 0, getWidth(), GROUND_Y);
            }
            if (groundImg != null) {
                g2.drawImage(groundImg, 0, GROUND_Y, getWidth(), Math.max(0, getHeight() - GROUND_Y), null);
            } else {
                g2.setColor(new Color(60, 180, 75));
                g2.fillRect(0, GROUND_Y, getWidth(), Math.max(0, getHeight() - GROUND_Y));
            }
        } else {
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 15, 35),
                                                  0, getHeight(), new Color(0, 0, 0));
            g2.setPaint(sky);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(230, 230, 255));
            if (stars != null) for (int[] s : stars) g2.fillRect(s[0], s[1], 2, 2);
            if (planetGroundImg != null) {
                g2.drawImage(planetGroundImg, 0, GROUND_Y, getWidth(), Math.max(0, getHeight() - GROUND_Y), null);
            } else {
                g2.setColor(new Color(40, 50, 70));
                g2.fillRect(0, GROUND_Y, getWidth(), Math.max(0, getHeight() - GROUND_Y));
            }
        }

        for (Obstacle o : obstacles)   o.draw(g2);
        for (RollingRock r : rocks)    r.draw(g2);
        for (Meteor m : meteors)       m.draw(g2);
        for (UFO u : ufos)             u.draw(g2);
        for (Coin c : coins)           c.draw(g2);

        boolean invincible = System.nanoTime() < invincibleUntilNanos;
        if (invincible) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        player.draw(g2);
        if (invincible) g2.setComposite(AlphaComposite.SrcOver);

        g2.setColor(level == Level.EARTH ? Color.BLACK : Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        int hudX = 12, hudY = 24, hudGap = 22;
        g2.drawString("Score: " + score, hudX, hudY);
        g2.drawString("Speed: " + gameSpeed, hudX, hudY + hudGap);
        g2.drawString("Mode: " + level, hudX, hudY + hudGap * 2);
        drawHeartsLeft(g2, hudX, hudY + hudGap * 3 + 8);

        if (paused && !gameOver) drawPauseOverlay(g2);
        if (gameOver)            drawGameOverOverlay(g2);
        if (won)            drawWinOverlay(g2);

        Toolkit.getDefaultToolkit().sync();
        g2.dispose();
    }

    private void drawResponsiveClouds(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();

        g2.setColor(new Color(140, 235, 255));
        g2.fillRect(0, 0, w, Math.min(GROUND_Y, h));

        double[] xs = {0.12, 0.45, 0.78};
        double[] ys = {0.12, 0.09, 0.16};
        double[] ws = {0.18, 0.22, 0.16};

        for (int i = 0; i < xs.length; i++) {
            int cw = (int) Math.max(80, w * ws[i]);
            int ch = (int) (cw * 0.6);
            int cx = (int) (w * xs[i]) - cw/2;
            int cy = (int) (GROUND_Y * ys[i]);
            g2.drawImage(cloudImg, cx, cy, cw, ch, null);
        }
    }

    private void drawHeartsLeft(Graphics g, int xLeft, int topY) {
        int heartSize = 28, gap = 8;
        for (int i = 0; i < maxHp; i++) {
            drawHeart(g, xLeft + i * (heartSize + gap), topY, heartSize, i < hp);
        }
    }

    private void drawHeart(Graphics g, int x, int y, int size, boolean filled) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = size, h = size, topX = x + w/2, topY = y + h/5;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(topX, topY);
        path.curveTo(x, y, x, y + h/2, topX, y + h);
        path.curveTo(x + w, y + h/2, x + w, y, topX, topY);
        g2.setColor(new Color(220, 20, 60));
        if (filled) g2.fill(path); else { g2.setStroke(new BasicStroke(2f)); g2.draw(path); }
        g2.dispose();
    }

    private void drawPauseOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        String text = "PAUSED";
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (getWidth() - tw) / 2, 220);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        String tip = "Press P to Resume";
        int tw2 = g2.getFontMetrics().stringWidth(tip);
        g2.drawString(tip, (getWidth() - tw2) / 2, 260);
        g2.dispose();
    }

    private void drawGameOverOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        String text = "GAME OVER";
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (getWidth() - tw) / 2, 220);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        String tip = "Press R to Restart";
        int tw2 = g2.getFontMetrics().stringWidth(tip);
        g2.drawString(tip, (getWidth() - tw2) / 2, 260);
        g2.dispose();
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (!gameOver) {
            if (k == KeyEvent.VK_P) { paused = !paused; return; }
            if (!paused) {
                if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT)  leftPressed  = true;
                if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) rightPressed = true;
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_W || k == KeyEvent.VK_UP) player.jump();
            }
        } else if (k == KeyEvent.VK_R) {
            initGame();
        }

        if (k == KeyEvent.VK_ESCAPE) {
            JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (top != null) top.dispose();
            stopGameLoop();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT)  leftPressed  = false;
        if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) rightPressed = false;
    }

    private int getGameWidth()  { return Math.max(1, getWidth()); }
    private int getGameHeight() { return Math.max(1, getHeight()); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("RunnerGame - Resizable");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            RunnerGame game = new RunnerGame(Level.EARTH);
            f.setContentPane(game);
            f.setResizable(true);
            f.setSize(1000, 700);    // ✅ ใช้ setSize แทน pack
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
