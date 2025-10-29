import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenu extends JPanel implements ActionListener {
    private JButton earthButton;
    private JButton planetButton;
    private JButton guideButton;
    private JButton creditButton;
    private JFrame parentFrame;

    // Colors
    private final Color skyBlue    = new Color(135, 206, 250);
    private final Color lightGreen = new Color(144, 238, 144);
    private final Color darkGreen  = new Color(60, 179, 113);
    private final Color purple     = new Color(138, 43, 226);
    private final Color white      = Color.WHITE;

    public MainMenu(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setDoubleBuffered(true);
        setOpaque(true);

        initializeComponents();

        // รีเลย์เอาต์ทุกครั้งที่รีไซซ์
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutButtons();
                revalidate();
                repaint();
            }
        });
        layoutButtons();
    }

    private void initializeComponents() {
        earthButton  = createStyledButton("Earth");
        planetButton = createStyledButton("Planet");
        guideButton  = createStyledButton("Guide");
        creditButton = createStyledButton("Credit");

        earthButton.addActionListener(this);
        planetButton.addActionListener(this);
        guideButton.addActionListener(this);
        creditButton.addActionListener(this);

        add(earthButton);
        add(planetButton);
        add(guideButton);
        add(creditButton);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(purple);
        button.setForeground(white);
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(purple.darker()); }
            @Override public void mouseExited (MouseEvent e) { button.setBackground(purple); }
        });
        return button;
    }

    
    private void layoutButtons() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());

        int btnW = Math.max(180, Math.min(360, (int)(w * 0.35)));
        int btnH = Math.max(48,  (int)(h * 0.09));
        int gap  = Math.max(12,  (int)(h * 0.02));
        int startY = Math.max((int)(h * 0.40), 160);

        int x = (w - btnW) / 2;
        earthButton.setBounds(x, startY, btnW, btnH);
        planetButton.setBounds(x, startY + (btnH + gap), btnW, btnH);
        guideButton.setBounds(x, startY + 2 * (btnH + gap), btnW, btnH);
        creditButton.setBounds(x, startY + 3 * (btnH + gap), btnW, btnH);

        int fontSize = Math.max(16, Math.min(28, (int)(btnH * 0.45)));
        Font f = new Font("Arial", Font.BOLD, fontSize);
        earthButton.setFont(f);
        planetButton.setFont(f);
        guideButton.setFont(f);
        creditButton.setFont(f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int horizon = (int) (h * 0.55);

        // Sky
        GradientPaint skyGradient = new GradientPaint(0, 0, skyBlue, 0, horizon, skyBlue.brighter());
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, w, horizon);

        // Ground
        GradientPaint groundGradient = new GradientPaint(0, horizon, lightGreen, 0, h, darkGreen);
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, horizon, w, h - horizon);

        drawClouds(g2d, w, horizon);
        drawHills(g2d, w, horizon);
        drawTitle(g2d, w);
    }

    private void drawClouds(Graphics2D g2d, int w, int skyH) {
        g2d.setColor(white);
        int[][] clouds = {
            { (int)(w*0.18), (int)(skyH*0.18), (int)(w*0.10), (int)(skyH*0.10) },
            { (int)(w*0.48), (int)(skyH*0.12), (int)(w*0.12), (int)(skyH*0.12) },
            { (int)(w*0.78), (int)(skyH*0.16), (int)(w*0.09), (int)(skyH*0.09) }
        };
        for (int[] c : clouds) {
            int x = c[0], y = c[1], cw = c[2], ch = c[3];
            g2d.fillOval(x - cw/2, y, (int)(cw*0.7), (int)(ch*0.6));
            g2d.fillOval(x,       y - ch/6, cw, ch);
            g2d.fillOval(x + cw/3, y, (int)(cw*0.7), (int)(ch*0.6));
        }
    }

    private void drawHills(Graphics2D g2d, int w, int horizon) {
        g2d.setColor(new Color(34, 139, 34, 150));
        int[] hillX1 = {0, w/3, 2*w/3, w};
        int[] hillY1 = {horizon, (int)(horizon*0.6), (int)(horizon*0.75), horizon};
        g2d.fillPolygon(hillX1, hillY1, 4);

        int[] hillX2 = {w/4, w/2, (int)(w*0.85), w};
        int[] hillY2 = {horizon, (int)(horizon*0.55), (int)(horizon*0.72), horizon};
        g2d.fillPolygon(hillX2, hillY2, 4);
    }

    private void drawTitle(Graphics2D g2d, int w) {
        int titleSize = Math.max(28, Math.min(56, (int)(w * 0.06)));
        g2d.setFont(new Font("Arial", Font.BOLD, titleSize));
        String title = "FluffyJump";
        int tw = g2d.getFontMetrics().stringWidth(title);

        // shadow
        g2d.setColor(Color.BLACK);
        g2d.drawString(title, (w - tw) / 2 + 2, (int)(getHeight() * 0.18) + 2);

        // main
        g2d.setColor(white);
        g2d.drawString(title, (w - tw) / 2, (int)(getHeight() * 0.18));

        g2d.setFont(new Font("Arial", Font.ITALIC, Math.max(12, (int)(titleSize * 0.33))));
        String sub = "Choose your world: Earth or Planet";
        int sw = g2d.getFontMetrics().stringWidth(sub);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(sub, (w - sw)/2, (int)(getHeight() * 0.18) + Math.max(24, titleSize / 2));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == earthButton) {
            startEarth();
        } else if (src == planetButton) {
            startPlanet();
        } else if (src == guideButton) {
            showGuide();
        } else if (src == creditButton) {
            showCredits();
        }
    }

    private void startPlanet() {
        RunnerGame game = new RunnerGame(RunnerGame.Level.PLANET);
        parentFrame.setContentPane(game);
        parentFrame.setResizable(true);
        parentFrame.setMinimumSize(new Dimension(640, 480));
        // ❌ ไม่ pack() เพื่อไม่ล็อกขนาด
        parentFrame.revalidate();
        parentFrame.repaint();
        parentFrame.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(game::requestFocusInWindow);
    }

    private void startEarth() {
        RunnerGame game = new RunnerGame(RunnerGame.Level.EARTH);
        parentFrame.setContentPane(game);
        parentFrame.setResizable(true);
        parentFrame.setMinimumSize(new Dimension(640, 480));
        // ❌ ไม่ pack() เพื่อไม่ล็อกขนาด
        parentFrame.revalidate();
        parentFrame.repaint();
        parentFrame.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(game::requestFocusInWindow);
    }

    private void showGuide() {
        String guide = "HOW TO PLAY \n\n" +
                "Earth:\n" +
                "• SPACE - Jump (double jump)\n" +
                "• A/D - Move Left/Right\n" +
                "• Collect coins +10 \n\n" +
                "• Collect Bonus coins +30\n\n" +
                " 100 Point to Win\n\n" +
                "Planet:\n" +
                "• Meteor -50 score on hit)\n" +
                "• UFO on ground lose a heart on hit\n" +
                "• Coins are on ground";
        JOptionPane.showMessageDialog(this, guide, "Game Guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCredits() {
        String credits = " FluffyJump \n\n" +
                "Podjanan Osatanan\n";
        JOptionPane.showMessageDialog(this, credits, "Credits", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FluffyJump");
            MainMenu menu = new MainMenu(frame);
            frame.setContentPane(menu);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.setSize(1000, 700);      // ✅ ใช้ setSize แทน pack
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.validate();              // ให้ layout คำนวณครั้งแรก
        });
    }
}
