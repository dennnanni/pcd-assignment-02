package lib;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;

public class TypeCollectorVisitor extends VoidVisitorAdapter<Set<String>> {

    @Override
    public void visit(ClassOrInterfaceType n, Set<String> collector) {
        super.visit(n, collector);
        System.out.println("ClassOrInterfaceType: " + n.getNameAsString());
        collector.add(n.getNameAsString());  // capture used type
    }

    @Override
    public void visit(ObjectCreationExpr n, Set<String> collector) {
        super.visit(n, collector);
        n.getType().ifClassOrInterfaceType(t -> {
            System.out.println("Class or interface: " + t.getNameAsString()); collector.add(t.getNameAsString());}); // new Type()
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Set<String> collector) {
        super.visit(n, collector);
        System.out.println("MarkerAnnotationExpr: " + n.getNameAsString());
        collector.add(n.getNameAsString()); // annotation is a type
    }
}
