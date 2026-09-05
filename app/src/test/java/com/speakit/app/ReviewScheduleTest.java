package com.speakit.app;
import org.junit.Test;
public class ReviewScheduleTest {
 @Test public void intervalsAndLapses() {
  if(ReviewSchedule.interval(0,0)!=1 || ReviewSchedule.interval(0,1)!=10 || ReviewSchedule.interval(0,2)!=1440 || ReviewSchedule.interval(0,3)!=4320) throw new AssertionError("Initial intervals");
  if(ReviewSchedule.interval(1440,2)!=2880 || ReviewSchedule.interval(1440,0)!=1) throw new AssertionError("Growth and lapse");
  if(ReviewSchedule.interval(525600,3)!=525600) throw new AssertionError("Cap");
  try { ReviewSchedule.interval(0,4); throw new AssertionError("Invalid rating"); } catch(IllegalArgumentException expected) {}
  System.out.println("Scheduler checks passed: initial intervals, successful recall, lapse reset, cap, invalid rating.");
 }
}
