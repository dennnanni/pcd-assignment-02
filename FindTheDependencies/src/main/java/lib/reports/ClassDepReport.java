package lib.reports;

import java.util.Set;

public class ClassDepReport {

    private final String className;
    private final String packageName;
    private final Set<String> dependencies;

    public ClassDepReport(String className, String packageName, Set<String> dependencies) {
        this.className = className;
        this.packageName = packageName;
        this.dependencies = dependencies;
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
}
