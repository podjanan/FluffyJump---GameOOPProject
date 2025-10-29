import java.awt.*;


public class BonusCoin extends Coin {
    public BonusCoin(double x, double y) {
        super(x, y);
    }

    @Override
    public void draw(Graphics2D g2) {
        
        super.draw(g2);

        
        int r  = (int)Math.round(getRadius());
        int cx = (int)Math.round(getCenterX());
        int cy = (int)Math.round(getCenterY());

        g2.setColor(new Color(127, 11, 41)); // ม่วงโปร่ง
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
    }
}
