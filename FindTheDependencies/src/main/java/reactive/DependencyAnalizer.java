package reactive;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;


public class DependencyAnalizer {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dependency Analyzer");
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> lista = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(lista);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        DependencyAnalizerReactive depReactive = new DependencyAnalizerReactive();

        Path root = Path.of("src\\main\\java");

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(e -> {
            listModel.clear();
            try (Stream<Path> files = Files.walk(root)) {
                Observable.fromIterable(files
                                .filter(p -> p.toString().endsWith(".java"))
                                .toList())
                        .flatMap(path ->
                                depReactive.parseClassDependenciesIncrementally(path)
                                        .subscribeOn(Schedulers.io())
                        )
                        .observeOn(Schedulers.trampoline())
                        .subscribe(
                                riga -> SwingUtilities.invokeLater(() -> listModel.addElement(riga)),
                                Throwable::printStackTrace,
                                () -> System.out.println("Analisi completata.")
                        );

            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });

        frame.add(analyzeButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }
}
