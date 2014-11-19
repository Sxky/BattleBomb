package battlebomb.gears;

import com.jumppixel.clockwork.CWLogger;
import com.jumppixel.clockwork.Core;
import com.jumppixel.clockwork.Database.Mongo.MongoBattleTracker;
import com.jumppixel.clockwork.Events.PlayerDeathEvent;
import com.jumppixel.clockwork.Game.GameHost;
import com.jumppixel.clockwork.Game.GameState;
import com.jumppixel.clockwork.Gear.Gear;
import com.jumppixel.clockwork.Gear.GearMeta;
import com.jumppixel.clockwork.TrackerLockedException;
import battlebomb.BBGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@GearMeta(gearName="Track kill gear", step=20)
public class TrackKillGear
  implements Gear, Listener
{
  public void onRegister()
  {
    Core.instance.getCWLogger().tell("Trackkillgear checking in!");
  }
  
  public void onUnregister() {}
  
  public void onEnable() {}
  
  public void onDisable() {}
  
  public void onStep() {}
  
  public void pauseStep(boolean b) {}
  
  public boolean getStepPaused()
  {
    return false;
  }
  
  @EventHandler
  public void PlayerDeathEvent(PlayerDeathEvent event)
  {
    if (BBGame.instance.game_host.getGameState() != GameState.IN_SESSION) {
      return;
    }
    
    try
    {
      BBGame.instance.tracker.logDeath(event.getInfo(), event.getContributors());
    }
    catch (TrackerLockedException e)
    {
      e.printStackTrace();
    }
  }
}
