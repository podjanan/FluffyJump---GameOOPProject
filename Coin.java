import java.awt.*;
import java.awt.geom.AffineTransform;

public class Coin implements Entity {
    private double x;
    private double y;
    private int size = 26;
    private double spinDeg = 0.0;

    public Coin(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // ==== Entity.update ====
    @Override
    public void update(double gameSpeed) {
        x -= gameSpeed;
        spinDeg += 3.6;
        if (spinDeg >= 360) spinDeg -= 360;
    }

    // ==== Entity.draw ====
    @Override
    public void draw(Graphics2D g2) {
        AffineTransform old = g2.getTransform();
        g2.translate(x + size / 2.0, y + size / 2.0);
        g2.rotate(Math.toRadians(spinDeg));

        int r = size / 2;
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(-r, -r, size, size);

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(220, 170, 0));
        g2.drawOval(-r, -r, size, size);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(-r/2, -r, r, r);

        g2.setTransform(old);
    }

    // ==== Entity.isOffscreen ====
    @Override
    public boolean isOffscreen(int panelW, int panelH) {
        return x + size < -80; // ใช้ buffer 80 แบบเดียวกับของเดิม
    }

    // ----- utilities เดิม -----
    public boolean isOffscreenLeft(int bufferPx) { return x + size < -bufferPx; }

    // geometry
    public double getCenterX() { return x + size / 2.0; }
    public double getCenterY() { return y + size / 2.0; }
    public double getRadius()  { return size / 2.0; }

    // resize helper
    public void shiftY(double dy) { this.y += dy; }
}
