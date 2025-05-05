package pcd.ass01.tasks;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BoidsSimulator {

    private static final int NTHREADS = Runtime.getRuntime().availableProcessors() + 1 ;

    private final SimulationStateMonitor monitor;
    private BoidsModel model;
    private Optional<BoidsView> view;
    private Executor executor;
    private Latch taskCounter;
    
    private static final int FRAMERATE = 25;
    private int framerate;
    
    public BoidsSimulator(BoidsModel model, SimulationStateMonitor monitor) {
        this.model = model;
        view = Optional.empty();
        this.monitor = monitor;
        this.executor = Executors.newFixedThreadPool(NTHREADS);
    }

    public void attachView(BoidsView view) {
    	this.view = Optional.of(view);
    }
      
    public void runSimulation() {
    	while (true) {
            try {
                this.monitor.waitIfPausedOrStopped();
            } catch (InterruptedException ex) {}

            var t0 = System.currentTimeMillis();
    		var boids = model.getBoids();
            taskCounter = new Latch(boids.size());

            for (Boid b : boids) {
                this.executor.execute(new UpdateVelocityTask(b, model, taskCounter));
            }

            try {
                taskCounter.await();
            } catch (InterruptedException ex) {
            } finally {
                taskCounter = new Latch(boids.size());
            }

            for (Boid b : boids) {
                this.executor.execute(new UpdatePositionTask(b, model, taskCounter));
            }

            try {
                taskCounter.await();
            } catch (InterruptedException ex) {
            }

            model.makeCopy();

    		if (view.isPresent()) {
            	view.get().update(framerate);
            	var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var framratePeriod = 1000/FRAMERATE;
                
                if (dtElapsed < framratePeriod) {		
                	try {
                		Thread.sleep(framratePeriod - dtElapsed);
                	} catch (Exception ex) {}
                	framerate = FRAMERATE;
                } else {
                	framerate = (int) (1000/dtElapsed);
                }
    		}
            
    	}
    }
}
