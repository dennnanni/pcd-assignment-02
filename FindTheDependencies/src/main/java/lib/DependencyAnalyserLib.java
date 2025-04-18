package lib;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.reports.ClassDepReport;
import lib.reports.PackageDepsReport;
import lib.reports.ProjectDepsReport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Future;

public class DependencyAnalyserLib {

    private final Vertx vertx;

    public DependencyAnalyserLib(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<ClassDepReport> getClassDependencies(Path classSrcFile) {
        Promise<ClassDepReport> reportPromise = Promise.promise();

        readSourceFile(classSrcFile).compose(this::parseCompilationUnitAsync
        ).onSuccess(cu -> {
            Set<String> usedTypes = new HashSet<>();
            new TypeCollectorVisitor().visit(cu, usedTypes);

            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.map(PackageDeclaration::getNameAsString).orElse("default");
            String className = classSrcFile.getFileName().toString();
            ClassDepReport report = new ClassDepReport(className, packageName, usedTypes);
            reportPromise.complete(report);
        }).onFailure(reportPromise::fail);

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

    private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
        return vertx.executeBlocking(() -> StaticJavaParser.parse(sourceCode), false);
    }

    public PackageDepsReport getPackageDependencies(String packageSrcFolder) {
        throw new UnsupportedOperationException();
    }

    public ProjectDepsReport getProjectDependencies(String projectSrcFolder) {
        throw new UnsupportedOperationException();
    }

}
