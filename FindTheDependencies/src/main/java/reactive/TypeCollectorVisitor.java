package reactive;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.reactivex.rxjava3.core.FlowableEmitter;

import java.util.Set;

public class TypeCollectorVisitor extends VoidVisitorAdapter<Set<String>> {

    private final String packageName;
    private final String className;
    private final FlowableEmitter<Dependency> emitter;

    public TypeCollectorVisitor(String packageName, String className, FlowableEmitter<Dependency> emitter) {
        this.packageName = packageName;
        this.className = className;
        this.emitter = emitter;
    }

    @Override
    public void visit(ClassOrInterfaceType n, Set<String> collector) {
        super.visit(n, collector);
        emitter.onNext(new Dependency(packageName, className, n.getNameAsString()));
    }

    @Override
    public void visit(ObjectCreationExpr n, Set<String> collector) {
        super.visit(n, collector);
        n.getType().ifClassOrInterfaceType(t -> emitter.onNext(new Dependency(packageName, className, t.getNameAsString())));
    }

    @Override
    public void visit(MethodDeclaration n, Set<String> arg) {
        super.visit(n, arg);

        n.getType().ifClassOrInterfaceType(t ->
                emitter.onNext(new Dependency(packageName, className, t.getNameAsString()))
        );

        n.getParameters().forEach(param -> {
            param.getType().ifClassOrInterfaceType(t ->
                    emitter.onNext(new Dependency(packageName, className, t.getNameAsString()))
            );
        });
    }

    @Override
    public void visit(FieldDeclaration n, Set<String> arg) {
        super.visit(n, arg);
        n.getVariables().forEach(v -> {
            n.getElementType().ifClassOrInterfaceType(t ->
                    emitter.onNext(new Dependency(packageName, className, t.getNameAsString()))
            );
        });
    }
}
