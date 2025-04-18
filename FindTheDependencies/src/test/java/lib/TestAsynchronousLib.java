package lib;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lib.reports.ClassDepReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestAsynchronousLib {

    @Test
    public void testGetClassDependencies(Vertx vertx, VertxTestContext testContext) {
        // Create an instance of the DependencyAnalizerLib class
        DependencyAnalyserLib dependencyAnalyzer = new DependencyAnalyserLib(vertx);

        // Define the path to the source file
        Path classSrcFile = Path.of("C:\\Users\\denno\\Desktop\\PCD\\pcd-assignment-02\\FindTheDependencies\\src\\main\\java\\lib\\DependencyAnalyserLib.java");

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
}
