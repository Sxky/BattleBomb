package battlebomb;

import com.jumppixel.clockwork.CWLogger;
import com.jumppixel.clockwork.ClockworkException;
import com.jumppixel.clockwork.Core;
import com.jumppixel.clockwork.Game.GameHost;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class BattleBomb
  extends JavaPlugin
{
  private GameHost host;
  public static BattleBomb instance;
  
  public void onEnable()
  {
    instance = this;
    for (Objective objective : Bukkit.getScoreboardManager().getMainScoreboard().getObjectives()) {
      objective.unregister();
    }
    for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
      team.unregister();
    }
    for (OfflinePlayer player : Bukkit.getScoreboardManager().getMainScoreboard().getPlayers()) {
      Bukkit.getScoreboardManager().getMainScoreboard().resetScores(player);
    }
    
    this.host = new GameHost();
    
          
    try
    {
      this.host.enableGame(BBGame.class);
    }
    catch (ClockworkException e)
    {
      Core.instance.getCWLogger().fatal("An error occured while starting the game!");
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
    }
    Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
  }
  
  public void onDisable()
  {
    this.host.finish();
  }
}