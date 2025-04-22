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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        Path classSrcFile = Path.of(currentPath + "\\pcd-assignment-01\\src\\main\\java\\pcd\\ass01\\threads\\BoidsSimulator.java");

        // Call the getClassDependencies method and handle the result
        dependencyAnalyzer.getClassDependencies(classSrcFile).onComplete(ar -> {
            if (ar.succeeded()) {
                ClassDepReport report = ar.result();
                assertEquals("BoidsSimulator", report.getClassName());
                assertEquals("pcd.ass01.threads", report.getPackageName());
                assertTrue(report.getDependencies().containsAll(List.of("BoidsModel", "Barrier", "SimulationStateMonitor", "SyncWorkersMonitor")));
                assertFalse(report.getDependencies().contains("Latch"));
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testGetPackageDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path packageSrc = Path.of(currentPath + "\\pcd-assignment-01\\src\\main\\java\\pcd\\ass01\\tasks");

        dependencyAnalyzer.getPackageDependencies(packageSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                PackageDepsReport report = ar.result();
                assertEquals("tasks", report.getPackageName());
                var classNames = report.getClasses().stream()
                        .map(ClassDepReport::getClassName)
                        .toList();
                assertTrue(classNames.containsAll(List.of("Barrier", "Boid", "BoidsModel", "SimulationStateMonitor", "BoidsView", "UpdatePositionTask", "UpdateVelocityTask")));
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testGetProjectDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path projectSrc = Path.of(currentPath + "\\pcd-assignment-01");

        dependencyAnalyzer.getProjectDependencies(projectSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                ProjectDepsReport report = ar.result();
                assertEquals("pcd-assignment-01", report.getProjectName());
                var packageNames = report.getPackages().stream()
                        .map(PackageDepsReport::getPackageName)
                        .toList();
                assertTrue(packageNames.contains("java"));
                assertEquals(1, packageNames.size());
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testPrintClassDependencies(Vertx vertx, VertxTestContext testContext) {
        // Create an instance of the DependencyAnalizerLib class
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        // Define the path to the source file
        Path classSrcFile = Path.of(currentPath + "\\pcd-assignment-01\\src\\main\\java\\pcd\\ass01\\threads\\BoidsSimulator.java");

        // Call the getClassDependencies method and handle the result
        dependencyAnalyzer.getClassDependencies(classSrcFile).onComplete(ar -> {
            if (ar.succeeded()) {
                ClassDepReport report = ar.result();
                System.out.println("Class Name: " + report.getClassName());
                System.out.println("Package Name: " + report.getPackageName());
                System.out.println("Dependencies: " + report.getDependencies());
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    @Test
    public void testPrintPackageDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path packageSrc = Path.of(currentPath + "\\pcd-assignment-01\\src\\main\\java");

        dependencyAnalyzer.getPackageDependencies(packageSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                PackageDepsReport report = ar.result();
                recursivePrint(report);

                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }

    private void recursivePrint(PackageDepsReport packageReport) {
        System.out.println("Package Name: " + packageReport.getPackageName());
        System.out.println("Package Dependencies: " + packageReport.getClasses());
        System.out.println("------------");
        for (ClassDepReport classReport : packageReport.getClasses()) {
            System.out.println("Class Name: " + classReport.getClassName());
            System.out.println("Package Name: " + classReport.getPackageName());
            System.out.println("Dependencies: " + classReport.getDependencies());
        }
        System.out.println("------------");
        for (PackageDepsReport subPackage : packageReport.getPackages()) {
            recursivePrint(subPackage);
        }
    }

    @Test
    public void testPrintProjectDependencies(Vertx vertx, VertxTestContext testContext) {
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        Path projectSrc = Path.of(currentPath + "\\pcd-assignment-01");

        dependencyAnalyzer.getProjectDependencies(projectSrc).onComplete(ar -> {
            if (ar.succeeded()) {
                ProjectDepsReport report = ar.result();
                System.out.println("Project Name: " + report.getProjectName());
                System.out.println("Project Dependencies: " + report.getPackages());
                for (PackageDepsReport packageReport : report.getPackages()) {
                    recursivePrint(packageReport);
                }

                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });
    }
}
