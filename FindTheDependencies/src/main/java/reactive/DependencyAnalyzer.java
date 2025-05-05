package reactive;

import javax.swing.*;
import java.io.IOException;

public class DependencyAnalyzer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DependencyAnalyzerApp ui = new DependencyAnalyzerApp();
            DependencyAnalyzerController controller = null;
            try {
                controller = new DependencyAnalyzerController(ui, new DependencyAnalyzerEngine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ui.setController(controller);
            ui.startApp();
        });
    }
}
