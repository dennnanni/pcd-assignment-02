package pcd.ass01.threads;

public class SyncWorkersMonitor {
    private final int totalWorkers;
    private int finishedCount = 0;

    public SyncWorkersMonitor(int totalWorkers) {
        this.totalWorkers = totalWorkers;
    }

    public synchronized void workDoneWaitCoordinator() throws InterruptedException {

        finishedCount++;
        if (finishedCount == totalWorkers) {
            notifyAll();
        }

        while (finishedCount != 0) {
            wait();
        }
    }

    public synchronized void waitWorkers() {
        try {
            while (finishedCount < totalWorkers) {
                wait();
            }
        } catch (InterruptedException e) {
        }
    }

    public synchronized void coordinatorDone() {
        finishedCount = 0;
        notifyAll();
    }
}
