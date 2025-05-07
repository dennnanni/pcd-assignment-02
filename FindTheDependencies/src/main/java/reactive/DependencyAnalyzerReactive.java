package reactive;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.file.Path;

public class DependencyAnalyzerReactive {

//    public record ClassDependency(String className, String packageName, Set<String> usedTypes) {}

    public Flowable<Dependency> parseClassDependenciesIncrementally(Path javaFile) {
        return Flowable.create(emitter -> {
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                String packageName = cu.getPackageDeclaration()
                        .map(NodeWithName::getNameAsString)
                        .orElse(javaFile.getParent().getFileName().toString());
                String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::getNameAsString).orElse("Anonymous");

                cu.findAll(ClassOrInterfaceType.class).stream()
                        .map(ClassOrInterfaceType::getNameAsString)
                        .distinct()
                        .forEach(dep -> emitter.onNext(new Dependency(packageName, className, dep)));

                cu.findAll(ObjectCreationExpr.class).stream()
                        .map(ObjectCreationExpr::getTypeAsString)
                        .distinct()
                        .forEach(dep -> emitter.onNext(new Dependency(packageName, className, dep)));

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }
}
