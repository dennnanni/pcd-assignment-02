package reactive;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Set;


public class DependencyAnalyzerApp {
    private DefaultListModel<String> listModelClasses = new DefaultListModel<>();
    private DefaultListModel<String> listModelDependencies = new DefaultListModel<>();
    private JButton classesButton = new JButton("0");
    private JButton dependenciesButton = new JButton("0");
    private JLabel pathField = new JLabel();
    private DependencyAnalyzerController controller;

    // Visualizzazione a gruppi
    private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Packages");
    private DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private JTree classTree = new JTree(treeModel);


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

        classTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
            if (selectedNode == null) return;

            if (selectedNode.isRoot()) {
                controller.onRootSelected();
            } else if (selectedNode.isLeaf()) {
                String className = selectedNode.getUserObject().toString();

                DefaultMutableTreeNode packageNode = (DefaultMutableTreeNode) selectedNode.getParent();
                String packageName = packageNode.getUserObject().toString();

                controller.onClassSelected(packageName, className);
            } else {
                String packageName = selectedNode.getUserObject().toString();
                controller.onPackageSelected(packageName);
            }
        });

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
        JList<String> depList = new JList<>(listModelDependencies);
        JScrollPane classScroll = new JScrollPane(classTree);
        JScrollPane depScroll = new JScrollPane(depList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                classScroll, depScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        return splitPane;
    }

    public void addClassToTree(String pkg, String className) {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode packageNode = findOrCreatePackageNode(pkg);
            if (!isClassPresentInPackage(packageNode, className)) {
                DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(className);
                packageNode.add(classNode);
            }
            treeModel.reload(); // Refresh view
            classesButton.setText(String.valueOf(countClasses()));
        });
    }

    private boolean isClassPresentInPackage(DefaultMutableTreeNode packageNode, String className) {
        for (int i = 0; i < packageNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) packageNode.getChildAt(i);
            if (className.equals(child.getUserObject())) {
                return true;
            }
        }
        return false;
    }

    private DefaultMutableTreeNode findOrCreatePackageNode(String pkg) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            if (pkg.equals(child.getUserObject())) {
                return child;
            }
        }

        DefaultMutableTreeNode newPackageNode = new DefaultMutableTreeNode(pkg);
        rootNode.add(newPackageNode);
        return newPackageNode;
    }

    private int countClasses() {
        int count = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            count += rootNode.getChildAt(i).getChildCount();
        }
        return count;
    }

    public void updatePathLabel(String path) {
        pathField.setText(path);
    }

    public void addDependency(String dep) {
        if (!listModelDependencies.contains(dep)) {
            listModelDependencies.addElement(dep);
            dependenciesButton.setText(String.valueOf(listModelDependencies.size()));
        }
    }

    public void updateDependencyPanel(Set<String> deps) {
        SwingUtilities.invokeLater(() -> {
            listModelDependencies.clear();
            for (String dep : deps) {
                listModelDependencies.addElement(dep);
            }
            dependenciesButton.setText(String.valueOf(listModelDependencies.size()));
        });
    }

    public void clearAll() {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            treeModel.reload();
            listModelDependencies.clear();
            classesButton.setText("0");
            dependenciesButton.setText("0");
        });
    }
}
