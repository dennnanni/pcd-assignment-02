package pcd.ass01.tasks;


public class UpdateVelocityTask implements Runnable {

    private final Boid boid;
    private final Latch latch;
    private final BoidsModel model;

    public UpdateVelocityTask(Boid boid,
                              BoidsModel model,
                              Latch latch) {
        this.boid = boid;
        this.model = model;
        this.latch = latch;
    }


    @Override
    public void run() {
        boid.updateVelocity(model);
        latch.countDown();
    }
}
