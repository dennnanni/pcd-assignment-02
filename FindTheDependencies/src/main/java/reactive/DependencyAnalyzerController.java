package reactive;

import io.reactivex.rxjava3.core.Flowable;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyAnalyzerController {
    private final DependencyAnalyzerApp ui;
    private final DependencyAnalyzerEngine engine;
    private Path rootPath = Path.of(new java.io.File(".").getCanonicalPath(), "test-project");
    private List<Dependency> mapClassDeps = new ArrayList<>();

    public DependencyAnalyzerController(DependencyAnalyzerApp ui, DependencyAnalyzerEngine engine) throws IOException {
        this.ui = ui;
        this.ui.updatePathLabel(rootPath.toString());
        this.engine = engine;
    }

    public void onSelectPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            rootPath = chooser.getSelectedFile().toPath();
            ui.updatePathLabel(rootPath.toString());
        }
    }

    public void onAnalyze() {
        if (rootPath == null) return;
        ui.clearAll();
        Flowable<Dependency> stream = engine.analyzePath(rootPath);

        stream.subscribe(dep -> {
            SwingUtilities.invokeLater(() -> {
                ui.addDependency(dep.dependency());
                ui.addClassToTree(dep.packageName(), dep.className());
            });
            Dependency classDepsReport = new Dependency(dep.className(), dep.packageName(), dep.dependency());
            mapClassDeps.add(classDepsReport);
        });
    }

    public void onClassSelected(String packageName, String className) {
         Set<String> classDeps = mapClassDeps.stream().filter(
                cdr -> cdr.className().equals(className) &&
                        cdr.packageName().equals(packageName))
                .map(Dependency::dependency)
                .collect(Collectors.toSet());

        ui.updateDependencyPanel(classDeps);
    }
}

