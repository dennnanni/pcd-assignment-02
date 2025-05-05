package pcd.ass01.tasks;


public class UpdatePositionTask implements Runnable {

    private final Latch latch;
    private final Boid boid;
    private final BoidsModel model;

    public UpdatePositionTask(Boid boid, BoidsModel model, Latch latch) {
        this.boid = boid;
        this.model = model;
        this.latch = latch;
    }

    @Override
    public void run() {
        boid.updatePos(model);
        latch.countDown();
    }
}
