package lib.reports;

import java.util.HashSet;
import java.util.Set;

public class ClassDepsReport {

    private final String className;
    private final String packageName;
    private final Set<String> dependencies;

    public ClassDepsReport(String className, String packageName, Set<String> dependencies) {
        this.className = className;
        this.packageName = packageName;
        this.dependencies = new HashSet<>(dependencies);
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }
}
