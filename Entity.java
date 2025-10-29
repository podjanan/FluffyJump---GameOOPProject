import java.awt.Graphics2D;

public interface Entity {
    void update(double gameSpeed);
    void draw(Graphics2D g2);
    default boolean isOffscreen(int panelW, int panelH) { return false; }
}
