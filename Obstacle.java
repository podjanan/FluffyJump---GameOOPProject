import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

public class Obstacle {
    private double x, y;
    private int width, height;
    private final int groundY;

    private static BufferedImage CACTUS_IMG;
    private static final String CACTUS_PATH = "/sprites/obstacles/cactus.png";

    public Obstacle(double startX, int groundY) {
        this.groundY = groundY;
        ensureSpriteLoaded();

        if (CACTUS_IMG != null) {
            int baseW = CACTUS_IMG.getWidth();
            int baseH = CACTUS_IMG.getHeight();
            int targetH = ThreadLocalRandom.current().nextInt(50, 91);
            int targetW = (int)Math.max(20, Math.round((targetH / (double)baseH) * baseW));
            this.width  = targetW;
            this.height = targetH;
        } else {
            this.width  = ThreadLocalRandom.current().nextInt(20, 40);
            this.height = ThreadLocalRandom.current().nextInt(40, 70);
        }

        this.x = startX;
        this.y = groundY - this.height;
    }

    private static void ensureSpriteLoaded() {
        if (CACTUS_IMG != null) return;
        try {
            var url = Obstacle.class.getResource(CACTUS_PATH);
            if (url != null) CACTUS_IMG = ImageIO.read(url);
        } catch (Exception e) {
            CACTUS_IMG = null;
        }
    }

    public void update(int speed) { x -= speed; }

    public void draw(Graphics g) {
        int ix = (int)Math.round(x);
        int iy = (int)Math.round(y);
        if (CACTUS_IMG != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(CACTUS_IMG, ix, iy, width, height, null);
            g2.dispose();
        } else {
            g.setColor(new Color(34, 177, 76));
            g.fillRect(ix, iy, width, height);
        }
    }

    public Rectangle getBounds() { return new Rectangle((int)Math.round(x), (int)Math.round(y), width, height); }
    public boolean isOffscreenLeft(int bufferPx) { return x + width < -bufferPx; }

    public int getX() { return (int)Math.round(x); }
    public int getY() { return (int)Math.round(y); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }

    // resize helper
    public void shiftY(double dy) { this.y += dy; }
}
