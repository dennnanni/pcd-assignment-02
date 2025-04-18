package lib;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.reports.ClassDepReport;
import lib.reports.PackageDepsReport;
import lib.reports.ProjectDepsReport;

import java.nio.file.Path;
import java.util.*;

import io.vertx.core.Future;

public class DependencyAnalyserLib {

    private final Vertx vertx;

    public DependencyAnalyserLib(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<ClassDepReport> getClassDependencies(Path classSrcFile) {
        Promise<ClassDepReport> reportPromise = Promise.promise();

        readSourceFile(classSrcFile)
            .compose(this::parseCompilationUnitAsync)
            .compose(this::visitAST)
            .onSuccess(packageAndDeps -> {
                String packageName = packageAndDeps.keySet().stream().findFirst()
                        .orElse("default");
                String className = classSrcFile.getFileName().toString();
                ClassDepReport report = new ClassDepReport(className, packageName, packageAndDeps.get(packageName));
                reportPromise.complete(report);
            })
            .onFailure(reportPromise::fail);

        return reportPromise.future();
    }

    private Future<String> readSourceFile(Path path) {
        Promise<String> promise = Promise.promise();
        vertx.fileSystem().readFile(path.toString(), ar -> {
            if (ar.succeeded()) {
                String source = ar.result().toString("UTF-8");
                promise.complete(source);
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    private Future<Map<String,Set<String>>> visitAST(CompilationUnit cu) {
        Promise<Map<String,Set<String>>> promise = Promise.promise();
        vertx.executeBlocking(() -> {
            Map<String, Set<String>> packageAndDependencies = new HashMap<>();
            Set<String> usedTypes = new HashSet<>();

            new TypeCollectorVisitor().visit(cu, usedTypes);

            String packageName = cu.getPackageDeclaration()
                    .map(NodeWithName::getNameAsString)
                    .orElse("default");

            packageAndDependencies.put(packageName, usedTypes);
            return packageAndDependencies;
        }).onSuccess(promise::complete)
        .onFailure(promise::fail);

        return promise.future();
    }

    private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
        return vertx.executeBlocking(() -> StaticJavaParser.parse(sourceCode), false);
    }

    public PackageDepsReport getPackageDependencies(String packageSrcFolder) {
        // Recursively get all class files in the package directory
        // For each class file, call getClassDependencies
        // Combine the results into a PackageDepsReport

        throw new UnsupportedOperationException();
    }

    public ProjectDepsReport getProjectDependencies(String projectSrcFolder) {
        throw new UnsupportedOperationException();
    }

}
