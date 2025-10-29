import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class UFO {
    private double x, y;
    private int width, height;
    private final int groundY;
    private double wobbleT = 0;

    private static BufferedImage UFO_IMG;
    private static final String PATH = "/sprites/obstacles/ufo.png";

    public UFO(double startX, int groundY) {
        this.groundY = groundY;
        ensureLoaded();

        if (UFO_IMG != null) {
            width  = (int)Math.round(UFO_IMG.getWidth()  * 0.08);
            height = (int)Math.round(UFO_IMG.getHeight() * 0.08);
        } else {
            width = 90; height = 50;
        }

        this.x = startX;
        this.y = groundY - height;
    }

    private static void ensureLoaded() {
        if (UFO_IMG != null) return;
        try {
            var url = UFO.class.getResource(PATH);
            if (url != null) UFO_IMG = ImageIO.read(url);
        } catch (Exception ignored) { UFO_IMG = null; }
    }

    public void update(int speed) {
        x -= speed + 1.0;
        wobbleT += 0.12;
    }

    public void draw(Graphics2D g2) {
        int ix = (int)Math.round(x);
        int iy = (int)Math.round(y - 6 * Math.sin(wobbleT));

        if (UFO_IMG != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(UFO_IMG, ix, iy, width, height, null);
        } else {
            g2.setColor(new Color(120, 200, 80));
            g2.fillOval(ix, iy + height/3, width, height/2);
            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(ix + width/8, iy + height/2, width*3/4, height/3);
        }
    }

    public boolean isOffscreenLeft(int bufferPx) { return x + width < -bufferPx; }
    public Rectangle getBounds() { return new Rectangle((int)Math.round(x), (int)Math.round(y), width, height); }

    public int getX() { return (int)Math.round(x); }
    public int getY() { return (int)Math.round(y); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }

    // resize helper
    public void shiftY(double dy) { this.y += dy; }
}
