package lib;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import reactive.Dependency;

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
    public void visit(MethodDeclaration n, Set<String> collector) {
        super.visit(n, collector);

        n.getType().ifClassOrInterfaceType(t ->
                collector.add(t.getNameAsString())
        );

        n.getParameters().forEach(param -> {
            param.getType().ifClassOrInterfaceType(t ->
                    collector.add(t.getNameAsString())
            );
        });
    }

    @Override
    public void visit(FieldDeclaration n, Set<String> collector) {
        super.visit(n, collector);
        n.getVariables().forEach(v -> {
            n.getElementType().ifClassOrInterfaceType(t ->
                    collector.add(t.getNameAsString())
            );
        });
    }
}
