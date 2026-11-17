package extermination;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Packets.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.net.Administration.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import arc.math.geom.*;
import static arc.util.Log.*;
import static mindustry.Vars.*;

import extermination.GameData.*;
import java.io.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExterminationMod extends Plugin{

    private EvictConfig ec;

	private Rules rules;
	
	private GameData data;
	private Leaderboard leaderboard;
	private boolean restarting = false, registered = false;
	private int gameID = 1;
	private float eCBR = 310f;
	private float eCBR2 = eCBR * eCBR;
	private Boolean endgame = false; 

	private static Rand rand = new Rand();

	private float resetUnitDamage = 1f;
	private float resetBlockDamage = 1f;

	private Seq<String> kickBotNames = new Seq<>();

	@Override
	public void init() {
 
		rules = new Rules();

		Pattern pattern = Pattern.compile("^\\d{4,6}$");

		kickBotNames.add("powerbottom toilet");
		kickBotNames.add("raid shadow legends");
		kickBotNames.add("nigger");
		kickBotNames.add("hitler");
		kickBotNames.add("nazi");

 		Config.all.find(conf -> conf.name.equalsIgnoreCase("desc")).set("FFA Hex PvP. No logic. https://discord.gg/GdnsrmKKAM");
 		Config.all.find(conf -> conf.name.equalsIgnoreCase("messageRateLimit")).set(2);
 		Config.all.find(conf -> conf.name.equalsIgnoreCase("enableVotekick")).set(false);

		rules.onlyDepositCore = true;
		rules.fog = false;
		rules.pvp = false;
		rules.tags.put("extermination", "true");
		rules.waves = false;
		rules.attackMode = false;
		rules.canGameOver = false;
		rules.coreIncinerates = true;
		rules.enemyCoreBuildRadius = eCBR;
		rules.fire = true;
		rules.unitCrashDamageMultiplier = 0.0f;

		Team fallen = Team.get(14);
		fallen.name = "Fallen";

		for(Block b : content.blocks()) {
			if(b instanceof CoreBlock) { 
				CoreBlock bc = (CoreBlock) b;
				for(Weapon w : bc.unitType.weapons) {
					w.bullet.damage = 0f; }}}

        ec = new EvictConfig();

		Events.on(PlayerJoin.class, event -> {
			Call.infoMessage(event.player.con, "Welcome to Extermination. A fork by banddans of eviction by larofeticus. \n/gameplay for how to play.\n\n" + leaderboard.topCardString() + "\n\nUse /attack and /rally to send an order to all units. /fullassault sends an attack command every 5 seconds. Units further than 1 hex away from a friendly core suffer attrition. At 90 minutes full assault is locked on for all teams and all cores start spawning air units.[white]");
			if(!data.hasNucleus(event.player)) {
				//need a nucleus
				if(!data.spawnNucleus(event.player.uuid())) {
					//nucleus spawn failed, move to fallen
                                        event.player.sendMessage("No parcels available to spawn, assigning to Fallen team, which currently has " + fallen.cores().size + " cores.");
					data.playerInitTeam(event.player, true);
					
				} else {
					//put player on their member team
		 			data.playerInitTeam(event.player, false);	
					event.player.sendMessage(data.welcomeString());
				}
			} else {
		 		data.playerInitTeam(event.player, false);	
				event.player.sendMessage(data.welcomeString());
			}
		});

		Events.on(RestartEvent.class, event -> { restartGame(); } );

		netServer.assigner = (player, players) -> {
			int tier = leaderboard.tierFromUuid(player.uuid()); 
 
			String prefix = "[]|| ";
                	if(tier == 2) { prefix = "[]|| "; }
                	if(tier == 3) { prefix = "[]|| "; }
                	if(tier == 4) { prefix = "[]|| "; }
                	if(tier == 5) { prefix = "[]|| "; }
                	if(tier == 6) { prefix = "[]|| "; }

			player.name = prefix + player.name + "[white]";

			return data.playerConnect(player, tier);			
        	};

		netServer.admins.addActionFilter(action -> {
			if(action.type == ActionType.control) return false;
			if(action.type == ActionType.command) return false;
			//if(action.type == ActionType.depositItem) {
			//	if(action.block instanceof CoreBlock) { return true; }
			//	if(rules.env == Planets.serpulo.defaultEnv) { return false; }
			//}
			return true;
		});

	        Events.on(ConnectPacketEvent.class, c -> {
			Matcher matcher = pattern.matcher(c.packet.name);
			if(matcher.find()) c.connection.kick(KickReason.kick);
			for(String s : kickBotNames) {
				if(c.packet.name.contains(s)) {
					c.connection.kick(KickReason.kick); }}});	

		Events.on(BlockDestroyEvent.class, event -> {
			if(restarting) { return; }
 
            		if(event.tile.block() instanceof CoreBlock) {
				float x, y;
				x = event.tile.getX();
				y = event.tile.getY();
				Log.info("core destroyed at " + x + " " + y);
                		Parcel parcel = data.getParcelNearPos(Math.round(x/8), Math.round(y/8));
				
				Team loser = event.tile.team();

				String conquer = "";
				parcel.status = Parcel.fallen;

	                        Team t = parcel.countTeamUnits(loser.id);
				String victim = data.teamidToUuid(loser.id);
				if(t != null){
					//Log.info("most units " + t.name);
					conquer = data.teamidToUuid(t.id);
					parcel.status = Parcel.province;
					data.setCoreLoss(conquer, victim); } 
				else {
					Log.info("no eligible capture units, default to fallen");
					t = Team.get(14);
					parcel.status = Parcel.fallen; }

				for(Parcel adjacent : parcel.adjacents){
					if(adjacent.status == Parcel.vacant){
						adjacent.vacantToFallen(fallen); } }


				if(victim != "" && loser.cores().size == 1 && !data.isFallen(victim)) {
					conquer = data.getInviteRecipient(victim);

					if(conquer.equals(victim)) { parcel.status = Parcel.fallen; conquer = data.teamidToUuid(14); }

					if(data.isFallen(conquer) && data.isBeforeTenMinutes()) { 
						Call.sendMessage("[yellow](!)[] [accent]" + loser.name + "[lightgray] has killed themself![yellow] (!)" + "[white] They will derive no benefit from it.");
						data.suicideRespawn(victim); }	
					else { 
						Call.sendMessage("[yellow](!)[] [accent]" + loser.name + "[lightgray] has fallen![yellow] (!)" + "[white] evicted by " + data.uuidToName(conquer) + " [white]team.");
						data.uuidKillsUuid(conquer, victim); } }


				Seq<TeamData> active = state.teams.getActive();
				if(loser.cores().size == 1) { active.remove(loser.data()); }

				if(active.size == 1) {
					if(active.first().team.id == 14) {
						if(!restarting) {
							String mess = "Fallen victory.\n Leaderboard loading error: No results will be recorded. Next game starts in 15 seconds.";
							if(leaderboard.loaded()) {
								mess = data.fallenVictoryCard(victim);
								data.fallenVictoryRecord(victim);
							}
							for(Player p : Groups.player) {
								Call.infoMessage(p.con, mess); }
							data.writeGameResults("");
                            Events.fire(new RestartEvent());
						}
					} else {
						Seq<Parcel> copy = data.parcels().copy();
						copy.retainAll(h -> h.vacant());
						if(copy.size > 0){
							copy.shuffle();
							copy.first().vacantToFallen(fallen);	
						} else {
							if(!restarting && !conquer.equals("")) {
								String mess = "Game over. Victory for " + active.first().team.name + "\n Leaderboard loading error: no results will be recorded.\nNext game starts in 15 seconds.";
								if(leaderboard.loaded()) {
									mess = data.playerVictoryCard(conquer);
									data.playerVictoryRecord(conquer);
								}
								for(Player p : Groups.player) {
									Call.infoMessage(p.con, mess); }
								data.writeGameResults(data.teamidToUuid(active.first().team.id));
								Events.fire(new RestartEvent()); } } }
							}
				
				final Team lambdaTeamArgument = t;
				final Parcel lambdaParcelArgument = parcel;
				Timer.schedule(() -> { 
					int ox = Math.round(parcel.center_x), oy = Math.round(parcel.center_y);
					Tile tile = world.tile(ox, oy);
					if(rules.env == Planets.serpulo.defaultEnv) { tile.setNet(Blocks.coreShard, lambdaTeamArgument, 0); }
					else { tile.setNet(Blocks.coreBastion, lambdaTeamArgument, 0); } 
					lambdaParcelArgument.removeBuildings();
				}, 6f);
            		}
        	});

	}

	private void startExtermination() {
		data = new GameData(ec);
		data.initFallen();
		data.initParcels();

		logic.reset();
		Log.info("Generating map.");
		if(rules.env == Planets.serpulo.defaultEnv) {
                	ExterminationGenerator generator = new ExterminationGenerator(data);
                	Log.info("after constructor");
                	world.loadGenerator(ec.mapSizeTiles, ec.mapSizeTiles, generator);
		}
		else {
			ErekirGenerator generator = new ErekirGenerator(data);
                        Log.info("after constructor");
                        world.loadGenerator(ec.mapSizeTiles, ec.mapSizeTiles, generator);
		}
                info("Map generated.");
                state.rules = rules.copy();
		leaderboard = new Leaderboard();

		Runnable isolationAttrition = () -> { 
			for(Unit u : Groups.unit) {
				Building core = u.closestCore();
				Vec2 distance = new Vec2(eCBR * 3, eCBR * 3);
				if(core != null) { distance = new Vec2(core.x() - u.x(), core.y() - u.y()); }
				if(distance.len2() > eCBR2 * 9) {
					if(rand.chance(0.2f)) { Core.app.post(() -> Call.unitDestroy(u.id)); }	
				} 
			}
		};

		Timer.schedule(isolationAttrition, 0, 5);

		Runnable fullAssaultCommander = () -> {
			Seq<Team> l = data.getFullAssaultTeams();
			for(Team t : l) {
				Seq<Unit> seq = t.data().units;
                        	for(Unit u : seq) {
                                	if(!u.isCommandable()) { continue; }
                                	Building core = u.closestEnemyCore();
                                	if(core == null) { break; }
                                	if(u.isGrounded()) {
                                	        Seq<Tile> moveOptions = new Seq<>();
                                	        for(int x = core.tile.x - 6; x < core.tile.x + 6; x++) {
                                	                for(int y = core.tile.y - 6; y < core.tile.y + 6; y++) {
                                	                        if(!world.tile(x, y).solid()) { moveOptions.add(world.tile(x,y)); }
                                	                }
                                	        }
                               	 	        if(!moveOptions.isEmpty()) {
                                	                moveOptions.shuffle();
                                        	        u.command().commandPosition(new Vec2(moveOptions.first().getX(), moveOptions.first().getY()));
                                        	}
                                	} else {
                                        	u.command().commandTarget(core); }
                        	}
			}
		};

		Timer.schedule(fullAssaultCommander, 0, 5);

		Runnable endgameSpawns = () -> {
			if(endgame) {
				mindustry.type.UnitType unit = content.units().find(b -> b.name.equals("antumbra"));
				if(rules.env == Planets.erekir.defaultEnv) { unit = content.units().find(b -> b.name.equals("obviate")); };
				for(TeamData td : state.teams.present) {
					for(CoreBuild c : td.cores) {
						unit.spawn(td.team, c.getX(), c.getY());
					}
				}
			}
		};

		Timer.schedule(endgameSpawns, 0, 19);

                Runnable setTierEvent = () -> { data.setGameTier(); Call.sendMessage(data.getParticipantsString()); };
                Timer.schedule(setTierEvent, 60 * 10);

		Runnable tenMinWarning = () -> { Call.sendMessage("Offworld reinforcements will arrive and finish the battle in ten minutes."); };
                Timer.schedule(tenMinWarning, data.getGameLength() - 600);

		Runnable triggerEndgame = () -> { 
			endgame = true;
			data.setFullAssault();
			rules.unitDamageMultiplier = 3.5f;
			Call.sendMessage("Offworld reinforcments are here to end the battle."); };
		    Timer.schedule(triggerEndgame, data.getGameLength());
            Runnable deadExit = () -> { restartGame(); };
            Timer.schedule(deadExit, 60 * 171);


            logic.play();
	}

	@Override
	public void registerServerCommands(CommandHandler handler){
		handler.register("extermination_s", "Begin hosting with the Extermination: Serpulo gamemode.", args -> {
			if(!state.is(State.menu)){
				Log.err("Stop server first.");
				return;
			}

			//init serpulo only content
			Config.all.find(conf -> conf.name.equalsIgnoreCase("name")).set("Extermination: Serpulo");

			rules.onlyDepositCore = true;
			rules.env = Planets.serpulo.defaultEnv;
			rules.buildSpeedMultiplier = 1.4f;
			resetUnitDamage = 0.5f;
			resetBlockDamage = 0.5f;
			rules.unitDamageMultiplier = resetUnitDamage;
			rules.blockDamageMultiplier = resetBlockDamage;
			rules.bannedBlocks.add(Blocks.microProcessor);
			rules.bannedBlocks.add(Blocks.logicProcessor);
			rules.bannedBlocks.add(Blocks.hyperProcessor);
			rules.bannedBlocks.add(Blocks.logicDisplay);
			rules.bannedBlocks.add(Blocks.largeLogicDisplay);
			rules.bannedBlocks.add(Blocks.foreshadow);
			rules.bannedBlocks.add(Blocks.ripple);

			//rules.bannedBlocks.add(Blocks.tsunami);
			//rules.bannedBlocks.add(Blocks.wave);


	 		rules.loadout = ItemStack.list(Items.copper, 900, Items.lead, 900, Items.silicon, 300, Items.scrap, 1000, Items.graphite, 200, Items.metaglass, 100, Items.titanium, 50);

			startExtermination();
			netServer.openServer();
		});

		handler.register("extermination_e", "Begin hosting with the Extermination: Erekir gamemode.", args -> {
                        if(!state.is(State.menu)){
                                Log.err("Stop server first.");
                                return;
                        }

                        //init erekir only content
                        Config.all.find(conf -> conf.name.equalsIgnoreCase("name")).set("Extermination: Erekir");

			rules.planet = Planets.erekir;
			rules.env = Planets.erekir.defaultEnv;
			resetUnitDamage = 1f;
			resetBlockDamage = 1f;
			rules.unitDamageMultiplier = resetUnitDamage;
			rules.blockDamageMultiplier = resetBlockDamage;
			rules.bannedBlocks.add(Blocks.canvas);

			//rules.bannedUnits.add(UnitTypes.quell);

                        rules.loadout = ItemStack.list(Items.beryllium, 1100, Items.graphite, 410, Items.silicon, 410, Items.tungsten, 200);

                        startExtermination();
                        netServer.openServer();
                });

		handler.register("botban", "[substring]", "Add a substring to the auto kick on connect list", arg -> {
			String total = "";
			for(String s : arg) total = total + s + " ";
			total.substring(0, total.length()-2);
			kickBotNames.add(total);
			info("adding " + total + " to name substring kick list");
		}); 

		handler.register("teamstate", "Display state of each player team.", args -> {
			String listing = data.gameStateString();
			Log.info(listing);
		});

		handler.register("eliminate", "[teamid] [teamid]", "manual elimination of second team by first", arg -> {
			if(arg.length == 2) {
				int t1 = Integer.parseInt(arg[0]);
				int t2 = Integer.parseInt(arg[1]);
				String uuid1 = data.teamidToUuid(t1);
				String uuid2 = data.teamidToUuid(t2);
				data.uuidKillsUuid(uuid1, uuid2);
			} else {
				info("need two teamid");
			}
		});

		handler.register("createteam", "[name]", "debug create an empty new team", arg -> {
			if(arg.length == 1) {
				data.createTeam(arg[0]);
			} else {
				info("need a team name");
			}
		});

		handler.register("invite", "[teamid] [teamid]", "manual first player invites second player", arg -> {
			if(arg.length == 2) {
 				int t1 = Integer.parseInt(arg[0]);
                                int t2 = Integer.parseInt(arg[1]);
                                String uuid1 = data.teamidToUuid(t1);
                                String uuid2 = data.teamidToUuid(t2);
				data.uuidInviteUuid(uuid1, uuid2);
			} else {
				info("must proviate 2 teamid");
			}
		});

		handler.register("restartgame", "end the current game and start a new one", arg -> {
			data.writeGameResults("none");
			restartGame();
		});

		handler.register("time", "Display age of current game.", arg -> {
			info(GameData.timeString(data.gameStart));
		});

		handler.register("testleaderboard", "Debug dump of leaderboard", arg -> {
			leaderboard.debugDumpLeaderboard();
		});

	}

	@Override
	public void registerClientCommands(CommandHandler handler){
		if(registered) return;
		registered = true;


		handler.<Player>register("invite", "[number]", "Without giving a number this shows players you've eliminated and their numbers. Include a player number to bring a player onto your team", (args, player) -> {
			//naked command
			if(args.length == 0) {
				//player is active
				if(data.isAlive(player.uuid())) {
					player.sendMessage(data.uuidInviteString(player.uuid()));
					Log.info("invite 0 arguments, player is active"); }
				//player slaved to an active team
				else if(data.isSlave(player.uuid())) {
					player.sendMessage("You cannot use invite when taken by another team.");
					Log.info("invite 0 arguments, player is slave"); }
				//on fallen
				else if(data.isFallen(player.uuid())) {
					Log.info("invite 0 arguments, Player is fallen");
					if(data.isClaimed(player.uuid())) {
						Log.info("is claimed"); 
						player.sendMessage(data.getClaim(player.uuid())); }
					else {
						Log.info("is not claimed"); 
						player.sendMessage(data.getClaim(player.uuid()));
						data.buildSuitorList(player.uuid());
						player.sendMessage(data.getSuitorListString(player.uuid())); } }
				//uh oh
				else {
					Log.info("invite 0 arguments, player state failed"); 
					player.sendMessage("Invite state conditionals failed. Tell Laro there's a bug."); }
			} 
                        //looking for 1 argument, number
			else if(args.length == 1) {
				//check that the argument parses to int
				try {
					int id = Integer.parseInt(args[0]);
 
					//player is active
                                	if(data.isAlive(player.uuid())) {
						Boolean outcome = data.inviteByCardinal(player.uuid(), id);
						if (outcome) { player.sendMessage("successfully invited player to your team"); }
						else { player.sendMessage("invite failed."); } 
                                	        Log.info("invite 1 arguments, player is active"); }
                                	//player slaved to an active team
                                	else if(data.isSlave(player.uuid())) {
						player.sendMessage("You cannot use invite when taken by another team.");
                                	        Log.info("invite 1 arguments, player is slave"); }
                                	//on fallen
                                	else if(data.isFallen(player.uuid())) {
 						if(data.isClaimed(player.uuid())) {
                                                	Log.info("is claimed"); 
                                                	player.sendMessage(data.getClaim(player.uuid())); }
                                        	else {  
                                                	Log.info("is not claimed");
                                                	player.sendMessage(data.getClaim(player.uuid())); 
							if(data.requestJoin(player.uuid(), id)) { player.sendMessage("Request to join sent."); }
							else { player.sendMessage("Request failed."); }
						}

                                	        Log.info("invite 1 arguments, Player is fallen"); }
                                	//uh oh
                                	else {  
						player.sendMessage("Invite state conditionals failed. Tell Laro there's a bug.");
                                        	Log.info("invite 1 arguments, player state failed"); }

				}
				//parse failed
				catch ( NumberFormatException e ) {
					player.sendMessage("/invite failed. Argument should be a natural number. Show list of invitable players and the number of each with `/invite`, use that number with `/invite #` to invite that player onto your team. Don't type their name."); } } 
			//wrong argument count
			else {
				player.sendMessage("Wrong number of arguments, expecting 0 or 1. Show list of invitable players and the number of each with `/invite`, use that number with `/invite #` to invite that player to your team."); }
		});

		handler.<Player>register("die", "Surrender, destroy your buildings and cores, and join the Fallen team. Only available after 10 minutes.", (args, player) -> {
			if(data.isFallen(player.uuid())) {
				player.sendMessage("You are already dead.");
				return; }
			if(data.isSlave(player.uuid())) {
				player.sendMessage("You are not the leader of your team.");
				return; }
			if(data.isBeforeTenMinutes()) {
				player.sendMessage("Command not available before 10 minutes.");
				return; }
			
			Rand r = new Rand();
			Team t = Team.get(player.team().id);
			for(CoreBlock.CoreBuild c : t.cores()) {
				Parcel p = data.getParcelNearPos(Math.round(c.x/8), Math.round(c.y/8));
				Runnable killOneCoreEvent = () -> { p.fail(); };
				Timer.schedule(killOneCoreEvent, r.nextInt() % 30); }
		}); 

		handler.<Player>register("serverinfo", "Display server info", (args, player) -> { 
			player.sendMessage("https://discord.gg/GdnsrmKKAM");
		});

		handler.<Player>register("gameplay", "Read how Extermination gameplay works.", (args, player) -> {
			String message = "How to play Extermination:\nCapture all cores from players and the Fallen team to win.  Destroy a core to capture it.  Core explosions kill all nearby buildings, some nearby units, and will spawn fallen cores in adjacent vacant cells.  Units too far away from any core suffer attrition. A player that loses all cores will be moved to the Fallen team.  If you eliminate a player you can use the /invite command to bring them onto your team. If you spawn as Fallen you can use /invite to ask to join another team. At 90 minutes players recieve free units at each core and are forced to attack until the game ends.";
			player.sendMessage(message);
		});

		handler.<Player>register("time", "How long ago did game start? How long ago did you join?", (args, player) -> {
			String message = GameData.timeString(data.gameStart) + " since game start\n";
			message = message + GameData.timeString(data.getPlayerJoin(player)) + " since you joined\n";
			player.sendMessage(message);
		});

		handler.<Player>register("tier", "Report the challenge tier of the current game and the competing players.", (args, player) -> {
			String message = data.getParticipantsString();
			player.sendMessage(message);
		});

		handler.<Player>register("top", "Show top players on server and their statistics.", (args, player) -> {
			String message = leaderboard.topCommandString();
			player.sendMessage(message);
		}); 

		handler.<Player>register("rank", "Show your own rank and statistics.", (args, player) -> {
			String message = leaderboard.statsFromUuid(player.uuid());
			player.sendMessage(message);
		});

		handler.<Player>register("fullassault", "Automatically issue an /attack command every 5 seconds.", (args, player) -> {
			if(endgame) { player.sendMessage("Full assault cannot be turned off during the endgame."); return; }
			Boolean outcome = data.toggleFullAssault(player.uuid());
			if(outcome) { player.sendMessage("Full assault command enabled."); }
			else { player.sendMessage("Full assault command disabled."); }
		});

		handler.<Player>register("attack", "Command all team units to attack the nearest enemy core.", (args, player) -> {
			Team t = Team.get(player.team().id);
			Seq<Unit> seq = t.data().units;
			for(Unit u : seq) {
				if(!u.isCommandable()) { continue; }
                                Building core = u.closestEnemyCore();
				if(core == null) { break; }
				if(u.isGrounded()) {
				        Seq<Tile> moveOptions = new Seq<>();
					for(int x = core.tile.x - 6; x < core.tile.x + 6; x++) {
						for(int y = core.tile.y - 6; y < core.tile.y + 6; y++) {
							if(!world.tile(x, y).solid()) { moveOptions.add(world.tile(x,y)); }	
						}
					}
					if(!moveOptions.isEmpty()) {
						moveOptions.shuffle();
						u.command().commandPosition(new Vec2(moveOptions.first().getX(), moveOptions.first().getY()));
					}
				} else {
					u.command().commandTarget(core); }
			}
		});

		handler.<Player>register("rally", "Command all team units to move to the player.", (args, player) -> {
			if(endgame) { player.sendMessage("Rally command is not available at the end of a game."); return; }
			Team t = Team.get(player.team().id);
			Seq<Unit> seq = t.data().units;
			for(Unit u : seq) {
				if(!u.isCommandable()) { continue; }
				if(u.isGrounded()) {
					Seq<Tile> moveOptions = new Seq<>();
					Tile pt = world.tile((int)Math.round(player.unit().x() / 8.0), (int)Math.round(player.unit().y() / 8.0));
					for(int x = pt.x - 6; x < pt.x + 6; x++) {
                                                for(int y = pt.y - 6; y < pt.y + 6; y++) {
                                                        if(world.tile(x,y) != null && !world.tile(x, y).solid()) { moveOptions.add(world.tile(x,y)); } 
                                                }
                                        }
                                        if(!moveOptions.isEmpty()) {
                                                moveOptions.shuffle();
                                                u.command().commandPosition(new Vec2(moveOptions.first().getX(), moveOptions.first().getY()));
                                        }

				} else {
					u.command().commandPosition(new Vec2(player.unit().x(), player.unit().y())); }
			}
		});
	}

	void restartGame() {
		if(restarting) { return; }
		restarting = true;
		Timer.instance().clear();
		gameID = gameID + 1;
		Log.info("restart game event");
		endgame = false;
		rules.unitDamageMultiplier = resetUnitDamage;
		rules.blockDamageMultiplier = resetBlockDamage;

                String[] cmd = new String[] { "/bin/bash", "-c", "python3 /root/GameRecords_v7/updateDatabase.py /root/GameRecords_v7/database.csv  /root/GameRecords_v7/update.csv  /root/GameRecords_v7/serverInput.data" };
		
		Log.info(rules.env + " & " + Planets.erekir.defaultEnv);
		if(rules.env == Planets.erekir.defaultEnv) {
			Log.info("load erekir!");
			cmd = new String[] { "/bin/bash", "-c", "python3 /root/GameRecords_erekir/updateDatabase.py /root/GameRecords_erekir/database.csv  /root/GameRecords_erekir/update.csv  /root/GameRecords_erekir/serverInput.data" };	
		}

		try {
			ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
                        Process process = pb.start();
                        StringBuilder result = new StringBuilder(80);
                        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while(true) {
                                String line = in.readLine();
                                if(line == null) break;
                                result.append(line).append("\n");
                        }
                        Log.info(result);

			
		} catch (Exception e) {
			Log.info(e.getMessage());
		}
		Runnable r = () -> {
			Seq<Player> players = new Seq<>();
			for(Player p : Groups.player) { players.add(p); }
			players.shuffle();

			for(Player p : players) { 
				if(p == null) { Log.info("groups players has a null player?"); }
				if(p.name == null) { 
					Log.info("on restart, players has a player with null name?"); 
					continue; }
				if(p.name.substring(6) == null) {
					Log.info("the substring is null?"); 
					continue; }
				p.name = p.name.substring(6); }

			WorldReloader reloader = new WorldReloader();
			state.set(State.menu);
			reloader.begin();
			startExtermination();
			reloader.end();
			
			for(Player p : players) { 
				int tier = leaderboard.tierFromUuid(p.uuid());

				String prefix = "[]|| ";
                        	if(tier == 2) { prefix = "[]|| "; }
                        	if(tier == 3) { prefix = "[]|| "; }
                        	if(tier == 4) { prefix = "[]|| "; }
                        	if(tier == 5) { prefix = "[]|| "; }
                        	if(tier == 6) { prefix = "[]|| "; }

                        	p.name = prefix + p.name;

				data.playerConnect(p, tier);
				Events.fire(new PlayerJoin(p)); } 
			restarting = false;
		};
		Timer.schedule(r, 15);
	}
}
