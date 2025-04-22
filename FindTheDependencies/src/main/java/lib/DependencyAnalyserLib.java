package lib;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lib.reports.ClassDepReport;
import lib.reports.PackageDepsReport;
import lib.reports.ProjectDepsReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.Future;

public class DependencyAnalyserLib {

    private static final String ENTRY_POINT_FOLDER_NAME = "java";
    public static final String DEFAULT_PACKAGE = "java";
    public static final String SRC = "src";
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
                String className = classSrcFile.getFileName().toString().replace(".java", "");
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
        Promise<PackageDepsReport> promise = Promise.promise();

        Future<Set<ClassDepReport>> classReports = getFilesOrDirectoriesPaths(packageSrcFolder, true)
            .compose(javaFiles -> {
                List<Future<ClassDepReport>> dependencyFutures = new ArrayList<>();
                for (Path file : javaFiles) {
                    dependencyFutures.add(getClassDependencies(file));
                }
                return Future.all(dependencyFutures);
            }).map(cf -> {
                Set<ClassDepReport> classesReport = new HashSet<>();
                for (int i = 0; i < cf.size(); i++) {
                    ClassDepReport report = cf.resultAt(i);
                    classesReport.add(report);
                }
                return classesReport;
            });

        Future<Set<PackageDepsReport>> packageReports = getFilesOrDirectoriesPaths(packageSrcFolder, false)
                .compose(directories -> {
                    List<Future<PackageDepsReport>> dependencyFutures = new ArrayList<>();
                    for (Path dir : directories) {
                        dependencyFutures.add(getPackageDependencies(dir));
                    }
                    return Future.all(dependencyFutures);
                }).map(cf -> {
                    Set<PackageDepsReport> packagesReport = new HashSet<>();
                    for (int i = 0; i < cf.size(); i++) {
                        PackageDepsReport report = cf.resultAt(i);
                        packagesReport.add(report);
                    }
                    return packagesReport;
                });

        Future.all(classReports, packageReports).onSuccess(cf -> {
            Set<ClassDepReport> classes = cf.resultAt(0);
            Set<PackageDepsReport> packages = cf.resultAt(1);
            String packageName = packageSrcFolder.getFileName().toString();
            PackageDepsReport packageReport = new PackageDepsReport(packageName, classes, packages);
            promise.complete(packageReport);
        }).onFailure(promise::fail);

        return promise.future();
    }

    public Future<Set<Path>> getFilesOrDirectoriesPaths(Path projectSrcFolder, boolean onlyFiles) {
        Promise<Set<Path>> promise = Promise.promise();
        vertx.executeBlocking(() -> {
            try (Stream<Path> paths = Files.list(projectSrcFolder)) {
                var filesOrDirectoriesPaths = paths
                        .filter(onlyFiles ? Files::isRegularFile : Files::isDirectory);
                if (onlyFiles) {
                    filesOrDirectoriesPaths = filesOrDirectoriesPaths.filter(path -> path.toString().endsWith(".java"));
                }
                return filesOrDirectoriesPaths.collect(Collectors.toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).onSuccess(promise::complete)
        .onFailure(promise::fail);

        return promise.future();
    }

    public Future<ProjectDepsReport> getProjectDependencies(Path projectFolder) {
        Promise<ProjectDepsReport> promise = Promise.promise();

        Future<Path> entrypoint = findSrcFolder(projectFolder).compose(this::findEntryPointFolder);

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
            Set<PackageDepsReport> packages = new HashSet<>();
            Set<ClassDepReport> classes = cf.resultAt(1);
            packages.add(new PackageDepsReport(entrypoint.result().getFileName().toString(), classes, cf.resultAt(0)));

            ProjectDepsReport projectReport = new ProjectDepsReport(projectFolder.getFileName().toString(), packages);

            promise.complete(projectReport);
        }).onFailure(promise::fail);

        return promise.future();
    }

    private Future<Path> findSrcFolder(Path startingPath) {
        Promise<Path> promise = Promise.promise();

        vertx.executeBlocking(() -> {
            Path srcFolder;
            try (Stream<Path> firstLevelDirs = Files.list(startingPath)) {
                srcFolder = firstLevelDirs
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().equals(SRC))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(SRC + " folder not found in " + startingPath));
                return srcFolder;
            } catch (IOException e) {
                throw new RuntimeException("Error accessing file system: " + e.getMessage(), e);
            }
        }).onSuccess(promise::complete)
        .onFailure(promise::fail);

        return promise.future();
    }

    private Future<Path> findEntryPointFolder(Path startingPath) {
        Promise<Path> promise = Promise.promise();

        vertx.executeBlocking(() -> {
                    Path entrypoint;
                    try (Stream<Path> paths = Files.walk(startingPath)) {
                        entrypoint = paths
                                .filter(Files::isDirectory)
                                .filter(path -> path.getFileName().toString().equals(ENTRY_POINT_FOLDER_NAME))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(ENTRY_POINT_FOLDER_NAME + " folder not found in " + startingPath));
                        return entrypoint;
                    } catch (IOException e) {
                        throw new RuntimeException("Error accessing file system: " + e.getMessage(), e);
                    }
                }).onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

}
