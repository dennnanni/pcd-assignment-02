package lib;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lib.reports.ClassDepReport;
import lib.reports.PackageDepsReport;
import lib.reports.ProjectDepsReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestAsynchronousLib {

    String currentPath = new java.io.File(".").getCanonicalPath();

    public TestAsynchronousLib() throws IOException {
    }

    @Test
    public void testGetClassDependencies(Vertx vertx, VertxTestContext testContext) {
        // Create an instance of the DependencyAnalizerLib class
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        // Define the path to the source file
        Path classSrcFile = Path.of(currentPath + "\\src\\main\\java\\lib\\DependencyAnalyserLib.java");

        // Call the getClassDependencies method and handle the result
        dependencyAnalyzer.getClassDependencies(classSrcFile).onComplete(ar -> {
            if (ar.succeeded()) {
                ClassDepReport report = ar.result();
                System.out.println("Class Name: " + report.getClassName());
                System.out.println("Package Name: " + report.getPackageName());
                System.out.println("Dependencies: " + report.getDependencies());
                assertEquals("DependencyAnalyserLib.java", report.getClassName());
                assertEquals("lib", report.getPackageName());
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testGetPackageDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path packageSrc = Path.of(currentPath + "\\src");

        dependencyAnalyzer.getPackageDependencies(packageSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                PackageDepsReport report = ar.result();
                System.out.println("Package Name: " + report.getPackageName());
                System.out.println("Package Dependencies: " + report.getDependencies());
                for (ClassDepReport classReport : report.getDependencies()) {
                    System.out.println("-----------");
                    System.out.println("Class Name: " + classReport.getClassName());
                    System.out.println("Package Name: " + classReport.getPackageName());
                    System.out.println("Dependencies: " + classReport.getDependencies());
                }

                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testGetProjectDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path projectSrc = Path.of(currentPath);

        dependencyAnalyzer.getProjectDependencies(projectSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                ProjectDepsReport report = ar.result();
                System.out.println("Project Name: " + report.getProjectName());
                System.out.println("Project Dependencies: " + report.getPackages());
                for (PackageDepsReport packageReport : report.getPackages()) {
                    System.out.println("-----------");
                    System.out.println("Package Name: " + packageReport.getPackageName());
                    for (ClassDepReport classReport : packageReport.getDependencies()) {
                        System.out.println("Class Name: " + classReport.getClassName());
                        System.out.println("Package Name: " + classReport.getPackageName());
                        System.out.println("Dependencies: " + classReport.getDependencies());
                    }
                }

                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }
}
