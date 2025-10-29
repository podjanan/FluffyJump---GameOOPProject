import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Player {
    private int x, y;
    private int width = 50, height = 50;
    private int groundY;

    private int velocityY = 0;
    private int gravity = 1;
    private int jumpStrength = -16;
    private int maxFallSpeed = 18;
    private boolean onGround = true;

    private int jumpsUsed = 0;
    private int maxJumps = 2;

    private int maxHP = 4;
    private int currentHP = 4;
    private int invincibleTicks = 0;
    private static final int INVINCIBLE_COOLDOWN = 45;

    private int minX = 0, maxX = 800 - width;

    private Image[] runFrames;
    private Image[] jumpFrames;
    private Image   fallFrame;
    private int frameIndex = 0;
    private int frameTick = 0;
    private int runFrameInterval = 6;
    private int jumpFrameHold = 8;
    private int jumpAnimTick = 0;

    public Player(int startX, int groundY) {
        this.x = startX;
        this.groundY = groundY;
        this.y = groundY - height;
        loadSprites();
    }

    private void loadSprites() {
        try {
            runFrames = new Image[] {
                readImg("/sprites/player/run 1.png"),
                readImg("/sprites/player/run 2.png"),
                readImg("/sprites/player/run 3.png"),
                readImg("/sprites/player/run 4.png"),
                readImg("/sprites/player/run 5.png"),
                readImg("/sprites/player/run 6.png")
            };
            jumpFrames = new Image[] {
                readImg("/sprites/player/jump 1.png"),
                readImg("/sprites/player/jump 2.png")
            };
            fallFrame = readImg("/sprites/player/fall .png"); // ชื่อไฟล์มีช่องว่างตามต้นฉบับ
        } catch (RuntimeException ex) {
            runFrames = null; jumpFrames = null; fallFrame = null;
        }
    }

    private Image readImg(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null) throw new IOException("Resource not found: " + path);
            BufferedImage img = ImageIO.read(url);
            return img;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load: " + path, e);
        }
    }

    public void update(boolean leftPressed, boolean rightPressed) {
        if (leftPressed)  x -= 6;
        if (rightPressed) x += 6;
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;

        velocityY += gravity;
        if (velocityY > maxFallSpeed) velocityY = maxFallSpeed;
        y += velocityY;

        if (y + height >= groundY) {
            y = groundY - height;
            velocityY = 0;
            onGround = true;
            jumpsUsed = 0;
            jumpAnimTick = 0;
        } else {
            onGround = false;
        }

        if (invincibleTicks > 0) invincibleTicks--;

        if (onGround) {
            frameTick++;
            if (frameTick >= runFrameInterval) {
                frameTick = 0;
                if (runFrames != null && runFrames.length > 0) {
                    frameIndex = (frameIndex + 1) % runFrames.length;
                }
            }
        } else {
            if (velocityY < 0 && jumpFrames != null) {
                jumpAnimTick++;
                int phase = Math.min(jumpFrames.length - 1, jumpAnimTick / jumpFrameHold);
                frameIndex = phase;
            } else {
                frameIndex = 0;
            }
        }
    }

    public void jump() {
        if (jumpsUsed < maxJumps) {
            velocityY = jumpStrength;
            onGround = false;
            jumpsUsed++;
            jumpAnimTick = 0;
        }
    }

    public void draw(Graphics g) {
        if (invincibleTicks > 0 && (invincibleTicks / 5) % 2 == 0) {
            g.setColor(new Color(255, 255, 0, 90));
            g.fillRect(x - 4, y - 4, width + 8, height + 8);
        }

        Image spriteToDraw = null;
        if (!onGround) {
            if (velocityY < 0 && jumpFrames != null) {
                int idx = Math.min(jumpFrames.length - 1, frameIndex);
                spriteToDraw = jumpFrames[idx];
            } else if (fallFrame != null) {
                spriteToDraw = fallFrame;
            }
        } else if (runFrames != null) {
            spriteToDraw = runFrames[Math.max(0, Math.min(frameIndex, runFrames.length - 1))];
        }

        if (spriteToDraw != null) {
            g.drawImage(spriteToDraw, x, y, width, height, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, width, height);
        }
    }

    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    public double getCenterX() { return x + width / 2.0; }
    public double getCenterY() { return y + height / 2.0; }
    public double getRadius()  { return Math.min(width, height) / 2.2; }

    public int getMaxHP() { return maxHP; }
    public int getCurrentHP() { return currentHP; }
    public void decreaseHP(int amount) {
        if (invincibleTicks > 0) return;
        currentHP = Math.max(0, currentHP - amount);
        invincibleTicks = INVINCIBLE_COOLDOWN;
    }
    public boolean isDead() { return currentHP <= 0; }

    public void reset(int startX, int groundY) {
        this.x = startX;
        this.groundY = groundY;
        this.y = groundY - height;
        this.velocityY = 0;
        this.currentHP = maxHP;
        this.invincibleTicks = 0;
        this.onGround = true;
        this.frameIndex = 0;
        this.frameTick = 0;
        this.jumpAnimTick = 0;
        this.jumpsUsed = 0;
    }

    public void setPlayArea(int minX, int maxX) {
        this.minX = minX;
        this.maxX = Math.max(minX, maxX - width);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    // resize helper (สำคัญ)
    public void adjustForNewGround(int newGroundY) {
        int dy = newGroundY - this.groundY;
        this.groundY = newGroundY;
        this.y += dy;
    }
}
