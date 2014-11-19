package battlebomb.gears;

import com.jumppixel.clockwork.Gear.Gear;
import com.jumppixel.clockwork.Gear.GearMeta;
import com.jumppixel.clockwork.MiscUtils.Messager;
import battlebomb.BBGame;
import org.bukkit.event.Listener;


@GearMeta(gearName="Game Time Gear", step=20)
public class GameTimeGear
  implements Gear, Listener
{
  int minutes_elapsed = -1;
 
  public void onRegister() {}
  public void onUnregister() {}
  
  public void startCounter()
  {
    this.minutes_elapsed = 0;
    BBGame.instance.seconds = Integer.valueOf(1200);
    Messager.sendMessage("Game over in 30 minutes!");
  }
  
  public void stopCounter()
  {
    this.minutes_elapsed = -1;
    BBGame.instance.seconds = Integer.valueOf(0); 
  }
  
  public void onEnable(){}
   
  public void onDisable() {}
  
  public void onStep()
  {
    if (this.minutes_elapsed == -1) {
      return;
    }
    BBGame localBBGame = BBGame.instance;Integer localInteger1 = localBBGame.seconds;Integer localInteger2 = localBBGame.seconds = Integer.valueOf(localBBGame.seconds.intValue() - 1);
    if (BBGame.instance.seconds.intValue() % 60 == 0)
    {
      this.minutes_elapsed += 1;
      if (this.minutes_elapsed == 1)
      {
        Messager.sendMessage("29 Minutes remain!");
      } 
      else
      if (this.minutes_elapsed == 10)
      {
        Messager.sendMessage("20 Minutes remain!");
      }
      else if (this.minutes_elapsed == 20)
      {
        Messager.sendMessage("10 Minutes remain!");
      }
      else if (this.minutes_elapsed == 30)
      {
        Messager.sendMessage("Time!");
        BBGame.instance.onEnd(true);
      }
    }
  }
  
  public void pauseStep(boolean b) {}
  
  public boolean getStepPaused()
  {
    return false;
  }
  
  
  

}
