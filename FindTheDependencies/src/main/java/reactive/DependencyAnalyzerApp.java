package reactive;

import javax.swing.*;
import java.awt.*;


public class DependencyAnalyzerApp {
    private DefaultListModel<String> listModelClasses = new DefaultListModel<>();
    private DefaultListModel<String> listModelDependencies = new DefaultListModel<>();
    private JButton classesButton = new JButton("0");
    private JButton dependenciesButton = new JButton("0");
    private JLabel pathField = new JLabel();
    private DependencyAnalyzerController controller;

    public void setController(DependencyAnalyzerController controller) {
        this.controller = controller;
    }

    public void startApp() {
        JFrame frame = new JFrame("Dependency Analyzer");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildControlPanel(), BorderLayout.NORTH);
        panel.add(buildSplitPanel(), BorderLayout.CENTER);

        frame.setContentPane(panel);

        frame.setVisible(true);
    }


    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectPathButton = new JButton("Select Path");

        selectPathButton.addActionListener(e -> controller.onSelectPath());

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(e -> controller.onAnalyze());

        panel.add(selectPathButton);
        panel.add(pathField);
        panel.add(new JLabel("Classes: "));
        panel.add(classesButton);
        panel.add(new JLabel("Dependencies: "));
        panel.add(dependenciesButton);
        panel.add(analyzeButton);

        return panel;
    }

    private JSplitPane buildSplitPanel() {
        JList<String> classList = new JList<>(listModelClasses);
        JList<String> depList = new JList<>(listModelDependencies);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(classList), new JScrollPane(depList));
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        return splitPane;
    }


    public void updatePathLabel(String path) {
        pathField.setText(path);
    }

    public void addClass(String className) {
        if (!listModelClasses.contains(className)) {
            listModelClasses.addElement(className);
            classesButton.setText(String.valueOf(listModelClasses.size()));
        }
    }

    public void addDependency(String dep) {
        listModelDependencies.addElement(dep);
        dependenciesButton.setText(String.valueOf(listModelDependencies.size()));
    }

    public void clearAll() {
        SwingUtilities.invokeLater(() -> {
            listModelClasses.clear();
            listModelDependencies.clear();
            classesButton.setText("0");
            dependenciesButton.setText("0");
        });
    }
}
