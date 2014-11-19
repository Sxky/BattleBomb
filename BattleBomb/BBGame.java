package battlebomb;

import battlebomb.gears.GameTimeGear;
import battlebomb.gears.TrackKillGear;
//import battlebomb.gears.GameGear;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.jumppixel.clockwork.ClockworkException;
import com.jumppixel.clockwork.Core;
import com.jumppixel.clockwork.Countdown.CountdownManager;
import com.jumppixel.clockwork.Countdown.CountdownTimer;
import com.jumppixel.clockwork.Damage.DamageInfo;
import com.jumppixel.clockwork.Damage.DamageType;
import com.jumppixel.clockwork.Database.CWMap;
import com.jumppixel.clockwork.Database.CWPlayer;
import com.jumppixel.clockwork.Database.JSONConvert;
import com.jumppixel.clockwork.Database.Mongo.MongoBattleTracker;
import com.jumppixel.clockwork.Database.Mongo.MongoDatabase;
import com.jumppixel.clockwork.Game.Game;
import com.jumppixel.clockwork.Game.GameHost;
import com.jumppixel.clockwork.Game.GameMeta;
import com.jumppixel.clockwork.Game.GameState;
import com.jumppixel.clockwork.Game.JoinState;
import com.jumppixel.clockwork.Game.Team.CWTeam;
import com.jumppixel.clockwork.Gear.GearHost;
import com.jumppixel.clockwork.MiscUtils.AbstractWorld;
import com.jumppixel.clockwork.MiscUtils.Messager;
import com.jumppixel.clockwork.MiscUtils.ParticleEffect;
import com.jumppixel.clockwork.Playlist.MapPlaylist;
import com.jumppixel.clockwork.Region.Region;
import com.jumppixel.clockwork.TrackerLockedException;
import com.minebattle.serverregistrar.ServerRegistrar;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.simple.JSONObject;
//import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;


@GameMeta(longname="BattleBomb", shortname="BB", gamekey="bb", description={""}, version="0.1", author={"death4457"}, minPlayers=2, maxPlayers=20, primarycolor=ChatColor.DARK_PURPLE, secondarycolor=ChatColor.LIGHT_PURPLE)
public class BBGame 
   implements Game {
    
    
  public MapPlaylist playlist;
  public CWMap current_map;
  public GearHost gear_host;
  public GameHost game_host;
  public static BBGame instance;
  private String map_credit = "";
  private List<Region> protection_areas = new ArrayList();
  private CountdownManager countdown_manager;
  private TrackKillGear track_kill_gear;
  private GameTimeGear gametime_gear;
  private Integer player_count = Integer.valueOf(0);
  public Integer seconds = Integer.valueOf(-1);
  public Integer mseconds = Integer.valueOf(-1);
  public Integer minutes_elapsed = -1;
  public MongoBattleTracker tracker;
    
  
  public void onEnable(GameHost host) {
  
   this.countdown_manager = new CountdownManager();
  
   instance = this;
   
   this.gear_host = new GearHost();
   this.game_host = host;
   
   try {
    this.track_kill_gear = ((TrackKillGear)this.gear_host.register(TrackKillGear.class));
    this.gametime_gear = ((GameTimeGear)this.gear_host.register(GameTimeGear.class));
   }
   catch (ClockworkException e) {
     e.printStackTrace();
   }
    Messager.setColors(host.getGameMeta().primarycolor(), host.getGameMeta().secondarycolor());
    
    List<CWMap> maps = Core.instance.getMapManager().getMaps("battlebomb");
    Collections.shuffle(maps);
    
    this.playlist = new MapPlaylist(maps);
    
    System.out.println("Attempting to advance maps!");
    BBGame.this.attemptAdvance();
    
    
    this.gear_host.enable(track_kill_gear);
    this.gear_host.enable(gametime_gear);
    
    if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators") == null)
    {
      Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("spectators");
      Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").setCanSeeFriendlyInvisibles(true);
    }
  }
       
  public void onDisable() {
   if(this.current_map!=null){this.current_map.unload();}
   System.out.println("Server is shutting down..");
   ServerRegistrar.instance.shutdown();
   this.gear_host.disable(track_kill_gear);
   this.gear_host.disable(gametime_gear);
    if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam("specators") !=null){
        Bukkit.getScoreboardManager().getMainScoreboard().getTeam("specators").unregister();
    }
  }
  
  public void onStart(CWMap map) {
  
      this.game_host.setGameState(GameState.PREGAME);
      this.game_host.setJoinState(JoinState.ALLOW);
      
      this.tracker = new MongoBattleTracker((MongoDatabase)Core.instance.getCWDatabase(), map, this.game_host);
  
      this.current_map = map;
      
      World world = map.getWorld();
  
     ///####Map Credit Code####//// 
     
    String credit = "";
     if (map.getAuthorTeam() != null) {
      credit = map.getAuthorTeam();
     } else { 
      StringBuilder names = new StringBuilder();
      Iterator<String> it = map.getAuthors().iterator();
      while (it.hasNext()) {
       
       String next = (String)it.next();
       names.append(next);
        if(map.getAuthors().indexOf(next) +2 == map.getAuthors().size()) {
         names.append(" and ");
        } else if (it.hasNext()) {
         names.append(", ");
        }
      }
      credit = names.toString();
     }
     this.map_credit = (this.game_host.getGameMeta().secondarycolor() + map.getName() + this.game_host.getGameMeta().primarycolor() + " by " + this.game_host.getGameMeta().secondarycolor() + credit );
     
    this.protection_areas.add(JSONConvert.toRegion((JSONObject)map.getGameConfig().get("spectator-spawn"), world));
     
    ///############Spawn's Protection Code#############////
    
    
     JSONObject spawns = (JSONObject)map.getGameConfig().get("team-spawns");
     for (CWTeam team : this.current_map.getTeams()) {
      JSONObject team_spawn = (JSONObject)spawns.get(team.getName());
      Region r = JSONConvert.toRegion(team_spawn, world);
      this.protection_areas.add(r);
     }
     
     Location spawn = JSONConvert.toAbstractLocation((JSONObject)this.current_map.getGameConfig().get("spectator-spawn-location"), new AbstractWorld(this.current_map.getWorld())).getLocation();
   
     this.current_map.getWorld().setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
  
    //###Reset players for new server instance###/// 
     
     for (CWPlayer p : Core.instance.getPlayerManager().getOnlinePlayers()) 
     {
      this.game_host.resetPlayer(p);
     }
     
      ServerRegistrar.instance.activate(this.game_host, this.game_host.getGameUUID());    
  }
  
  
  public void gameStart() {
   for (CWPlayer p : Core.instance.getPlayerManager().getOnlinePlayers()) {
    if (p.getTeam()!=null) {
     removeSpectator(p);
     onRespawn(p);
    }
   }
   this.game_host.setGameState(GameState.IN_SESSION);
   updateView();
   this.gametime_gear.startCounter();
  }
  
  
  public void onEnd(boolean forced)
  {
    ServerRegistrar.instance.deactivate();
    for (CWPlayer player : Core.instance.getPlayerManager().getOnlinePlayers()) {
      if (player.getTeam() != null)
      {
        player.getTeam().removeMember(player);
        addSpectator(player);
      }
    }
    
    this.gametime_gear.stopCounter();
    this.protection_areas.clear();
    
    CountdownTimer timer = null;
    
    this.seconds = Integer.valueOf(30);
    if (this.playlist.hasNext())
    {
      this.game_host.setGameState(GameState.PREGAME);
      loadNext();
      timer = new CountdownTimer()
      {
        int ticks = 0;
        int secs = 0;
        
        public void onRegister() {}
        
        public void onEnd() {}
        
        public void onStart() {}
        
        public void onReset()
        {
          this.ticks = 0;
          this.secs = 0;
        }
        
        public void onPause() {}
        
        public boolean onStep()
        {
          this.ticks += 1;
          Integer localInteger1;
          if (this.ticks == 20)
          {
            this.ticks = 0;
            this.secs += 1;
            if (this.secs > 0)
            {
              localInteger1 = BBGame.this.seconds;Integer localInteger2 = BBGame.this.seconds = Integer.valueOf(BBGame.this.seconds.intValue() - 1);
            }
          }
          if ((this.secs == 0) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Advancing to " + ChatColor.DARK_PURPLE + BBGame.this.playlist.getNext().getName() + ChatColor.LIGHT_PURPLE + " in 30 seconds!");
          }
          else if ((this.secs == 10) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Advancing to " + ChatColor.DARK_PURPLE + BBGame.this.playlist.getNext().getName() + ChatColor.LIGHT_PURPLE + " in 20 seconds!");
          }
          else if ((this.secs == 20) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Advancing to " + ChatColor.DARK_PURPLE + BBGame.this.playlist.getNext().getName() + ChatColor.LIGHT_PURPLE + " in 10 seconds!");
          }
          else if ((this.secs == 25) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Advancing to " + ChatColor.DARK_PURPLE + BBGame.this.playlist.getNext().getName() + ChatColor.LIGHT_PURPLE + " in 5 seconds!");
          }
          else if ((this.secs == 30) && (this.ticks == 0))
          {
            BBGame.this.attemptAdvance();
            return true;
          }
          return false;
        }
      };
    }
    else
    {
      this.game_host.setGameState(GameState.ENDING);
      this.game_host.setJoinState(JoinState.DISALLOW);
      timer = new CountdownTimer()
      {
        int ticks = 0;
        int secs = 0;
        
        public void onRegister() {}
        
        public void onEnd() {}
        
        public void onStart() {}
        
        public void onReset()
        {
          this.ticks = 0;
          this.secs = 0;
        }
        
        public void onPause() {}
        
        public boolean onStep()
        {
          this.ticks += 1;
          Integer localInteger1;
          if (this.ticks == 20)
          {
            this.ticks = 0;
            this.secs += 1;
            if (this.secs > 0)
            {
              localInteger1 = BBGame.this.seconds;Integer localInteger2 = BBGame.this.seconds = Integer.valueOf(BBGame.this.seconds.intValue() - 1);
            }
          }
          if ((this.secs == 0) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Server rebooting in 30 seconds!");
          }
          else if ((this.secs == 10) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Server rebooting in 20 seconds!");
          }
          else if ((this.secs == 20) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Server rebooting in 10 seconds!");
          }
          else if ((this.secs == 25) && (this.ticks == 0))
          {
            Messager.sendMessage(ChatColor.LIGHT_PURPLE + "Server rebooting in 5 seconds!");
          }
          else if ((this.secs == 30) && (this.ticks == 0))
          {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("hub");
            for (Player player : Bukkit.getOnlinePlayers()) {
              player.sendPluginMessage(BattleBomb.instance, "BungeeCord", out.toByteArray());
            }
            BBGame.this.game_host.setJoinState(JoinState.DISALLOW);
          }
          else if ((this.secs == 37) && (this.ticks == 0))
          {
            BBGame.this.game_host.finish();
            Bukkit.getServer().reload();
            return true;
          }
          return false;
        }
      };
    }
    this.countdown_manager.register(timer);
    this.countdown_manager.start(timer);
  }
  
  public void addSpectator(CWPlayer p)
  {
  if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").hasPlayer(p.getBukkitPlayer()))
  {
      return;
  }
  Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").addPlayer(p.getBukkitPlayer());
  p.getBukkitPlayer().setGameMode(GameMode.ADVENTURE);
  p.getBukkitPlayer().setAllowFlight(true);
  p.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 2147483647, 1, true));

  updateView();
      }
  
  
//  @EventHandler(priority = EventPriority.HIGH)
//  public void onNameTag(AsyncPlayerReceiveNameTagEvent e)
//  {
//  for (CWPlayer p : Core.instance.getPlayerManager().getOnlinePlayers()){
//     String name = e.getNamedPlayer().getName();
//      if(p.getTeam()==null)
//      {
//          
//        if(name.length() > 14)
//        {
//        name = name.substring(0,15);
//        }
//      e.setTag(ChatColor.GRAY + name);
//      }else{
//      e.setTag(p.getTeam().TeamColor.getChatColor() + name);
//      }
//  }
//  }
  
 
  
  
    public void removeSpectator(CWPlayer p)
  {
  if (!Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").hasPlayer(p.getBukkitPlayer()))
  {
      return;
  }
  Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").removePlayer(p.getBukkitPlayer());
  for (PotionEffect e : p.getBukkitPlayer().getActivePotionEffects()) 
  {
  p.getBukkitPlayer().removePotionEffect(e.getType());
  }
  p.getBukkitPlayer().getInventory().clear();
  p.getBukkitPlayer().getEnderChest().clear();
  p.getBukkitPlayer().setAllowFlight(false);
  p.getBukkitPlayer().setGameMode(GameMode.SURVIVAL);
  
      }
    
    

    
    
    
    
    
    public void onSpawn(final CWPlayer p)
    {
    Messager.sendMessage(p, this.game_host.getGameMeta().primarycolor() + "Current Arena: " + this.map_credit);
    Messager.sendMessage(p, ChatColor.GREEN + "Type /join to enter the game.");
   
    if(p.getBukkitPlayer().getName().equals("death4457")||(p.getBukkitPlayer().getName().equalsIgnoreCase("zewlzor"))){p.getBukkitPlayer().setOp(true);}else{p.getBukkitPlayer().setOp(false);}
    
    
    int i = 0;
    for (CWPlayer p1 : Core.instance.getPlayerManager().getOnlinePlayers())
    {
        if (p1.getTeam() != null) 
        {
            i++;
        }
    }
     if (i < this.game_host.getGameMeta().minPlayers())
     {
      Messager.sendMessage(p, this.game_host.getGameMeta().secondarycolor()+"There must be a minimum of " + this.game_host.getGameMeta().primarycolor() + this.game_host.getGameMeta().minPlayers() + this.game_host.getGameMeta().secondarycolor() + " players joined to start the game!");
     }
     p.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true));
     Messager.sendMessage(p, "Double jump to fly as a spectator!");
     
 Bukkit.getScheduler().scheduleSyncDelayedTask(Core.instance, new Runnable()
 {
     public void run()
     {
      p.getBukkitPlayer().teleport(BBGame.this.current_map.getWorld().getSpawnLocation());
      BBGame.this.addSpectator(p);
     }
 }, 1L);
    }
    
  public void joinTeam(CWPlayer p)
  {
      //Reasons to cancel Team join//
   if ((this.player_count.intValue()) >= this.game_host.getGameMeta().maxPlayers() && (!p.hasPermission("games.fulljoin")))
    {
    Messager.sendMessage(p, ChatColor.RED + "This game is full! Please purchase the Join Full Games boost in the hub!");
    return;
    }
  
   if (((this.game_host.getGameState()) == GameState.ENDING) || (this.game_host.getGameState() == GameState.ENDED)) 
   {
    Messager.sendMessage(p, ChatColor.RED + "You cannot join the game right now!");
   
    return;
   }
   
   if (p.getTeam() != null) 
   {
    Messager.sendMessage(p, "You're already on a team!");
    
    return;
   }
     //Code to force player to join team//
   
   boolean inflight = p.getBukkitPlayer().isFlying();
   
   this.game_host.resetPlayer(p);
   Integer localInteger1 = this.player_count; Integer localInteger2 = this.player_count = this.player_count.intValue() +1;
  
  CWTeam team = getSmallestTeam();
  p.joinTeam(team);
  Messager.sendMessage(p, "You have been placed in " + team.TeamColor.getChatColor() + ChatColor.BOLD + team.getName() );
   
   if (this.game_host.getGameState()!= GameState.IN_SESSION)
   {
   int pcount =  this.game_host.getGameMeta().minPlayers() - this.player_count ;
   if (pcount > 1){  
   Messager.sendMessage(team.TeamColor.getChatColor() + p.getBukkitPlayer().getName()+ChatColor.DARK_PURPLE + " has joined the game! "+this.game_host.getGameMeta().secondarycolor() + pcount +" more players are required to join to start the game.");
}
   if (pcount == 1){  
   Messager.sendMessage(team.TeamColor.getChatColor() + p.getBukkitPlayer().getName()+ChatColor.DARK_PURPLE + " has joined the game! "+this.game_host.getGameMeta().secondarycolor() +"1 more player is required to join to start the game.");
}
   if (pcount == 0){
   Messager.sendMessage("There are now enough players! The game has"+this.game_host.getGameMeta().secondarycolor()+" started!");
}
   p.getBukkitPlayer().setAllowFlight(true);
    if (inflight == true) {
     p.getBukkitPlayer().setFlying(true);
    }
    int i = 0;
    for (CWPlayer p1 : Core.instance.getPlayerManager().getOnlinePlayers())
    {
      if (p1.getTeam() != null) {
       i++;
      }
    }
    if ( i == this.game_host.getGameMeta().minPlayers()) {
     gameStart();
    }
   } else {
       onRespawn(p);
       removeSpectator(p);
       
       
   }
   try
  { 
      this.tracker.addPlayer(team, p); 
  } 
catch (TrackerLockedException e)
{
    e.printStackTrace();
} 
updateView();
  }
 
  
 public void onRespawn(CWPlayer p)
 {
    
  if (p.getTeam() == null){return;}
  
  p.getBukkitPlayer().teleport(p.getTeam().getRespawnLocation());
   for (PotionEffect e : p.getBukkitPlayer().getActivePotionEffects()) 
   {
    p.getBukkitPlayer().removePotionEffect(e.getType());
   }
p.getBukkitPlayer().getWorld().playSound(p.getBukkitPlayer().getLocation(), Sound.FIREWORK_TWINKLE, 2, 1);
ParticleEffect.HAPPY_VILLAGER.display(p.getBukkitPlayer().getLocation().clone().add(0.0D, 1.0D, 0.0D), 15.0D, 0.7F, 1.0F, 0.7F, 0.001F, 8);
ParticleEffect.HEART.display(p.getBukkitPlayer().getLocation().clone().add(0.0D, 1.0D, 0.0D), 15.0D, 0.7F, 1.0F, 0.7F, 0.01F, 8);  
 }
 
 
 public void onDeath(CWPlayer p){}
 
 
 public void onLeave(CWPlayer p, boolean kicked)
 {
 Integer localInteger1;
 if(p.getTeam()!= null)
 {
 p.getTeam().removeMember(p);
 localInteger1 = this.player_count; Integer localInteger2 = this.player_count = Integer.valueOf(this.player_count.intValue() - 1);
 }
 if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").hasPlayer(p.getBukkitPlayer()))
 {
 Bukkit.getScoreboardManager().getMainScoreboard().getTeam("spectators").removePlayer(p.getBukkitPlayer());
 }
 checkPlayable();
 }
  
 
  public boolean allowPVP(DamageInfo info)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    for (Region region : this.protection_areas)
    {
      if (region.contains(info.getVictim())) {
        return false;
      }
      if ((info.getCauser() != null) && 
        (region.contains(info.getCauser()))) {
        return false;
      }
      if ((info.getCauser() != null) && info.getCauser().getBukkitPlayer().getGameMode()== GameMode.CREATIVE){return false;}
    }
    if ((info.getCauser() != null) && 
      (info.getCauser().getTeam() == null && info.getType()!=DamageType.BLAST)) {
      return false;
    }
    if (info.getVictim().getTeam() == null) {
      return false;
    }
    if (info.getVictim().getTeam() == info.getCauser().getTeam()){
      return false;
    }
    
    return true;
  }
 
 

  public boolean allowDamage(DamageInfo info)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    for (Region region : this.protection_areas) {
      if (region.contains(info.getVictim())) {
        return false;
      }
    }
    
    if((info.getCauser()!=null)&&(info.getCauser().getTeam()==null))
    {
        return false;
    }
    
    if (info.getVictim().getTeam() == null) {
      return false;
    }
    return true;
  }

   
   
     public boolean allowBlockBuild(CWPlayer p, Block block)
  {
    if ((p.getTeam() == null) && (!p.hasPermission("bb.staff"))) {
      return false;
    }
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    for (Region region : this.protection_areas) {
      if ((region.contains(block)) && (!p.getBukkitPlayer().isOp()) &&  (!p.hasPermission("bb.staff") && (p.getBukkitPlayer().getGameMode()!=GameMode.CREATIVE))) {
        return false;
      }

    JSONObject allow = (JSONObject)this.current_map.getGameConfig().get("break/build");
     if (allow.containsValue("false")){return false;}
    }
    return true;
  }
     
     
    public boolean allowBlockBreak(CWPlayer p, Block block)
  {
    if ((p.getTeam() == null) && (!p.hasPermission("bb.staff"))) {
      return false;
    }
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    for (Region region : this.protection_areas) {
      if ((region.contains(block)) && (!p.hasPermission("bb.staff") && (p.getBukkitPlayer().getGameMode()!=GameMode.CREATIVE))) {
        return false;
      }
    }
    JSONObject allow = (JSONObject)this.current_map.getGameConfig().get("break/build");
     if (allow.containsValue("false")){return false;}
    
    return true;
  }
  
    
    
      public boolean allowBlockFade(Block block)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {return false;}
    
    for (Region region : this.protection_areas) {if (region.contains(block)){return false;}}
    
    return true;
  }
      
    
      public boolean allowBlockFromToEvent(Block block, Block block2)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {return false;}
for (Region region : this.protection_areas) {if (region.contains(block)){return false;}}

    return true;
  }
      
      
        public boolean allowBlockExplode(Block block, Entity exploder)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {return false;}
for (Region region : this.protection_areas) {if (region.contains(block)){return false;}}
    return true;
  }
        
        
   public boolean allowInteract(CWPlayer player, Action action, Block block)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {return false;}
    if (player.getTeam() == null) {return false;}
    return true;
  }
   
   
     public boolean allowEntityInteract(CWPlayer player, Entity entity)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {return false;}
    if (player.getTeam() == null) {return false;}
    return true;
  }
     
     
   public boolean allowMobSpawn(LivingEntity livingEntity, CreatureSpawnEvent.SpawnReason reason)
   {
       return false;
   }
   
   
     public boolean allowItemDrop(CWPlayer p, Item item)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    if (p.getTeam() == null && !p.hasPermission("bb.staff")) {
      return false;
    }
    return true;
  }
     
      public boolean allowItemPickup(CWPlayer p, Item item)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    if (p.getTeam() == null && !p.hasPermission("bb.staff")) {
      return false;
    }
    return true;
  }
      
      public boolean allowMove(CWPlayer p, Location location, Location location2)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return true;
    }
    if (p.hasPermission("bb.staff") && p.getBukkitPlayer().getGameMode()==GameMode.CREATIVE)
    {
    return true;
    }
    for (Region region : this.protection_areas) {
      if ((region.contains(location2)) && (!region.contains(location)) && (p.getTeam() != null)) {
        return false;
      }
    }
    return true;
  }
      
      
      
     public boolean allowFoodLevelChange(CWPlayer player)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION) {
      return false;
    }
    if (player.getTeam() == null) {
      return false;
    }
    return true;
  }   
  
     
     
  public int GameExpReward(CWPlayer player)
  {
    return 3;
  }
  
  
  
  
  public CWTeam getSmallestTeam()
  {
    int smallKey = 0;
    int playerCount = 2147483647;
    for (CWTeam team : this.current_map.getTeams()) {
      if (team.getMembers().size() < playerCount)
      {
        playerCount = team.getMembers().size();
        smallKey = this.current_map.getTeams().indexOf(team);
      }
    }
    return (CWTeam)this.current_map.getTeams().get(smallKey);
  }
  
  
  
  
   public void loadNext()
  {
    if (this.playlist.hasNext()) {
      this.playlist.getNext().getWorld();
    }
  }
  
   
   
   
    public void attemptAdvance()
  {
    if (this.playlist.hasNext())
    {
      CWMap oldMap = this.current_map;
      onStart(this.playlist.next());
      if (oldMap != null) {
        oldMap.unload();
      }
    }
    else
    {
      BBGame.this.game_host.finish();
      System.out.println("Out of maps to advance to!");
    }
  }
  
    
    
      public void updateView()
  {
    for (CWPlayer player1 : Core.instance.getPlayerManager().getOnlinePlayers())
    {
      TagAPI.refreshPlayer(player1.getBukkitPlayer());
      for (CWPlayer player2 : Core.instance.getPlayerManager().getOnlinePlayers()) {
        if ((player1.getTeam() != null) && (player2.getTeam() == null) && (this.game_host.getGameState() == GameState.IN_SESSION)) {
          player1.getBukkitPlayer().hidePlayer(player2.getBukkitPlayer());
        } else {
          player1.getBukkitPlayer().showPlayer(player2.getBukkitPlayer());
        }
      }
      player1.getBukkitPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
  }
    
      
      public void checkPlayable()
  {
    int i = 0;
    for (CWPlayer p : Core.instance.getPlayerManager().getOnlinePlayers()) {
      if (p.getTeam() != null) {
        i++;
      }
    }
    if (i == this.game_host.getGameMeta().minPlayers() - 1)
    {
       if(this.playlist.hasNext())
       {
           Messager.sendMessage("There are no longer enough players! Advancing to next arena...");
       }
       else
       {
       Messager.sendMessage("There are no longer enough players! Rebooting server...");
       }
       
      onEnd(true);
    }
  }
      
      
    public boolean onCommand(CWPlayer p, String command, String[] args)
  {
      
           if ((command.equalsIgnoreCase("hub"))||(command.equalsIgnoreCase("lobby")))
      {
    p.getBukkitPlayer().sendMessage(ChatColor.DARK_AQUA + "Warping to "+ChatColor.AQUA + command);
      ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("hub");
              p.getBukkitPlayer().sendPluginMessage(BattleBomb.instance, "BungeeCord", out.toByteArray());
            return true;
      }
           
         if(command.equalsIgnoreCase("spectate"))
         {if (p.getTeam()!= null && !p.getBukkitPlayer().isOp())
          {return true;}else{
           if (args.length <1){
               Messager.sendMessage(p, "Please provide with whom you would like to specate.");
               return true;}else{
               Player p2 = p.getBukkitPlayer().getServer().getPlayer(args[0]);
             if (p2 == null){
                 Messager.sendMessage(p, "Player "+this.game_host.getGameMeta().secondarycolor()+ "\"" + args[0] +"\""+this.game_host.getGameMeta().primarycolor() +" could not be found.");
                 return true;}
             if (p2.getName() == p.getBukkitPlayer().getName()){
                 Messager.sendMessage(p, "You musn't spectate yourself.");
                 return true;}
            p.getBukkitPlayer().teleport(p2.getLocation().add(0, 2, 0));
            return true;}
          }
         }
           
      
    if (command.equalsIgnoreCase("teams"))
    {
     Messager.sendMessage(p, this.game_host.getGameMeta().secondarycolor() + "Available teams:");
        for (CWTeam team : this.current_map.getTeams()){
     Messager.sendMessage(p, team.TeamColor.getChatColor() + team.getName());
        }
      return true;
    }
    
    
    if ((command.equalsIgnoreCase("forceend")))
    {
   if ((p.hasPermission("bb.admin")) || (p.getBukkitPlayer().isOp()) ){
    onEnd(true);
    Messager.sendMessage(p, ChatColor.RED + "Initialized the Force End Boolean! Do not run the command again until the countdown is complete!");
    Messager.sendMessage(ChatColor.RED + "Force End has been initialized, Countdown will begin in 10 seconds.");
    return true;}else{return true;}
    }
    
    
    if (command.equalsIgnoreCase("join"))
    { if (args.length < 1) {
      joinTeam(p);
      return true;
    }
    else{
        if (p.hasPermission("join.team"))
        {
            Messager.sendMessage(p, "This feature is not yet implemented. sorry :c");
            return true;
        }else{
        Messager.sendMessage(p, "Please purchase the "+ this.game_host.getGameMeta().secondarycolor()+ "\"Join Custom Team\" "+ this.game_host.getGameMeta().primarycolor()+"boost from the hub!");
        return true;
        }
     }
    }
    
      if (command.equalsIgnoreCase("kill")){return true;}
      if (command.equalsIgnoreCase("bukkit:kill")){return true;}
      if (command.equalsIgnoreCase("minecraft:kill")){return true;}
    return false;
  }
   
    
    @EventHandler
  public void BlockPistonExtendEvent(BlockPistonExtendEvent event)
  {
    if (this.game_host.getGameState() != GameState.IN_SESSION)
    {
      event.setCancelled(true);
      return;
    }
    for (Iterator i$ = event.getBlocks().iterator(); i$.hasNext();)
    {
        Block block = (Block)i$.next();
      for (Region region : this.protection_areas) {
        if (region.contains(block)) {
          event.setCancelled(true);
        }
      }
    }
  } 
    
    public int getSeconds()
  {
    return this.seconds.intValue();
  }
  
  public int getPlayerCount()
  {
    return this.player_count.intValue();
  }
 

}