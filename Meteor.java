import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Meteor {
    private double x, y;
    private double vx, vy;     // ความเร็วแกน x/y
    private int size;          // เส้นผ่านศูนย์กลาง
    private double angleDeg;

    private static BufferedImage METEOR_IMG;
    private static final String PATH = "/sprites/obstacles/meteor.png"; // ไฟล์ PNG ที่คุณใส่ไว้

    public Meteor(int startX, int startY, int gameSpeed) {
        ensureSprite();
        // ขนาดสุ่ม 28–56 px
        this.size = 28 + (int)(Math.random() * 29);

        // ความเร็ว: ให้ตกตรง ๆ ลงมา + เคลื่อนซ้ายเล็กน้อย
        this.vx = -(gameSpeed * 0.3);  // เลื่อนซ้ายช้า ๆ
        this.vy =  3.0 + Math.random() * 2.0; // ตกลงด้านล่างเร็วขึ้น

        this.x = startX;
        this.y = startY;
        this.angleDeg = Math.random() * 360;
    }

    private static void ensureSprite() {
        if (METEOR_IMG != null) return;
        try {
            var url = Meteor.class.getResource(PATH);
            if (url != null) METEOR_IMG = ImageIO.read(url);
        } catch (Exception ignored) {
            METEOR_IMG = null;
        }
    }

    public void update() {
        x += vx;
        y += vy;
        angleDeg += 3; 
        if (angleDeg >= 360) angleDeg -= 360;
    }

    public boolean isOffscreen(int w, int h, int buffer) {
        return x + size < -buffer || y - size > h + buffer;
    }

    public void draw(Graphics2D g2) {
        AffineTransform old = g2.getTransform();
        g2.translate(x + size / 2.0, y + size / 2.0);
        g2.rotate(Math.toRadians(angleDeg));
        if (METEOR_IMG != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(METEOR_IMG, -size / 2, -size / 2, size, size, null);
        } else {
            // fallback: วาดก้อนหินสีเทา
            g2.setColor(new Color(100, 100, 100));
            g2.fillOval(-size / 2, -size / 2, size, size);
        }
        g2.setTransform(old);
    }

    // ใช้ชนแบบวงกลม
    public double cx() { return x + size / 2.0; }
    public double cy() { return y + size / 2.0; }
    public double r()  { return size / 2.0; }
}
