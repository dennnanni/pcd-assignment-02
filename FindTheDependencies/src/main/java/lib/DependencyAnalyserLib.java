package lib;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.reports.ClassDepReport;
import lib.reports.PackageDepsReport;
import lib.reports.ProjectDepsReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.Future;

public class DependencyAnalyserLib {

    private static final String ENTRY_POINT_FOLDER_NAME = "java";
    public static final String DEFAULT_PACKAGE = "java";
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
                        .orElse(DEFAULT_PACKAGE);
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
                    .orElse(DEFAULT_PACKAGE);

            packageAndDependencies.put(packageName, usedTypes);
            return packageAndDependencies;
        }).onSuccess(promise::complete)
        .onFailure(promise::fail);

        return promise.future();
    }

    private Future<CompilationUnit> parseCompilationUnitAsync(String sourceCode) {
        return vertx.executeBlocking(() -> StaticJavaParser.parse(sourceCode), false);
    }

    public Future<PackageDepsReport> getPackageDependencies(Path packageSrcFolder) {
        // Recursively get all class files in the package directory
        // For each class file, call getClassDependencies
        // Combine the results into a PackageDepsReport
        Promise<PackageDepsReport> promise = Promise.promise();

        getFilesPaths(packageSrcFolder)
            .compose(javaFiles -> {
                List<Future<ClassDepReport>> dependencyFutures = new ArrayList<>();

                for (Path file : javaFiles) {
                    dependencyFutures.add(getClassDependencies(file));
                }

                return Future.all(dependencyFutures);
            }).onSuccess(cf -> {
                Set<ClassDepReport> dependencies = new HashSet<>();
                String packageName = packageSrcFolder.getFileName().toString();

                for (int i = 0; i < cf.size(); i++) {
                    ClassDepReport report = cf.resultAt(i);
                    dependencies.add(report);
                }

                PackageDepsReport report = new PackageDepsReport(packageName, dependencies);
                promise.complete(report);
            }).onFailure(promise::fail);

        return promise.future();
    }

    public Future<Set<Path>> getFilesPaths(Path projectSrcFolder) {
        Promise<Set<Path>> promise = Promise.promise();
        vertx.executeBlocking(() -> {
            try (Stream<Path> paths = Files.walk(projectSrcFolder)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).onSuccess(promise::complete)
        .onFailure(promise::fail);

        return promise.future();
    }

    public Future<ProjectDepsReport> getProjectDependencies(Path projectFolder) {
        Promise<ProjectDepsReport> promise = Promise.promise();

        Future<Path> entrypoint = findEntryPointFolder(projectFolder);

        Future<Set<PackageDepsReport>> packageReports = entrypoint.compose(ep -> {
            List<Future<PackageDepsReport>> packagePromise = new ArrayList<>();
            try (Stream<Path> paths = Files.list(ep)) {
                paths.filter(Files::isDirectory)
                        .filter(path -> !path.equals(ep))
                        .forEach(path -> packagePromise.add(getPackageDependencies(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Future.all(packagePromise).map(cf -> {
                Set<PackageDepsReport> packages = new HashSet<>();
                for (int i = 0; i < cf.size(); i++) {
                    PackageDepsReport report = cf.resultAt(i);
                    packages.add(report);
                }
                return packages;
            });
        });

        Future<Set<ClassDepReport>> classReports = entrypoint.compose(ep -> {
            List<Future<ClassDepReport>> classPromise = new ArrayList<>();
            try (Stream<Path> paths = Files.list(ep)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> classPromise.add(getClassDependencies(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Future.all(classPromise).map(cf -> {
                Set<ClassDepReport> classes = new HashSet<>();
                for (int i = 0; i < cf.size(); i++) {
                    ClassDepReport report = cf.resultAt(i);
                    classes.add(report);
                }
                return classes;
            });
        });

        Future.all(packageReports, classReports).onSuccess(cf -> {
            Set<PackageDepsReport> packages = cf.resultAt(0);
            Set<ClassDepReport> classes = cf.resultAt(1);
            packages.add(new PackageDepsReport(entrypoint.result().getFileName().toString(), classes));

            ProjectDepsReport projectReport = new ProjectDepsReport(projectFolder.getFileName().toString(), packages);
            promise.complete(projectReport);
        }).onFailure(promise::fail);

        return promise.future();
    }

    private Future<Path> findEntryPointFolder(Path startingPath) {
        Promise<Path> promise = Promise.promise();

        vertx.executeBlocking(() -> {
                try (Stream<Path> paths = Files.walk(startingPath)) {
                    Optional<Path> srcFolder = paths
                            .filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().equals(ENTRY_POINT_FOLDER_NAME))
                            .findFirst();

                    if (srcFolder.isPresent()) {
                        return srcFolder.get();
                    } else {
                        throw new RuntimeException(ENTRY_POINT_FOLDER_NAME + " folder not found.");
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Error walking file tree: " + e.getMessage(), e);
                }
            }).onSuccess(promise::complete)
            .onFailure(promise::fail);

        return promise.future();
    }

}
