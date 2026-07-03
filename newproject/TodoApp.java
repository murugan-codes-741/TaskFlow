import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

public class TodoApp extends JFrame {
    private static final String DATA_FILE = "todo_data.ser";
    private static final Preferences prefs = Preferences.userNodeForPackage(TodoApp.class);
    
    private static List<Project> projects = new ArrayList<>();
    private static TodoApp instance;
    
    // Global filter and search variables
    public static String currentFilter = "All";
    public static String searchQuery = "";
    public static boolean isDarkMode = false;
    
    // Theme Colors
    public static Color currentBgColor;
    public static Color currentCardBgColor;
    public static Color currentBorderColor;
    public static Color currentTextPrimary;
    public static Color currentTextSecondary;
    public static Color currentAccentColor;
    public static Color currentAccentHover;
    public static Color currentAccentLight;

    // Light Theme Palette
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color LIGHT_CARD_BG = Color.WHITE;
    private static final Color LIGHT_BORDER = new Color(226, 232, 240);
    private static final Color LIGHT_TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color LIGHT_TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color LIGHT_ACCENT = new Color(79, 70, 229);
    private static final Color LIGHT_ACCENT_HOVER = new Color(67, 56, 202);
    private static final Color LIGHT_ACCENT_LIGHT = new Color(224, 231, 255);

    // Dark Theme Palette
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color DARK_CARD_BG = new Color(30, 41, 59);
    private static final Color DARK_BORDER = new Color(51, 65, 85);
    private static final Color DARK_TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color DARK_TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color DARK_ACCENT = new Color(99, 102, 241);
    private static final Color DARK_ACCENT_HOVER = new Color(129, 140, 248);
    private static final Color DARK_ACCENT_LIGHT = new Color(49, 46, 129);

    // Date display formatter
    public static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd");

    // UI Panels and Elements
    private JPanel contentPane;
    private JPanel cardsPanel;
    private JScrollPane scrollPane;
    private Component verticalGlue;
    private JLabel statsLabel;
    private JComboBox<String> filterCombo;
    private ModernTextField searchBar;
    private ModernTextField projectInput;
    private JComboBox<String> projectEmojiCombo;
    
    // Stats Pills
    private StatPill donePill;
    private StatPill overduePill;
    private StatPill tasksPill;

    // Models
    public enum Priority {
        LOW("🟢 LOW", new Color(16, 185, 129), new Color(209, 250, 229)),
        MEDIUM("🟡 MED", new Color(217, 119, 6), new Color(254, 243, 199)),
        HIGH("🔴 HIGH", new Color(239, 68, 68), new Color(254, 226, 228));

        public final String label;
        public final Color fg;
        public final Color bg;

        Priority(String label, Color fg, Color bg) {
            this.label = label;
            this.fg = fg;
            this.bg = bg;
        }

        public Color getBgColor(boolean isDark) {
            if (isDark) {
                switch(this) {
                    case HIGH: return new Color(153, 27, 27);
                    case MEDIUM: return new Color(146, 64, 14);
                    case LOW: return new Color(6, 95, 70);
                }
            }
            return bg;
        }

        public Color getFgColor(boolean isDark) {
            if (isDark) {
                switch(this) {
                    case HIGH: return new Color(254, 226, 226);
                    case MEDIUM: return new Color(254, 243, 199);
                    case LOW: return new Color(209, 250, 229);
                }
            }
            return fg;
        }
    }

    public static class Project implements Serializable {
        private static final long serialVersionUID = 2L;
        String name;
        String emoji;
        List<Task> tasks;
        
        public Project(String name, String emoji) {
            this.name = name;
            this.emoji = emoji;
            this.tasks = new ArrayList<>();
        }
    }

    public static class Task implements Serializable {
        private static final long serialVersionUID = 2L;
        String name;
        boolean completed;
        Priority priority;
        LocalDate dueDate;
        
        public Task(String name, Priority priority, LocalDate dueDate) {
            this.name = name;
            this.completed = false;
            this.priority = priority;
            this.dueDate = dueDate;
        }

        public boolean isOverdue() {
            return !completed && dueDate != null && dueDate.isBefore(LocalDate.now());
        }
    }

    public TodoApp() {
        super("Project Planner");
        instance = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 750);
        setMinimumSize(new Dimension(420, 600));
        setLocationRelativeTo(null);
        
        // Initialize Theme from preferences
        isDarkMode = prefs.getBoolean("darkMode", false);
        setThemeColors(isDarkMode);

        // Load data
        loadData();

        // JLayeredPane layer layout configuration
        JLayeredPane layeredPane = getLayeredPane();

        // Initialize GlassPane Celebration Particle Engine
        CelebrationEngine celebrationEngine = new CelebrationEngine();
        setGlassPane(celebrationEngine);

        // Main Layout Container
        contentPane = new JPanel(new BorderLayout(0, 12));
        contentPane.setBackground(currentBgColor);
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(contentPane);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setOpaque(false);

        // Title and Theme Toggle Row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 To Do List");
        titleLabel.setFont(getModernFont(Font.BOLD, 24));
        titleLabel.setForeground(currentTextPrimary);
        titleRow.add(titleLabel, BorderLayout.WEST);

        // Theme Toggle Button
        ModernButton themeToggleBtn = new ModernButton(isDarkMode ? "☀️" : "🌙", new Color(241, 245, 249), new Color(226, 232, 240)) {
            {
                setPreferredSize(new Dimension(32, 32));
                setFont(getModernFont(Font.PLAIN, 14));
            }
        };
        themeToggleBtn.addActionListener(e -> toggleTheme());
        titleRow.add(themeToggleBtn, BorderLayout.EAST);
        headerPanel.add(titleRow, BorderLayout.NORTH);

        // Search Bar & Filter Row
        JPanel controlsRow = new JPanel(new BorderLayout(8, 0));
        controlsRow.setOpaque(false);

        searchBar = new ModernTextField("🔍 Find a project or task...", 15);
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSearch(); }
            public void removeUpdate(DocumentEvent e) { updateSearch(); }
            public void changedUpdate(DocumentEvent e) { updateSearch(); }
            private void updateSearch() {
                searchQuery = searchBar.getText().trim();
                renderProjects();
            }
        });
        controlsRow.add(searchBar, BorderLayout.CENTER);

        // Global Filter Combobox
        filterCombo = new JComboBox<>(new String[]{"All", "Active", "Completed", "Overdue"});
        filterCombo.setRenderer(new ModernComboBoxRenderer());
        filterCombo.setBackground(currentCardBgColor);
        filterCombo.setForeground(currentTextPrimary);
        filterCombo.setFont(getModernFont(Font.BOLD, 12));
        filterCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentBorderColor, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        filterCombo.addActionListener(e -> {
            currentFilter = (String) filterCombo.getSelectedItem();
            renderProjects();
        });
        controlsRow.add(filterCombo, BorderLayout.EAST);
        headerPanel.add(controlsRow, BorderLayout.CENTER);

        // Global Stats Summary Panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 4, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        donePill = new StatPill("📊", "0%", "Done");
        donePill.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { filterCombo.setSelectedItem("Completed"); }
        });

        JLabel divider1 = new JLabel("|", SwingConstants.CENTER);
        divider1.setForeground(currentBorderColor);
        divider1.setFont(getModernFont(Font.PLAIN, 14));

        overduePill = new StatPill("⏰", "0", "Overdue");
        overduePill.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { filterCombo.setSelectedItem("Overdue"); }
        });

        JLabel divider2 = new JLabel("|", SwingConstants.CENTER);
        divider2.setForeground(currentBorderColor);
        divider2.setFont(getModernFont(Font.PLAIN, 14));

        tasksPill = new StatPill("✅", "0/0", "Tasks");
        tasksPill.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { filterCombo.setSelectedItem("All"); }
        });

        statsPanel.add(donePill);
        statsPanel.add(divider1);
        statsPanel.add(overduePill);
        statsPanel.add(divider2);
        statsPanel.add(tasksPill);

        JPanel summaryWrapper = new JPanel(new BorderLayout(0, 4));
        summaryWrapper.setOpaque(false);
        
        statsLabel = new JLabel("0 projects · 0 tasks");
        statsLabel.setFont(getModernFont(Font.PLAIN, 12));
        statsLabel.setForeground(currentTextSecondary);
        summaryWrapper.add(statsLabel, BorderLayout.NORTH);
        
        // Inner stats panel
        JPanel statsCard = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(isDarkMode ? new Color(30, 41, 59) : new Color(241, 245, 249));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(currentBorderColor);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2d.dispose();
            }
        };
        statsCard.setOpaque(false);
        statsCard.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statsCard.add(statsPanel, BorderLayout.CENTER);
        summaryWrapper.add(statsCard, BorderLayout.CENTER);

        headerPanel.add(summaryWrapper, BorderLayout.SOUTH);
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // Project Cards List (Center)
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);

        scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Bottom Project creation layout (South)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        ShadowPanel inputCard = new ShadowPanel() {
            {
                setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }
        };
        inputCard.setLayout(new BorderLayout(8, 0));

        projectEmojiCombo = new JComboBox<>(new String[]{"🏕️", "🚀", "🎨", "📝", "💻", "🛒", "🍔"});
        projectEmojiCombo.setRenderer(new ModernComboBoxRenderer());
        projectEmojiCombo.setBackground(currentCardBgColor);
        projectEmojiCombo.setForeground(currentTextPrimary);
        projectEmojiCombo.setFont(getModernFont(Font.PLAIN, 16));
        projectEmojiCombo.setBorder(BorderFactory.createLineBorder(currentBorderColor, 1, true));

        projectInput = new ModernTextField("Create a new project...", 20);
        ModernButton addProjectBtn = new ModernButton("+") {
            {
                setPreferredSize(new Dimension(40, 40));
                setFont(getModernFont(Font.BOLD, 18));
            }
        };

        inputCard.add(projectEmojiCombo, BorderLayout.WEST);
        inputCard.add(projectInput, BorderLayout.CENTER);
        inputCard.add(addProjectBtn, BorderLayout.EAST);
        bottomPanel.add(inputCard, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        // Add Project Action
        ActionListener addProjectAction = e -> {
            String name = projectInput.getText().trim();
            String emoji = (String) projectEmojiCombo.getSelectedItem();
            if (!name.isEmpty()) {
                Project newProject = new Project(name, emoji);
                projects.add(newProject);
                saveData();
                
                renderProjects();
                projectInput.setText("");
                projectEmojiCombo.setSelectedIndex(0);
                updateGlobalStatsPanel();
            }
        };
        projectInput.addActionListener(addProjectAction);
        addProjectBtn.addActionListener(addProjectAction);

        verticalGlue = Box.createVerticalGlue();
        renderProjects();
        updateGlobalStatsPanel();
    }

    private void setThemeColors(boolean dark) {
        currentBgColor = dark ? DARK_BG : LIGHT_BG;
        currentCardBgColor = dark ? DARK_CARD_BG : LIGHT_CARD_BG;
        currentBorderColor = dark ? DARK_BORDER : LIGHT_BORDER;
        currentTextPrimary = dark ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
        currentTextSecondary = dark ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;
        currentAccentColor = dark ? DARK_ACCENT : LIGHT_ACCENT;
        currentAccentHover = dark ? DARK_ACCENT_HOVER : LIGHT_ACCENT_HOVER;
        currentAccentLight = dark ? DARK_ACCENT_LIGHT : LIGHT_ACCENT_LIGHT;
    }

    private void toggleTheme() {
        boolean nextDark = !isDarkMode;
        prefs.putBoolean("darkMode", nextDark);

        Color startBg = currentBgColor;
        Color startCardBg = currentCardBgColor;
        Color startBorder = currentBorderColor;
        Color startTextP = currentTextPrimary;
        Color startTextS = currentTextSecondary;
        Color startAccent = currentAccentColor;

        setThemeColors(nextDark);
        isDarkMode = nextDark;

        // Theme fade animation (300ms cross-fade)
        Timer themeTimer = new Timer(15, null);
        themeTimer.addActionListener(new ActionListener() {
            int step = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                float t = step / 20.0f;
                currentBgColor = lerpColor(startBg, currentBgColor, t);
                currentCardBgColor = lerpColor(startCardBg, currentCardBgColor, t);
                currentBorderColor = lerpColor(startBorder, currentBorderColor, t);
                currentTextPrimary = lerpColor(startTextP, currentTextPrimary, t);
                currentTextSecondary = lerpColor(startTextS, currentTextSecondary, t);
                currentAccentColor = lerpColor(startAccent, currentAccentColor, t);
                
                contentPane.setBackground(currentBgColor);
                refreshUIComponents(getContentPane());
                
                if (step >= 20) {
                    themeTimer.stop();
                    setThemeColors(isDarkMode); // Lock exact end colors
                    refreshFrame();
                }
            }
        });
        themeTimer.start();
    }

    private void refreshUIComponents(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                jc.setForeground(currentTextPrimary);
                if (jc instanceof JTextField) {
                    jc.setForeground(currentTextPrimary);
                } else if (jc instanceof JComboBox) {
                    jc.setBackground(currentCardBgColor);
                    jc.setForeground(currentTextPrimary);
                    jc.setBorder(BorderFactory.createLineBorder(currentBorderColor, 1, true));
                }
            }
            if (c instanceof Container) {
                refreshUIComponents((Container) c);
            }
        }
        container.repaint();
    }

    private void refreshFrame() {
        SwingUtilities.updateComponentTreeUI(this);
        renderProjects();
        updateGlobalStatsPanel();
    }

    public static Color lerpColor(Color c1, Color c2, float t) {
        int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        int a = (int) (c1.getAlpha() + t * (c2.getAlpha() - c1.getAlpha()));
        return new Color(r, g, b, a);
    }

    public void renderProjects() {
        for (Component c : cardsPanel.getComponents()) {
            if (c instanceof EmptyStatePanel) {
                ((EmptyStatePanel) c).stopTimer();
            }
        }
        
        cardsPanel.removeAll();
        String query = searchQuery.toLowerCase().trim();
        int visibleCount = 0;

        for (Project p : projects) {
            boolean projectMatches = p.name.toLowerCase().contains(query) || (p.emoji != null && p.emoji.toLowerCase().contains(query));
            boolean taskMatches = false;
            
            for (Task t : p.tasks) {
                if (t.name.toLowerCase().contains(query)) {
                    taskMatches = true;
                    break;
                }
            }

            if (projectMatches || taskMatches) {
                ProjectCard card = new ProjectCard(p);
                cardsPanel.add(card);
                visibleCount++;
            }
        }

        if (visibleCount == 0) {
            EmptyStatePanel emptyPanel;
            if (projects.isEmpty()) {
                emptyPanel = new EmptyStatePanel("✨ No tasks yet. Create your first project!", true);
            } else {
                emptyPanel = new EmptyStatePanel("🔍 No matching results found.", false);
            }
            cardsPanel.add(emptyPanel);
        } else {
            cardsPanel.add(verticalGlue);
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    public void updateGlobalStatsPanel() {
        int projectCount = projects.size();
        int totalTasks = 0;
        int completedTasks = 0;
        int overdueTasks = 0;

        for (Project p : projects) {
            for (Task t : p.tasks) {
                totalTasks++;
                if (t.completed) {
                    completedTasks++;
                } else if (t.isOverdue()) {
                    overdueTasks++;
                }
            }
        }

        int donePercent = totalTasks == 0 ? 0 : (completedTasks * 100 / totalTasks);
        
        statsLabel.setText(projectCount + " project" + (projectCount != 1 ? "s" : "") + " · " 
                         + totalTasks + " task" + (totalTasks != 1 ? "s" : ""));

        donePill.setValue(donePercent, "%", true);
        overduePill.setValue(overdueTasks, "", true);
        tasksPill.setRatioValue(completedTasks, totalTasks, true);
    }

    public static void showToast(String message) {
        if (instance != null) {
            ToastNotification toast = new ToastNotification(message, instance.getLayeredPane());
            instance.getLayeredPane().add(toast, JLayeredPane.POPUP_LAYER);
            instance.getLayeredPane().revalidate();
            instance.getLayeredPane().repaint();
        }
    }

    public void triggerGlobalCelebration(ProjectCard card) {
        CelebrationEngine engine = (CelebrationEngine) getGlassPane();
        engine.startCelebration(card);
    }

    // Font utility
    public static Font getModernFont(int style, int size) {
        Font font = new Font("Inter", style, size);
        if ("Dialog".equals(font.getFamily())) {
            return new Font("Segoe UI", style, size);
        }
        return font;
    }

    // Storage
    public static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(projects);
        } catch (IOException e) {
            System.err.println("Error saving task data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                projects = (List<Project>) ois.readObject();
                for (Project p : projects) {
                    if (p.emoji == null) p.emoji = "🚀";
                    for (Task t : p.tasks) {
                        if (t.priority == null) t.priority = Priority.LOW;
                        if (t.dueDate == null) t.dueDate = LocalDate.now();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load task data, creating fresh list: " + e.getMessage());
                projects = new ArrayList<>();
            }
        } else {
            // Seed sample data matching the mockup exactly
            Project sample = new Project("Trip", "🏕️");
            sample.tasks.add(new Task("Book flight", Priority.HIGH, LocalDate.now().minusDays(8))); 
            sample.tasks.get(0).completed = true;
            sample.tasks.add(new Task("Pack bags", Priority.MEDIUM, LocalDate.now().minusDays(5))); 
            sample.tasks.add(new Task("Buy souvenirs", Priority.LOW, LocalDate.now().minusDays(3))); 

            projects.add(sample);
            saveData();
        }
    }

    public static void removeProject(ProjectCard card, Project p) {
        projects.remove(p);
        saveData();
        instance.cardsPanel.remove(card);
        instance.cardsPanel.revalidate();
        instance.cardsPanel.repaint();
        instance.updateGlobalStatsPanel();
        instance.renderProjects();
    }

    // Bouncing Empty State Panel
    public static class EmptyStatePanel extends JPanel {
        private final String text;
        private int bounceY = 0;
        private int direction = 1;
        private Timer bounceTimer;

        public EmptyStatePanel(String text, boolean animate) {
            this.text = text;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(350, 200));

            if (animate) {
                bounceTimer = new Timer(50, e -> {
                    bounceY += direction;
                    if (bounceY > 6 || bounceY < 0) {
                        direction = -direction;
                    }
                    repaint();
                });
                bounceTimer.start();
            }
        }

        public void stopTimer() {
            if (bounceTimer != null) bounceTimer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(currentTextSecondary);
            g2d.setFont(getModernFont(Font.BOLD, 13));
            FontMetrics fm = g2d.getFontMetrics();

            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + bounceY;
            g2d.drawString(text, x, y);
            
            g2d.dispose();
        }
    }

    // Particle Model Definition
    public static class Particle {
        double x, y;           // Position
        double vx, vy;         // Velocity
        int size;              // 2-8px
        Color color;
        double rotation;       // 0-360
        double rotationSpeed;  // Random
        int alpha = 255;       // 255 → 0
        int shapeType;         // 0=Circle, 1=Square, 2=Diamond, 3=Star, 4=Emoji
        String emoji;          // Custom floating emoji
        int life;              // Remaining frames

        public Particle(double x, double y, double vx, double vy, int size, Color color, int shapeType, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.shapeType = shapeType;
            this.rotation = Math.random() * 360;
            this.rotationSpeed = (Math.random() - 0.5) * 10;
            this.life = life;
        }

        public Particle(double x, double y, double vx, double vy, String emoji, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.emoji = emoji;
            this.shapeType = 4; // Emoji type
            this.rotation = 0;
            this.rotationSpeed = (Math.random() - 0.5) * 4;
            this.size = 24;
            this.life = life;
        }

        public boolean update() {
            x += vx;
            y += vy;
            rotation += rotationSpeed;

            if (shapeType == 4) {
                // Floating Emojis rise up and slow down horizontally
                vx *= 0.98;
            } else if (vy > 0 && Math.abs(vx) < 2) {
                // Confetti Rain gravity fall acceleration
                vy += 0.1;
            }

            life--;
            alpha = Math.max(0, (life * 255) / 180); // Fade ratio
            return life > 0;
        }

        public void draw(Graphics2D g2d) {
            Graphics2D g = (Graphics2D) g2d.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            g.rotate(Math.toRadians(rotation));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0f));

            if (shapeType == 4) {
                g.setColor(Color.WHITE);
                g.setFont(getModernFont(Font.PLAIN, size));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(emoji, -fm.stringWidth(emoji) / 2, fm.getAscent() - fm.getHeight() / 2);
            } else {
                g.setColor(color);
                int half = size / 2;
                switch (shapeType) {
                    case 0: // Circle
                        g.fillOval(-half, -half, size, size);
                        break;
                    case 1: // Square
                        g.fillRect(-half, -half, size, size);
                        break;
                    case 2: // Diamond
                        int[] dx = {0, half, 0, -half};
                        int[] dy = {-half, 0, half, 0};
                        g.fillPolygon(dx, dy, 4);
                        break;
                    case 3: // Star
                        int[] sx = {0, half/3, half, half/3, 0, -half/3, -half, -half/3};
                        int[] sy = {-half, -half/3, 0, half/3, half, half/3, 0, -half/3};
                        g.fillPolygon(sx, sy, 8);
                        break;
                }
            }
            g.dispose();
        }
    }

    // GlassPane Centralized Celebration Engine
    public static class CelebrationEngine extends JComponent {
        private final List<Particle> particles = Collections.synchronizedList(new ArrayList<>());
        private Timer timer;

        public CelebrationEngine() {
            setOpaque(false);
        }

        public void startCelebration(Component projectCard) {
            Point cardPos = SwingUtilities.convertPoint(projectCard, new Point(0, 0), this);
            int cardW = projectCard.getWidth();
            int cardH = projectCard.getHeight();
            int cardCenterX = cardPos.x + cardW / 2;
            int cardCenterY = cardPos.y + cardH / 2;

            Color[] colors = {
                new Color(239, 68, 68),   // red
                new Color(245, 158, 11),  // gold
                new Color(59, 130, 246),  // blue
                new Color(16, 185, 129),  // green
                new Color(139, 92, 246),  // purple
                new Color(236, 72, 153)   // pink
            };

            // 1. CONFETTI RAIN (100-150 particles)
            int confettiCount = 130;
            for (int i = 0; i < confettiCount; i++) {
                double px = Math.random() * getWidth();
                double py = -50 - (Math.random() * 200);
                double vx = (Math.random() - 0.5) * 4;
                double vy = 2 + Math.random() * 5;
                int size = 6 + (int)(Math.random() * 6);
                Color c = colors[(int)(Math.random() * colors.length)];
                int shape = (int)(Math.random() * 3); // Circle, Square, Diamond
                int life = 150 + (int)(Math.random() * 50); // 3-4s life
                particles.add(new Particle(px, py, vx, vy, size, c, shape, life));
            }

            // 2. PARTICLE EXPLOSION (30-40 star sparkles)
            int explosionCount = 35;
            for (int i = 0; i < explosionCount; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double speed = 1 + Math.random() * 7;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                int size = 8 + (int)(Math.random() * 6);
                Color c = colors[(int)(Math.random() * colors.length)];
                int life = 40 + (int)(Math.random() * 30);
                particles.add(new Particle(cardCenterX, cardCenterY, vx, vy, size, c, 3, life)); // shape 3 is Star
            }

            // 3. FLOATING EMOJI CELEBRATION (10-15 particles)
            String[] emojis = {"🎉", "🎊", "⭐", "✨", "🏆", "🎈", "🎁", "🌟", "💫", "🎇"};
            int emojiCount = 12;
            for (int i = 0; i < emojiCount; i++) {
                double px = cardPos.x + 20 + Math.random() * (cardW - 40);
                double py = cardPos.y + cardH - 10;
                double vx = (Math.random() - 0.5) * 2;
                double vy = -2 - Math.random() * 3; // Float upward
                String emoji = emojis[(int)(Math.random() * emojis.length)];
                int life = 120 + (int)(Math.random() * 40);
                particles.add(new Particle(px, py, vx, vy, emoji, life));
            }

            if (timer == null || !timer.isRunning()) {
                // ~60fps animations
                timer = new Timer(16, e -> {
                    synchronized (particles) {
                        for (int i = particles.size() - 1; i >= 0; i--) {
                            Particle p = particles.get(i);
                            if (!p.update()) {
                                particles.remove(i);
                            }
                        }
                    }
                    repaint();
                    if (particles.isEmpty()) {
                        timer.stop();
                        setVisible(false);
                    }
                });
                setVisible(true);
                timer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            synchronized (particles) {
                for (Particle p : particles) {
                    p.draw(g2d);
                }
            }
            g2d.dispose();
        }
    }

    // Double-Border Sliding Toast Notification
    public static class ToastNotification extends JPanel {
        private final String message;
        private float alpha = 0.0f;
        private int xOffset = 0;
        private Timer slideTimer;
        private Timer dismissTimer;
        private final List<Point> miniConfetti = new ArrayList<>();
        private final Color[] confettiColors = {
            new Color(239, 68, 68, 120),
            new Color(245, 158, 11, 120),
            new Color(59, 130, 246, 120),
            new Color(16, 185, 129, 120),
            new Color(139, 92, 246, 120)
        };

        public ToastNotification(String message, JLayeredPane parent) {
            this.message = message;
            setOpaque(false);
            setSize(340, 60);
            
            xOffset = parent.getWidth();
            setLocation(xOffset, 20);

            // Confetti shower points behind text
            for (int i = 0; i < 20; i++) {
                miniConfetti.add(new Point((int)(Math.random() * 320), (int)(Math.random() * 50)));
            }

            // Slide In from top-right corner (400ms)
            slideTimer = new Timer(16, null);
            slideTimer.addActionListener(new ActionListener() {
                int frame = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame++;
                    float t = frame / 25.0f;
                    xOffset = parent.getWidth() - (int)(360 * easeOut(t));
                    alpha = Math.min(1.0f, t);
                    setLocation(xOffset, 20);
                    parent.repaint();
                    
                    if (frame >= 25) {
                        slideTimer.stop();
                        // Auto-dismisses after 4 seconds
                        dismissTimer = new Timer(4000, ev -> slideOut(parent));
                        dismissTimer.setRepeats(false);
                        dismissTimer.start();
                    }
                }
            });
            slideTimer.start();
        }

        private float easeOut(float t) {
            return 1.0f - (1.0f - t) * (1.0f - t);
        }

        private void slideOut(JLayeredPane parent) {
            if (slideTimer != null && slideTimer.isRunning()) slideTimer.stop();
            
            slideTimer = new Timer(16, null);
            slideTimer.addActionListener(new ActionListener() {
                int frame = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame++;
                    float t = frame / 25.0f;
                    xOffset = (parent.getWidth() - 360) + (int)(360 * easeIn(t));
                    alpha = Math.max(0.0f, 1.0f - t);
                    setLocation(xOffset, 20);
                    parent.repaint();
                    
                    if (frame >= 25) {
                        slideTimer.stop();
                        parent.remove(ToastNotification.this);
                        parent.repaint();
                    }
                }
            });
            slideTimer.start();
        }

        private float easeIn(float t) {
            return t * t;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Background Pill
            g2d.setColor(isDarkMode ? new Color(30, 41, 59) : Color.WHITE);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            // Double Border
            g2d.setColor(currentAccentColor);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 14, 14);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 11, 11);

            // Background Confetti Shower
            for (int i = 0; i < miniConfetti.size(); i++) {
                Point pt = miniConfetti.get(i);
                g2d.setColor(confettiColors[i % confettiColors.length]);
                g2d.fillOval(pt.x, pt.y, 4 + (i % 4), 4 + (i % 4));
            }

            // Text Label
            g2d.setColor(isDarkMode ? Color.WHITE : currentTextPrimary);
            g2d.setFont(getModernFont(Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(message)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(message, tx, ty);

            g2d.dispose();
        }
    }

    // Pill-shaped Stats Counter Panel
    public static class StatPill extends JPanel {
        private final String icon;
        private final String label;
        private int currentVal = 0;
        private int targetVal = 0;
        
        private int currentNum = 0;
        private int currentDen = 0;
        private int targetNum = 0;
        private int targetDen = 0;
        
        private boolean isRatio = false;
        private String suffix = "";
        private Timer animTimer;

        public StatPill(String icon, String initialVal, String label) {
            this.icon = icon;
            this.label = label;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { repaint(); }
                public void mouseExited(MouseEvent e) { repaint(); }
            });
        }

        public void setValue(int newValue, String suffix, boolean animate) {
            this.isRatio = false;
            this.targetVal = newValue;
            this.suffix = suffix;
            
            if (!animate) {
                this.currentVal = newValue;
                repaint();
                return;
            }
            
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();
            
            animTimer = new Timer(15, e -> {
                if (currentVal < targetVal) {
                    currentVal += Math.ceil((targetVal - currentVal) * 0.15f);
                    if (currentVal > targetVal) currentVal = targetVal;
                } else if (currentVal > targetVal) {
                    currentVal -= Math.ceil((currentVal - targetVal) * 0.15f);
                    if (currentVal < targetVal) currentVal = targetVal;
                } else {
                    animTimer.stop();
                }
                repaint();
            });
            animTimer.start();
        }

        public void setRatioValue(int num, int den, boolean animate) {
            this.isRatio = true;
            this.targetNum = num;
            this.targetDen = den;
            
            if (!animate) {
                this.currentNum = num;
                this.currentDen = den;
                repaint();
                return;
            }
            
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();
            
            animTimer = new Timer(15, e -> {
                boolean doneNum = false;
                boolean doneDen = false;
                
                if (currentNum < targetNum) {
                    currentNum += Math.ceil((targetNum - currentNum) * 0.15f);
                    if (currentNum > targetNum) currentNum = targetNum;
                } else if (currentNum > targetNum) {
                    currentNum -= Math.ceil((currentNum - targetNum) * 0.15f);
                    if (currentNum < targetNum) currentNum = targetNum;
                } else {
                    doneNum = true;
                }

                if (currentDen < targetDen) {
                    currentDen += Math.ceil((targetDen - currentDen) * 0.15f);
                    if (currentDen > targetDen) currentDen = targetDen;
                } else if (currentDen > targetDen) {
                    currentDen -= Math.ceil((currentDen - targetDen) * 0.15f);
                    if (currentDen < targetDen) currentDen = targetDen;
                } else {
                    doneDen = true;
                }
                
                if (doneNum && doneDen) animTimer.stop();
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean hovered = getMousePosition() != null;
            
            g2d.setColor(hovered ? currentAccentLight : (isDarkMode ? new Color(51, 65, 85) : Color.WHITE));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2d.setColor(hovered ? currentAccentColor : currentBorderColor);
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2d.setFont(getModernFont(Font.PLAIN, 11));
            FontMetrics fm = g2d.getFontMetrics();
            
            String valStr = isRatio ? (currentNum + "/" + currentDen) : (currentVal + suffix);
            String fullStr = icon + " " + valStr + " " + label;

            g2d.setColor(currentTextPrimary);
            int tx = (getWidth() - fm.stringWidth(fullStr)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(fullStr, tx, ty);

            g2d.dispose();
        }
    }

    // Custom Rounded Panel with Shadows
    public static class ShadowPanel extends JPanel {
        protected float alpha = 1.0f;
        protected double shrinkScale = 1.0f;

        public ShadowPanel() {
            setOpaque(false);
            int shadowOffset = 4;
            Border outer = BorderFactory.createEmptyBorder(6, 0, 6, 0);
            Border inner = BorderFactory.createEmptyBorder(16 + shadowOffset, 16 + shadowOffset, 16 + shadowOffset, 16 + shadowOffset);
            setBorder(BorderFactory.createCompoundBorder(outer, inner));
            setLayout(new BorderLayout(0, 12));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = (int) (d.height * shrinkScale);
            return d;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension d = super.getMinimumSize();
            d.height = (int) (d.height * shrinkScale);
            return d;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension d = super.getMaximumSize();
            d.height = (int) (d.height * shrinkScale);
            return d;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.clipRect(0, 0, getWidth(), getHeight());
            super.paint(g2d);
            g2d.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int shadowOffset = 4;
            int cardW = w - shadowOffset * 2;
            int cardH = h - shadowOffset * 2;

            if (cardW > 0 && cardH > 8) {
                int yOffset = 6;
                cardH -= 12;

                g2d.setColor(isDarkMode ? new Color(0, 0, 0, 45) : new Color(0, 0, 0, 8));
                g2d.fillRoundRect(shadowOffset, yOffset + shadowOffset + 2, cardW, cardH, 12, 12);
                g2d.setColor(isDarkMode ? new Color(0, 0, 0, 20) : new Color(0, 0, 0, 4));
                g2d.fillRoundRect(shadowOffset - 1, yOffset + shadowOffset + 1, cardW + 2, cardH, 12, 12);

                g2d.setColor(currentCardBgColor);
                g2d.fillRoundRect(shadowOffset, yOffset + shadowOffset, cardW, cardH, 12, 12);

                g2d.setColor(currentBorderColor);
                g2d.drawRoundRect(shadowOffset, yOffset + shadowOffset, cardW, cardH, 12, 12);
            }
            g2d.dispose();
        }
    }

    // Custom Project Card Component
    public static class ProjectCard extends ShadowPanel {
        private final Project project;
        private final JLabel titleLabel;
        private final JLabel counterLabel;
        private final JLabel percentLabel;
        private final ModernProgressBar progressBar;
        private final CollapsiblePanel collapsiblePanel;
        private final JPanel tasksContainer;
        private final JPanel controlBarPanel;
        
        // Sorting and Card Filtering variables
        private String cardFilterPriority = "All";
        private String cardSortOption = "None";
        
        // Gold glowing outline variables
        private boolean isGlowing = false;
        private float glowAlpha = 0.0f;
        private Timer glowTimer;
        
        private final JPanel taskFormPanel;
        private final JButton toggleBtn;
        private final JButton deleteBtn;
        private Timer fadeTimer;

        public ProjectCard(Project project) {
            this.project = project;

            // Header panel (North)
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);

            // Title and Task count (Left)
            JPanel titlePanel = new JPanel();
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.setOpaque(false);

            titleLabel = new JLabel(project.emoji + " " + project.name);
            titleLabel.setFont(getModernFont(Font.BOLD, 16));
            titleLabel.setForeground(currentTextPrimary);

            JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            statsRow.setOpaque(false);
            statsRow.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            counterLabel = new JLabel("0/0 tasks");
            counterLabel.setFont(getModernFont(Font.PLAIN, 12));
            counterLabel.setForeground(currentTextSecondary);

            percentLabel = new JLabel("0%");
            percentLabel.setFont(getModernFont(Font.BOLD, 12));
            percentLabel.setForeground(currentAccentColor);

            statsRow.add(counterLabel);
            statsRow.add(percentLabel);

            titlePanel.add(titleLabel);
            titlePanel.add(statsRow);
            headerPanel.add(titlePanel, BorderLayout.CENTER);

            // Action Buttons (Right)
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actionPanel.setOpaque(false);

            toggleBtn = new ModernButton("▼", new Color(241, 245, 249), new Color(226, 232, 240)) {
                {
                    setForeground(currentTextSecondary);
                    setPreferredSize(new Dimension(28, 28));
                    setFont(getModernFont(Font.BOLD, 10));
                }
            };

            deleteBtn = new ModernButton("×", new Color(241, 245, 249), new Color(254, 226, 226)) {
                {
                    setForeground(new Color(148, 163, 184));
                    setPreferredSize(new Dimension(28, 28));
                    setFont(getModernFont(Font.BOLD, 16));
                }
            };
            deleteBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { deleteBtn.setForeground(new Color(220, 38, 38)); }
                public void mouseExited(MouseEvent e) { deleteBtn.setForeground(new Color(148, 163, 184)); }
            });

            actionPanel.add(toggleBtn);
            actionPanel.add(deleteBtn);
            headerPanel.add(actionPanel, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            // Center Panel
            JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
            centerContainer.setOpaque(false);

            progressBar = new ModernProgressBar();
            progressBar.setPreferredSize(new Dimension(0, 6));
            centerContainer.add(progressBar, BorderLayout.NORTH);

            // Collapsible tasks wrapper
            JPanel listWrapper = new JPanel();
            listWrapper.setLayout(new BoxLayout(listWrapper, BoxLayout.Y_AXIS));
            listWrapper.setOpaque(false);

            // Card Sort & Priority Filter Controls Bar
            controlBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            controlBarPanel.setOpaque(false);
            controlBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

            JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Sort: None", "Sort: Priority", "Sort: Date"});
            sortCombo.setRenderer(new ModernComboBoxRenderer());
            sortCombo.setBackground(currentCardBgColor);
            sortCombo.setForeground(currentTextPrimary);
            sortCombo.setFont(getModernFont(Font.PLAIN, 10));
            sortCombo.setBorder(BorderFactory.createLineBorder(currentBorderColor, 1));
            sortCombo.addActionListener(e -> {
                cardSortOption = (String) sortCombo.getSelectedItem();
                renderTasks();
            });

            JComboBox<String> priorityFilterCombo = new JComboBox<>(new String[]{"Priority: All", "Priority: High", "Priority: Med", "Priority: Low"});
            priorityFilterCombo.setRenderer(new ModernComboBoxRenderer());
            priorityFilterCombo.setBackground(currentCardBgColor);
            priorityFilterCombo.setForeground(currentTextPrimary);
            priorityFilterCombo.setFont(getModernFont(Font.PLAIN, 10));
            priorityFilterCombo.setBorder(BorderFactory.createLineBorder(currentBorderColor, 1));
            priorityFilterCombo.addActionListener(e -> {
                cardFilterPriority = (String) priorityFilterCombo.getSelectedItem();
                renderTasks();
            });

            controlBarPanel.add(sortCombo);
            controlBarPanel.add(priorityFilterCombo);
            listWrapper.add(controlBarPanel);

            // Tasks Container
            tasksContainer = new JPanel();
            tasksContainer.setLayout(new BoxLayout(tasksContainer, BoxLayout.Y_AXIS));
            tasksContainer.setOpaque(false);
            listWrapper.add(tasksContainer);

            // Expandable inline add-task form panel
            taskFormPanel = new JPanel(new BorderLayout(8, 0));
            taskFormPanel.setOpaque(false);
            taskFormPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

            ModernButton expandAddTaskBtn = new ModernButton("+ Add Task", currentBgColor, currentAccentLight) {
                {
                    setPreferredSize(new Dimension(0, 32));
                    setFont(getModernFont(Font.BOLD, 12));
                    setForeground(currentAccentColor);
                }
            };
            taskFormPanel.add(expandAddTaskBtn, BorderLayout.CENTER);
            listWrapper.add(taskFormPanel);

            collapsiblePanel = new CollapsiblePanel(listWrapper);
            centerContainer.add(collapsiblePanel, BorderLayout.CENTER);
            add(centerContainer, BorderLayout.CENTER);

            // Toggle card actions
            toggleBtn.addActionListener(e -> {
                boolean nextState = !collapsiblePanel.isExpanded();
                collapsiblePanel.setExpanded(nextState, true);
                toggleBtn.setText(nextState ? "▲" : "▼");
            });

            deleteBtn.addActionListener(e -> animateRemoval());

            // Expandable Form Action
            expandAddTaskBtn.addActionListener(e -> {
                taskFormPanel.removeAll();
                
                JPanel form = new JPanel(new GridLayout(2, 1, 0, 6));
                form.setOpaque(false);
                form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(currentBorderColor, 1, true),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));

                ModernTextField descField = new ModernTextField("Add a new task...", 15);
                
                JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                metaRow.setOpaque(false);

                JComboBox<Priority> prioCombo = new JComboBox<>(Priority.values());
                prioCombo.setRenderer(new ModernComboBoxRenderer());
                prioCombo.setBackground(currentCardBgColor);
                prioCombo.setSelectedItem(Priority.MEDIUM);
                prioCombo.setBorder(BorderFactory.createLineBorder(currentBorderColor, 1));
                
                String todayStr = LocalDate.now().toString();
                ModernTextField dateField = new ModernTextField(todayStr, 10);
                dateField.setToolTipText("Due date (YYYY-MM-DD)");

                ModernButton submitBtn = new ModernButton("Add", currentAccentColor, currentAccentHover) {
                    { setPreferredSize(new Dimension(60, 26)); setForeground(Color.WHITE); }
                };
                ModernButton cancelBtn = new ModernButton("Cancel", currentBgColor, currentBorderColor) {
                    { setPreferredSize(new Dimension(70, 26)); setForeground(currentTextSecondary); }
                };

                metaRow.add(prioCombo);
                metaRow.add(dateField);
                metaRow.add(cancelBtn);
                metaRow.add(submitBtn);

                form.add(descField);
                form.add(metaRow);
                taskFormPanel.add(form, BorderLayout.CENTER);
                taskFormPanel.revalidate();
                taskFormPanel.repaint();
                descField.requestFocus();

                ActionListener submitAction = ev -> {
                    String desc = descField.getText().trim();
                    if (!desc.isEmpty()) {
                        LocalDate dueDate = null;
                        try {
                            dueDate = LocalDate.parse(dateField.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                            dateField.setBorder(BorderFactory.createLineBorder(currentBorderColor)); 
                        } catch (DateTimeParseException ex) {
                            dateField.setBorder(BorderFactory.createLineBorder(Color.RED, 1)); 
                            return;
                        }
                        
                        Task task = new Task(desc, (Priority) prioCombo.getSelectedItem(), dueDate);
                        project.tasks.add(task);
                        saveData();

                        taskFormPanel.removeAll();
                        taskFormPanel.add(expandAddTaskBtn, BorderLayout.CENTER);
                        taskFormPanel.revalidate();
                        taskFormPanel.repaint();

                        renderTasks();
                        updateProgress();
                        TodoApp.instance.updateGlobalStatsPanel();
                    }
                };

                submitBtn.addActionListener(submitAction);
                descField.addActionListener(submitAction);
                dateField.addActionListener(submitAction);

                cancelBtn.addActionListener(ev -> {
                    taskFormPanel.removeAll();
                    taskFormPanel.add(expandAddTaskBtn, BorderLayout.CENTER);
                    taskFormPanel.revalidate();
                    taskFormPanel.repaint();
                });
            });

            // Start glowing timer loop if initialized in completed state
            int total = project.tasks.size();
            int completed = 0;
            for (Task t : project.tasks) {
                if (t.completed) completed++;
            }
            if (total > 0 && completed == total) {
                startGlow();
            }

            updateProgress();
            renderTasks();
        }

        public void startGlow() {
            isGlowing = true;
            if (glowTimer != null && glowTimer.isRunning()) return;
            
            // 1.5s glow cycle (sine wave alpha)
            glowTimer = new Timer(16, new ActionListener() {
                double angle = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    angle += 0.04; 
                    glowAlpha = (float)(0.40 + 0.30 * Math.sin(angle)); // pulse
                    repaint();
                    if (!isGlowing) {
                        glowTimer.stop();
                        glowAlpha = 0.0f;
                        repaint();
                    }
                }
            });
            glowTimer.start();
        }

        public void stopGlow() {
            isGlowing = false;
        }

        public void renderTasks() {
            tasksContainer.removeAll();
            
            List<Task> displayTasks = new ArrayList<>(project.tasks);

            if (cardFilterPriority.contains("High")) {
                displayTasks.removeIf(t -> t.priority != Priority.HIGH);
            } else if (cardFilterPriority.contains("Med")) {
                displayTasks.removeIf(t -> t.priority != Priority.MEDIUM);
            } else if (cardFilterPriority.contains("Low")) {
                displayTasks.removeIf(t -> t.priority != Priority.LOW);
            }

            if ("Active".equals(currentFilter)) {
                displayTasks.removeIf(t -> t.completed);
            } else if ("Completed".equals(currentFilter)) {
                displayTasks.removeIf(t -> !t.completed);
            } else if ("Overdue".equals(currentFilter)) {
                displayTasks.removeIf(t -> !t.isOverdue());
            }

            if (cardSortOption.contains("Priority")) {
                displayTasks.sort((t1, t2) -> t2.priority.ordinal() - t1.priority.ordinal());
            } else if (cardSortOption.contains("Date")) {
                displayTasks.sort((t1, t2) -> {
                    if (t1.dueDate == null && t2.dueDate == null) return 0;
                    if (t1.dueDate == null) return 1;
                    if (t2.dueDate == null) return -1;
                    return t1.dueDate.compareTo(t2.dueDate);
                });
            }

            for (Task t : displayTasks) {
                TaskRow row = new TaskRow(t, this);
                tasksContainer.add(row);
            }
            
            checkCelebrationState();

            tasksContainer.revalidate();
            tasksContainer.repaint();
        }

        public void checkCelebrationState() {
            for (Component c : tasksContainer.getComponents()) {
                if (c instanceof CelebrationPanel) {
                    tasksContainer.remove(c);
                }
            }

            int total = project.tasks.size();
            int completed = 0;
            for (Task t : project.tasks) {
                if (t.completed) completed++;
            }

            if (total > 0 && completed == total) {
                CelebrationPanel cPanel = new CelebrationPanel("🎉 All tasks completed in " + project.name + "!");
                tasksContainer.add(cPanel);
            }
        }

        public void updateProgress() {
            int total = project.tasks.size();
            int completed = 0;
            for (Task t : project.tasks) {
                if (t.completed) completed++;
            }
            int percent = total == 0 ? 0 : (completed * 100 / total);
            progressBar.setTargetValue(percent);
            counterLabel.setText(completed + "/" + total + " task" + (total != 1 ? "s" : ""));
            percentLabel.setText(percent + "%");
        }

        public void removeTask(TaskRow row, Task t) {
            project.tasks.remove(t);
            saveData();
            tasksContainer.remove(row);
            tasksContainer.revalidate();
            tasksContainer.repaint();
            updateProgress();
            TodoApp.instance.updateGlobalStatsPanel();
            checkCelebrationState();
        }

        public void animateRemoval() {
            if (fadeTimer != null && fadeTimer.isRunning()) return;
            fadeTimer = new Timer(15, e -> {
                alpha -= 0.05f;
                shrinkScale -= 0.05;
                if (alpha <= 0.0f) {
                    alpha = 0f;
                    shrinkScale = 0.0;
                    fadeTimer.stop();
                    TodoApp.removeProject(this, project);
                }
                revalidate();
                repaint();
            });
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw Gold Glow pulsing border
            if (isGlowing && glowAlpha > 0) {
                Graphics2D gGlow = (Graphics2D) g.create();
                gGlow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                int shadowOffset = 4;
                int cardW = w - shadowOffset * 2;
                int cardH = h - shadowOffset * 2;

                if (cardW > 0 && cardH > 8) {
                    int yOffset = 6;
                    cardH -= 12;

                    // Gold gradient outer pulsing stroke
                    gGlow.setColor(new Color(245, 158, 11, (int)(glowAlpha * 255)));
                    gGlow.setStroke(new BasicStroke(4.0f));
                    gGlow.drawRoundRect(shadowOffset, yOffset + shadowOffset, cardW, cardH, 12, 12);
                }
                gGlow.dispose();
            }
        }
    }

    // Double-Border Completed Milestone Panel inside expanded card
    public static class CelebrationPanel extends JPanel {
        private final String message;
        public CelebrationPanel(String message) {
            this.message = message;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 44));
            setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(isDarkMode ? new Color(49, 46, 129) : new Color(245, 243, 255));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            // Double Border
            g2d.setColor(currentAccentColor);
            g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 6, 6);

            // Label text
            g2d.setColor(isDarkMode ? Color.WHITE : currentAccentColor);
            g2d.setFont(getModernFont(Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(message)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(message, tx, ty);

            g2d.dispose();
        }
    }

    // Custom Task Row Component inside cards
    public static class TaskRow extends JPanel {
        private final Task task;
        private final ProjectCard parentCard;
        private float alpha = 1.0f;
        private double shrinkScale = 1.0f;
        private Timer fadeTimer;
        private boolean isHovered = false;

        public TaskRow(Task task, ProjectCard parentCard) {
            this.task = task;
            this.parentCard = parentCard;
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });

            ModernCheckBox checkBox = new ModernCheckBox(task.name, task.completed);
            String labelText = task.name;
            String query = searchQuery.toLowerCase().trim();
            if (!query.isEmpty() && labelText.toLowerCase().contains(query)) {
                int start = labelText.toLowerCase().indexOf(query);
                int end = start + query.length();
                String before = labelText.substring(0, start);
                String match = labelText.substring(start, end);
                String after = labelText.substring(end);
                labelText = "<html>" + before + "<span style='background-color: #fef08a; color: #000000; font-weight: bold;'>" + match + "</span>" + after + "</html>";
            }
            checkBox.setText(labelText);

            checkBox.addActionListener(e -> {
                task.completed = checkBox.isSelected();
                parentCard.updateProgress();
                saveData();
                TodoApp.instance.updateGlobalStatsPanel();
                parentCard.renderTasks(); 

                // Milestone completion toast & particle triggers
                int total = parentCard.project.tasks.size();
                int completed = 0;
                for (Task tk : parentCard.project.tasks) {
                    if (tk.completed) completed++;
                }
                
                if (total > 0 && completed == total) {
                    // 1. Play auditory chime beep
                    try {
                        Toolkit.getDefaultToolkit().beep();
                    } catch (Exception ex) {
                        // ignore toolkit sound failure
                    }

                    // 2. Trigger gold border glow pulse loop
                    parentCard.startGlow();

                    // 3. Trigger sliding toast notification
                    TodoApp.showToast("🎉 All tasks completed in " + parentCard.project.name + "!");

                    // 4. Trigger global confetti showers & star explosions
                    TodoApp.instance.triggerGlobalCelebration(parentCard);
                } else {
                    // Turn off card glowing
                    parentCard.stopGlow();
                }
                
                repaint();
            });

            JPanel leftPane = new JPanel(new BorderLayout());
            leftPane.setOpaque(false);
            leftPane.add(checkBox, BorderLayout.CENTER);
            add(leftPane, BorderLayout.CENTER);

            JPanel rightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            rightPane.setOpaque(false);

            JLabel priorityBadge = new JLabel(task.priority.label) {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(task.priority.getBgColor(isDarkMode));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2d.dispose();
                    super.paintComponent(g);
                }
            };
            priorityBadge.setFont(getModernFont(Font.BOLD, 10));
            priorityBadge.setForeground(task.priority.getFgColor(isDarkMode));
            priorityBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            rightPane.add(priorityBadge);

            JLabel dueDateBadge = new JLabel("📅 " + (task.dueDate != null ? task.dueDate.format(displayFormatter) : "")) {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (task.isOverdue()) {
                        g2d.setColor(new Color(254, 226, 226));
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g2d.setColor(new Color(239, 68, 68));
                        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    } else {
                        g2d.setColor(isDarkMode ? new Color(51, 65, 85) : new Color(241, 245, 249));
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g2d.setColor(currentBorderColor);
                        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    }
                    g2d.dispose();
                    super.paintComponent(g);
                }
            };
            dueDateBadge.setFont(getModernFont(Font.PLAIN, 10));
            dueDateBadge.setForeground(task.isOverdue() ? new Color(239, 68, 68) : currentTextSecondary);
            dueDateBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            rightPane.add(dueDateBadge);

            ModernButton delBtn = new ModernButton("×", new Color(248, 250, 252), new Color(254, 226, 226)) {
                {
                    setForeground(new Color(148, 163, 184));
                    setFont(getModernFont(Font.BOLD, 16));
                    setPreferredSize(new Dimension(24, 24));
                }
            };
            delBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { delBtn.setForeground(new Color(220, 38, 38)); }
                public void mouseExited(MouseEvent e) { delBtn.setForeground(new Color(148, 163, 184)); }
            });
            delBtn.addActionListener(e -> removeAnimate());
            rightPane.add(delBtn);

            add(rightPane, BorderLayout.EAST);
        }

        public void removeAnimate() {
            if (fadeTimer != null && fadeTimer.isRunning()) return;
            fadeTimer = new Timer(15, e -> {
                alpha -= 0.08f;
                shrinkScale -= 0.08;
                if (alpha <= 0f) {
                    alpha = 0f;
                    shrinkScale = 0.0;
                    fadeTimer.stop();
                    parentCard.removeTask(this, task);
                }
                revalidate();
                repaint();
            });
            fadeTimer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = (int) (d.height * shrinkScale);
            return d;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.clipRect(0, 0, getWidth(), getHeight());

            if (task.isOverdue()) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(isHovered ? new Color(254, 226, 226, 80) : new Color(254, 226, 226, 40));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2d.setColor(new Color(239, 68, 68, 80));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            } else if (isHovered) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(isDarkMode ? new Color(51, 65, 85, 80) : new Color(241, 245, 249, 100));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            }

            super.paint(g2d);
            g2d.dispose();
        }
    }

    // Collapsible Panel for slide expandable sections
    public static class CollapsiblePanel extends JPanel {
        private final JPanel contentPanel;
        private double expansion = 0.0;
        private Timer slideTimer;
        private boolean expanded = false;

        public CollapsiblePanel(JPanel contentPanel) {
            this.contentPanel = contentPanel;
            setOpaque(false);
            setLayout(new BorderLayout());
            add(contentPanel, BorderLayout.CENTER);
            setVisible(false);
        }

        public boolean isExpanded() { return expanded; }

        public void setExpanded(boolean expanded, boolean animate) {
            if (this.expanded == expanded) return;
            this.expanded = expanded;

            if (!animate) {
                expansion = expanded ? 1.0 : 0.0;
                setVisible(expanded);
                revalidate();
                repaint();
                return;
            }

            if (slideTimer != null && slideTimer.isRunning()) slideTimer.stop();

            if (expanded) setVisible(true);

            double target = expanded ? 1.0 : 0.0;
            slideTimer = new Timer(15, e -> {
                expansion += (target - expansion) * 0.20;
                if (Math.abs(expansion - target) < 0.01) {
                    expansion = target;
                    if (!expanded) setVisible(false);
                    slideTimer.stop();
                }
                revalidate();
                repaint();
            });
            slideTimer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = contentPanel.getPreferredSize();
            d.height = (int) (d.height * expansion);
            return d;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension d = contentPanel.getMinimumSize();
            d.height = (int) (d.height * expansion);
            return d;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension d = contentPanel.getMaximumSize();
            d.height = (int) (d.height * expansion);
            return d;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.clipRect(0, 0, getWidth(), getHeight());
            super.paint(g2d);
            g2d.dispose();
        }
    }

    // Custom ProgressBar with easing fill animations
    public static class ModernProgressBar extends JProgressBar {
        private double animValue = 0.0;
        private Timer animTimer;

        public ModernProgressBar() {
            setOpaque(false);
            setBorder(null);
            setStringPainted(false);
            setMinimum(0);
            setMaximum(100);
            setValue(0);
        }

        public void setTargetValue(int value) {
            double target = Math.max(0, Math.min(100, value));
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();

            animTimer = new Timer(15, e -> {
                animValue += (target - animValue) * 0.12;
                if (Math.abs(animValue - target) < 0.1) {
                    animValue = target;
                    animTimer.stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int round = h;

            g2d.setColor(isDarkMode ? new Color(51, 65, 85) : new Color(241, 245, 249));
            g2d.fillRoundRect(0, 0, w, h, round, round);

            int fillW = (int) (w * (animValue / 100.0));
            if (fillW > 0) {
                g2d.setColor(currentAccentColor);
                g2d.fillRoundRect(0, 0, fillW, h, round, round);
            }
            g2d.dispose();
        }
    }

    // Custom Styled Checkbox with spring scaling
    public static class ModernCheckBox extends JCheckBox {
        private double checkScale = 0.0;
        private double velocity = 0.0;
        private Timer springTimer;
        private float hoverPercent = 0f;
        private Timer hoverTimer;

        public ModernCheckBox(String text, boolean initialSelected) {
            super(text);
            setSelected(initialSelected);
            setOpaque(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(getModernFont(Font.PLAIN, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            checkScale = initialSelected ? 1.0 : 0.0;

            addItemListener(e -> {
                double targetScale = isSelected() ? 1.0 : 0.0;
                if (springTimer != null && springTimer.isRunning()) springTimer.stop();
                
                springTimer = new Timer(15, ev -> {
                    double stiffness = 0.28;
                    double damping = 0.52;
                    double force = (targetScale - checkScale) * stiffness - velocity * damping;
                    velocity += force;
                    checkScale += velocity;

                    if (Math.abs(checkScale - targetScale) < 0.001 && Math.abs(velocity) < 0.001) {
                        checkScale = targetScale;
                        velocity = 0.0;
                        springTimer.stop();
                    }
                    repaint();
                });
                springTimer.start();
            });

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { animateHover(true); }
                public void mouseExited(MouseEvent e) { animateHover(false); }
            });
        }

        private void animateHover(boolean hover) {
            if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
            float target = hover ? 1f : 0f;
            hoverTimer = new Timer(15, e -> {
                hoverPercent += (target - hoverPercent) * 0.2f;
                if (Math.abs(hoverPercent - target) < 0.01f) {
                    hoverPercent = target;
                    hoverTimer.stop();
                }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(getText().replaceAll("<[^>]*>", "")); // Strip HTML tags
            int width = 4 + 20 + 10 + textWidth + 10;
            int height = Math.max(32, fm.getHeight() + 10);
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int boxSize = 20;
            int y = (getHeight() - boxSize) / 2;
            int x = 4;

            Color normalColor = isDarkMode ? new Color(51, 65, 85) : new Color(241, 245, 249);
            Color hoverColor = isDarkMode ? new Color(71, 85, 105) : new Color(226, 232, 240);
            Color uncheckedBg = lerpColor(normalColor, hoverColor, hoverPercent);
            Color checkedBg = currentAccentColor;

            float clampedScale = (float) Math.max(0.0, Math.min(1.0, checkScale));
            Color bg = lerpColor(uncheckedBg, checkedBg, clampedScale);
            g2d.setColor(bg);
            g2d.fillRoundRect(x, y, boxSize, boxSize, 6, 6);

            Color uncheckedBorder = isDarkMode ? new Color(71, 85, 105) : new Color(203, 213, 225);
            Color checkedBorder = checkedBg;
            Color borderColor = lerpColor(uncheckedBorder, checkedBorder, clampedScale);
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(x, y, boxSize, boxSize, 6, 6);

            if (checkScale > 0) {
                Graphics2D gCheck = (Graphics2D) g2d.create();
                double cx = x + boxSize / 2.0;
                double cy = y + boxSize / 2.0;
                gCheck.translate(cx, cy);
                gCheck.scale(checkScale, checkScale);
                gCheck.translate(-cx, -cy);

                gCheck.setColor(Color.WHITE);
                gCheck.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int startX = x + 5;
                int startY = y + 10;
                int midX = x + 9;
                int midY = y + 14;
                int endX = x + 15;
                int endY = y + 6;
                gCheck.drawLine(startX, startY, midX, midY);
                gCheck.drawLine(midX, midY, endX, endY);
                gCheck.dispose();
            }

            g2d.setFont(getFont());
            g2d.setColor(isSelected() ? currentTextSecondary : currentTextPrimary);

            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + boxSize + 10;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            String text = getText();
            
            if (text.startsWith("<html>")) {
                JLabel tempLabel = new JLabel(text);
                tempLabel.setFont(getFont());
                tempLabel.setForeground(isSelected() ? currentTextSecondary : currentTextPrimary);
                tempLabel.setSize(tempLabel.getPreferredSize());
                
                Graphics2D gHtml = (Graphics2D) g2d.create();
                gHtml.translate(textX, (getHeight() - tempLabel.getHeight()) / 2);
                tempLabel.paint(gHtml);
                gHtml.dispose();
            } else {
                g2d.drawString(text, textX, textY);
                if (isSelected() || checkScale > 0) {
                    int textWidth = fm.stringWidth(text);
                    g2d.setColor(currentTextSecondary);
                    g2d.setStroke(new BasicStroke(1.5f));
                    int lineY = (getHeight() / 2) + 1;
                    int strikeWidth = (int) (textWidth * clampedScale);
                    g2d.drawLine(textX, lineY, textX + strikeWidth, lineY);
                }
            }

            g2d.dispose();
        }
    }

    // Custom Button with scale hover triggers
    public static class ModernButton extends JButton {
        private float hoverPercent = 0f;
        private float scalePercent = 0f;
        private Timer hoverTimer;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private Color bgNormal = LIGHT_ACCENT;
        private Color bgHover = LIGHT_ACCENT_HOVER;

        public ModernButton(String text) {
            super(text);
            this.bgNormal = currentAccentColor;
            this.bgHover = currentAccentHover;
            initButton();
        }

        public ModernButton(String text, Color normalColor, Color hoverColor) {
            super(text);
            this.bgNormal = normalColor;
            this.bgHover = hoverColor;
            initButton();
        }

        private void initButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(getModernFont(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHovered = true; animateHover(true); }
                public void mouseExited(MouseEvent e) { isHovered = false; animateHover(false); }
                public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
            });
        }

        private void animateHover(boolean hover) {
            if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
            float target = hover ? 1f : 0f;
            hoverTimer = new Timer(15, e -> {
                hoverPercent += (target - hoverPercent) * 0.2f;
                scalePercent += (target - scalePercent) * 0.2f;
                if (Math.abs(hoverPercent - target) < 0.01f) {
                    hoverPercent = target;
                    scalePercent = target;
                    hoverTimer.stop();
                }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double scale = 1.0 + 0.02 * scalePercent;
            if (isPressed) scale = 0.98;

            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            g2d.translate(cx, cy);
            g2d.scale(scale, scale);
            g2d.translate(-cx, -cy);

            Color currentNormal = bgNormal;
            Color currentHover = bgHover;
            
            if (bgNormal.equals(LIGHT_ACCENT)) {
                currentNormal = currentAccentColor;
                currentHover = currentAccentHover;
            }

            Color bgColor = lerpColor(currentNormal, currentHover, hoverPercent);
            g2d.setColor(bgColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2d.setColor(getForeground());
            g2d.setFont(getFont());
            FontMetrics fm = g2d.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2d);
            int x = (int) ((getWidth() - r.getWidth()) / 2);
            int y = (int) ((getHeight() - r.getHeight()) / 2 + fm.getAscent());
            g2d.drawString(getText(), x, y);

            g2d.dispose();
        }
    }

    // Custom Styled TextField
    public static class ModernTextField extends JTextField {
        private final String placeholder;

        public ModernTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setForeground(currentTextPrimary);
            setCaretColor(currentAccentColor);
            setFont(getModernFont(Font.PLAIN, 13));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(currentCardBgColor);
            g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            g2d.setColor(currentBorderColor);
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            super.paintComponent(g2d);

            if (getText().isEmpty() && placeholder != null) {
                g2d.setColor(currentTextSecondary);
                FontMetrics fm = g2d.getFontMetrics();
                int x = getInsets().left;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(placeholder, x, y);
            }
            g2d.dispose();
        }
    }

    // ComboBox renderer UI mapping
    private static class ModernComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            if (isSelected) {
                setBackground(currentAccentColor);
                setForeground(Color.WHITE);
            } else {
                setBackground(currentCardBgColor);
                setForeground(currentTextPrimary);
            }
            setFont(getModernFont(Font.PLAIN, 12));
            return this;
        }
    }

    // Custom scrollbar look
    private static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(currentBgColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(isDarkMode ? new Color(71, 85, 105) : new Color(203, 213, 225));
            g2d.fillRoundRect(thumbBounds.x + 4, thumbBounds.y + 2, thumbBounds.width - 8, thumbBounds.height - 4, 6, 6);
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Keep cross platform look and feel if system look fails
            }
            new TodoApp().setVisible(true);
        });
    }
}
