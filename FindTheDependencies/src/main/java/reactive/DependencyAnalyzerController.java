package reactive;

import io.reactivex.rxjava3.core.Flowable;
import reports.ClassDepsReport;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyAnalyzerController {
    private final DependencyAnalyzerApp ui;
    private final DependencyAnalyzerEngine engine;
    private Path rootPath = Path.of(new java.io.File(".").getCanonicalPath(), "test-project");
    private ConcurrentHashMap<String, ClassDepsReport> mapClassDeps = new ConcurrentHashMap<>();

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
        Flowable<Dependency> stream = engine.analyzePath(rootPath).share();

        stream.subscribe(dep -> SwingUtilities.invokeLater(() -> ui.addClass(dep.className())));
        stream.subscribe(dep -> { SwingUtilities.invokeLater(() ->
            ui.addDependency(dep.dependency()));
            if (mapClassDeps.containsKey(dep.className())) {
                mapClassDeps.get(dep.className()).addDependency(dep.dependency());
            } else {
                ClassDepsReport classDepsReport = new ClassDepsReport(dep.className(), dep.packageName(), Set.of(dep.dependency()));
                mapClassDeps.put(dep.className(), classDepsReport);
            }
        });
    }
}

