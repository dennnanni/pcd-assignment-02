package lib;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;

public class TypeCollectorVisitor extends VoidVisitorAdapter<Set<String>> {

    @Override
    public void visit(ClassOrInterfaceType n, Set<String> collector) {
        super.visit(n, collector);
        collector.add(n.getNameAsString());  // capture used type
    }

    @Override
    public void visit(ObjectCreationExpr n, Set<String> collector) {
        super.visit(n, collector);
        n.getType().ifClassOrInterfaceType(t -> collector.add(t.getNameAsString())); // new Type()
    }

    @Override
    public void visit(MethodCallExpr n, Set<String> collector) {
        super.visit(n, collector);
        n.getScope().ifPresent(scope -> {
            if (scope.isNameExpr()) {
                collector.add(scope.asNameExpr().getNameAsString()); // static call? object call?
            }
        });
    }

    @Override
    public void visit(FieldAccessExpr n, Set<String> collector) {
        super.visit(n, collector);
        collector.add(n.getScope().toString());  // likely a class name in static access
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Set<String> collector) {
        super.visit(n, collector);
        collector.add(n.getNameAsString()); // annotation is a type
    }
}
