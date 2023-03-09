package attini.deploy.origin;

public class SystemClockFacade {

    public long getCurrentTime(){
       return System.currentTimeMillis();
    }
}
