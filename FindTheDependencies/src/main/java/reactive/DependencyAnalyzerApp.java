package reactive;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import reports.ClassDepsReport;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


public class DependencyAnalyzerApp {
    private static DefaultListModel<String> listModelClasses = new DefaultListModel<>();
    private static DefaultListModel<String> listModelDependencies = new DefaultListModel<>();
    private static ConcurrentHashMap<String, ClassDepsReport> mapClassDeps = new ConcurrentHashMap<>();
    private static Path rootPath = null;
    private static DependencyAnalizerReactive depReactive = new DependencyAnalizerReactive();


    public void startApp() {
        JFrame frame = new JFrame("Dependency Analyzer");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        drawControlPanel(mainPanel);
        drawSplitPanel(mainPanel);

        frame.setContentPane(mainPanel);

        frame.setVisible(true);
    }

    private void drawSplitPanel(JPanel mainPanel) {
        JScrollPane leftScroll = new JScrollPane(new JList<>(listModelClasses));
        JScrollPane rightScroll = new JScrollPane(new JList<>(listModelDependencies));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private void drawControlPanel(JPanel mainPanel) {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        drawSelectPathControl(controlPanel);

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(e -> {
            if (rootPath != null) {
                interfaceUpdate(rootPath);
            }
        });

        JButton classesButton = new JButton("0");
        classesButton.addActionListener(e -> {
            // Implement the action for the Classes button
            // Reset della visualizzazione
        });

        JButton dependenciesButton = new JButton("0");
        dependenciesButton.addActionListener(e -> {
            // Implement the action for the Dependencies button
            // Reset della visualizzazione
        });

        controlPanel.add(new JLabel("Classes: "));
        controlPanel.add(classesButton);
        controlPanel.add(new JLabel("Dependencies: "));
        controlPanel.add(dependenciesButton);
        controlPanel.add(analyzeButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
    }

    private void interfaceUpdate(Path root) {
        listModelClasses.clear();
        Flowable<Dependency> dependencyFlowable = Flowable.using(
                () -> Files.walk(root),
                files -> Flowable.fromStream(files)
                        .filter(p -> p.toString().endsWith(".java"))
                        .flatMap(path ->
                                depReactive.parseClassDependenciesIncrementally(path)
                                        .subscribeOn(Schedulers.io())
                        ),
                Stream::close
        ).observeOn(Schedulers.trampoline());

        Flowable<Dependency> sharedFlowable = dependencyFlowable.share();
        // Subcription to update classes list
        sharedFlowable.subscribe(
                riga -> SwingUtilities.invokeLater(() -> {
                    if (!listModelClasses.contains(riga.className())) {
                        listModelClasses.addElement(riga.className());
                    }
                }),
                Throwable::printStackTrace,
                () -> System.out.println("Analisi completata.")
        );

        // Subscription to update dependencies list
        sharedFlowable.subscribe(riga -> {
            if (riga.dependency() != null) {
                SwingUtilities.invokeLater(() -> listModelDependencies.addElement(riga.dependency()));

                if (mapClassDeps.containsKey(riga.className())) {
                    mapClassDeps.get(riga.className()).addDependency(riga.dependency());
                } else {
                    ClassDepsReport classDepsReport = new ClassDepsReport(riga.className(), riga.packageName(), Set.of(riga.dependency()));
                    mapClassDeps.put(riga.className(), classDepsReport);
                }
            }
        },
                Throwable::printStackTrace
        );
    }

    private void drawSelectPathControl(JPanel mainPanel) {
        JButton selectPathButton = new JButton("Select path");
        JLabel pathField = new JLabel();
        selectPathButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                Path selectedPath = fileChooser.getSelectedFile().toPath();
                rootPath = selectedPath;
                pathField.setText(selectedPath.toString());
            }
        });
        mainPanel.add(selectPathButton);
        mainPanel.add(pathField);
    }
}
