package reactive;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.nio.file.Path;

import java.util.Set;
import java.util.stream.Collectors;

public class DependencyAnalizerReactive {

//    public record ClassDependency(String className, String packageName, Set<String> usedTypes) {}

    public Observable<Dependency> parseClassDependenciesIncrementally(Path javaFile) {
        return Observable.create(emitter -> {
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                String packageName = cu.getPackageDeclaration()
                        .map(NodeWithName::getNameAsString)
                        .orElse("java");
                String className = cu.findFirst(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::getNameAsString).orElse("Anonymous");
                        
                Set<String> dep = cu.findAll(ClassOrInterfaceType.class).stream()
                        .map(ClassOrInterfaceType::getNameAsString)
                        .collect(Collectors.toSet());
                dep.addAll(cu.findAll(ObjectCreationExpr.class).stream()
                        .map(ObjectCreationExpr::getTypeAsString)
                        .collect(Collectors.toSet()));

                dep.forEach(d -> {
                    Dependency depObject = new Dependency(packageName, className, d);
                    emitter.onNext(depObject);
                });

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
