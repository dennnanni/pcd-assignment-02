package reactive;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.file.Path;

public class DependencyAnalyzerReactive {

    public Flowable<Dependency> parseClassDependenciesIncrementally(Path javaFile) {
        return Flowable.create(emitter -> {
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                String packageName = cu.getPackageDeclaration()
                        .map(NodeWithName::getNameAsString)
                        .orElse(javaFile.getParent().getFileName().toString());

                String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::getNameAsString).orElse("Anonymous");

                TypeCollectorVisitor visitor = new TypeCollectorVisitor(packageName, className, emitter);
                visitor.visit(cu, null);

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }
}
