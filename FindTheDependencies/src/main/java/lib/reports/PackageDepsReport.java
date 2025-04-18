package lib.reports;

import java.util.Set;

public class PackageDepsReport {

    public String packageName;
    public Set<ClassDepReport> dependencies;

    public PackageDepsReport(String packageName, Set<ClassDepReport> dependencies) {
        this.packageName = packageName;
        this.dependencies = dependencies;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<ClassDepReport> getDependencies() {
        return dependencies;
    }

}
