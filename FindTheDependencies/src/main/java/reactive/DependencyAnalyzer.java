package reactive;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import reports.ClassDepsReport;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public class DependencyAnalyzer {
    private static DefaultListModel<String> listModelClasses = new DefaultListModel<>();
    private static DefaultListModel<String> listModelDependencies = new DefaultListModel<>();
    private static Map<String, ClassDepsReport> mapClassDeps = new HashMap<>();
    private static Path rootPath = null;
    private static DependencyAnalizerReactive depReactive = new DependencyAnalizerReactive();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dependency Analyzer");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        drawControlPanel(mainPanel);
        drawSplitPanel(mainPanel);

        frame.setContentPane(mainPanel);

        frame.setVisible(true);
    }

    private static void drawSplitPanel(JPanel mainPanel) {
        JScrollPane leftScroll = new JScrollPane(new JList<>(listModelClasses));
        JScrollPane rightScroll = new JScrollPane(new JList<>(listModelDependencies));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private static void drawControlPanel(JPanel mainPanel) {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        drawSelectPathControl(controlPanel);

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(e -> {
            // Implement the action for the Analyze button
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

    private static void interfaceUpdate(Path root) {
        listModelClasses.clear();
        try (Stream<Path> files = Files.walk(root)) {
            Observable<Dependency> dependencyObservable = Observable.fromIterable(files
                            .filter(p -> p.toString().endsWith(".java"))
                            .toList())
                    .flatMap(path ->
                            depReactive.parseClassDependenciesIncrementally(path)
                                    .subscribeOn(Schedulers.io())
                    )
                    .observeOn(Schedulers.trampoline());

            // Subcription to update classes list
            dependencyObservable.subscribe(
                    riga -> SwingUtilities.invokeLater(() -> {
                        if (!listModelClasses.contains(riga.className())) {
                            listModelClasses.addElement(riga.className());
                        }
                    }),
                    Throwable::printStackTrace,
                    () -> System.out.println("Analisi completata.")
            );

            // Subscription to update dependencies list
            dependencyObservable.subscribe(riga -> {
                if (riga.dependency() != null) {
                    SwingUtilities.invokeLater(() -> listModelDependencies.addElement(riga.dependency()));
                }
            }, Throwable::printStackTrace);

            // Subscription to update class reports
            dependencyObservable.subscribe(riga -> {
                if (mapClassDeps.containsKey(riga.className())) {
                    mapClassDeps.get(riga.className()).addDependency(riga.dependency());
                } else {
                    ClassDepsReport classDepsReport = new ClassDepsReport(riga.className(), riga.packageName(), Set.of(riga.dependency()));
                    mapClassDeps.put(riga.className(), classDepsReport);
                }
            },
                    Throwable::printStackTrace
            );


        } catch (IOException exc) {
            exc.printStackTrace();
        }

    }

    private static void drawSelectPathControl(JPanel mainPanel) {
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
