import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class RollingRock {
    private double x, y;
    private int size;
    private double angleDeg = 0.0;

    private static BufferedImage ROCK_IMG;
    private static final String ROCK_PATH = "/sprites/obstacles/rock.png";

    public RollingRock(double startX, int groundY) {
        ensureSpriteLoaded();
        this.size = 32 + (int)(Math.random() * 25);
        this.x = startX;
        this.y = groundY - size;
    }

    private static void ensureSpriteLoaded() {
        if (ROCK_IMG != null) return;
        try {
            var url = RollingRock.class.getResource(ROCK_PATH);
            if (url != null) ROCK_IMG = ImageIO.read(url);
        } catch (Exception ignored) { ROCK_IMG = null; }
    }

    public void update(int speed) {
        x -= speed + 1.5;
        angleDeg += Math.max(2, speed * 2.2);
        if (angleDeg >= 360) angleDeg -= 360;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int cx = (int)Math.round(x + size / 2.0);
        int cy = (int)Math.round(y + size / 2.0);

        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(angleDeg));

        if (ROCK_IMG != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(ROCK_IMG, -size/2, -size/2, size, size, null);
        } else {
            g2.setColor(new Color(120, 120, 120));
            g2.fillOval(-size/2, -size/2, size, size);
            g2.setColor(new Color(90, 90, 90));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(-size/3, 0, size/3, 0);
            g2.drawArc(-size/2 + 4, -size/2 + 4, size - 8, size - 8, 40, 100);
        }

        g2.setTransform(old);
        g2.dispose();
    }

    public boolean isOffscreenLeft(int bufferPx) { return x + size < -bufferPx; }

    public double getCenterX() { return x + size / 2.0; }
    public double getCenterY() { return y + size / 2.0; }
    public double getRadius()  { return size / 2.0; }

    public int getX() { return (int)Math.round(x); }
    public int getY() { return (int)Math.round(y); }
    public int getSize() { return size; }

    // resize helper
    public void shiftY(double dy) { this.y += dy; }
}
