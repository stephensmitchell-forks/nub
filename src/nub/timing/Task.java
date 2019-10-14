/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.timing;

/**
 * Tasks are single-threaded recurrent callbacks defined by {@link #execute()}.
 * Tasks should be registered after instantiation calling {@link TimingHandler#registerTask(Task)}.
 * <p>
 * Call {@link #toggleRecurrence()} to toggle recurrence, i.e., the tasks
 * will only be executed once.
 * <p>
 * Call {@link TimingHandler#unregisterTask(Task)} to cancel the task.
 */
abstract public class Task implements Taskable {
  protected boolean _active;
  protected boolean _once;
  private long _counter;
  private long _period;
  private long _startTime;

  /**
   * Executes the callback method defined by the {@link #execute()}.
   *
   * <b>Note:</b> You should not call this method since it's done by the timing handler
   * (see {@link nub.timing.TimingHandler#handle()}).
   */
  protected boolean _execute(float frameRate) {
    boolean result = false;
    if (_active) {
      long elapsedTime = System.currentTimeMillis() - _startTime;
      float timePerFrame = (1 / frameRate) * 1000;
      long threshold = _counter * _period;
      if (threshold >= elapsedTime) {
        long diff = elapsedTime + (long) timePerFrame - threshold;
        if (diff >= 0)
          if ((threshold - elapsedTime) < diff)
            result = true;
      } else
        result = true;
      if (result)
        _counter++;
    }
    if (result) {
      execute();
      if (_once)
        _active = false;
    }
    return result;
  }

  @Override
  public void run(long period) {
    setPeriod(period);
    run();
  }

  @Override
  public void run() {
    if (_period <= 0)
      return;
    _active = true;
    _counter = 1;
    _startTime = System.currentTimeMillis();
  }

  @Override
  public void stop() {
    _active = false;
  }

  @Override
  public void toggle() {
    if (isActive())
      stop();
    else
      run();
  }

  @Override
  public boolean isActive() {
    return _active;
  }

  @Override
  public long period() {
    return _period;
  }

  @Override
  public void setPeriod(long period) {
    _period = period;
  }

  @Override
  public void toggleRecurrence() {
    _once = !_once;
  }

  @Override
  public boolean isRecurrent() {
    return !_once;
  }
}