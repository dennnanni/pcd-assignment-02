package reactive;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DependencyAnalyzerEngine {
    private final DependencyAnalizerReactive reactiveParser = new DependencyAnalizerReactive();

    public Flowable<Dependency> analyzePath(Path root) {
        return Flowable.using(
                () -> Files.walk(root),
                files -> Flowable.fromStream(files)
                        .filter(p -> p.toString().endsWith(".java"))
                        .flatMap(path -> reactiveParser.parseClassDependenciesIncrementally(path)
                                .subscribeOn(Schedulers.io())),
                Stream::close
        ).observeOn(Schedulers.trampoline());
    }
}

