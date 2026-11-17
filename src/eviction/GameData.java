package eviction;

import arc.*;
import arc.math.geom.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.game.Schematic.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.*;
import mindustry.content.*;
import mindustry.type.*;
import static mindustry.Vars.*;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Arrays;
import java.io.*;

public class GameData {

    public EvictConfig ec;

	private Seq<Parcel> parcels = new Seq<>();
    private Parcel[][][] parcelIndex;
	
	public static final int parcel_width = 8;
	public static final int parcel_height = 9;
 
	private Seq<PlayerTeam> teamData = new Seq<>();
	private static int teamAvailable = 8;

	private static int gameTier = 0;
	private Seq<String> peerUuid = new Seq<>();
	private int[] participants = {0, 0, 0, 0, 0, 0, 0};

    Integer[] intArray = new Integer[]{8,9,12,19,22,30,33,46,60,51,110,116,141,149,163,164,171,210,228};

    private Seq<Integer> useTeams = new Seq<>(intArray);

	public long gameStart = System.currentTimeMillis() / 1000L;
	public static final long gameLength = 90 * 60L; 

    public GameData(EvictConfig evc) {
        ec = evc;
        parcelIndex = new Parcel[ec.hexLayers*2+2][ec.hexLayers*2+2][ec.hexLayers*2+2];
    }

	public static class PlayerTeam {
		String uuid;
		String name;
		int tier;
		Team team;
		PlayerTeam member;
		Boolean fallen;
		Boolean nucleus;
		Boolean oneMinute;
		float score;
		int players;
		Seq<String> victims;
		Seq<String> suitors;
		long joinTime;
		long fallTime = 0;
		Boolean fullAssault = false;
		long previousLossTime;
		String previousLossUuid = "";
	}

	public void setCoreLoss(String killer, String loser)
	{
		PlayerTeam l = uuidToData(loser);
		if(l == null) return;
		l.previousLossTime = System.currentTimeMillis() / 1000L;
		if(killer.equals(loser)) { l.previousLossUuid = ""; return; }
		l.previousLossUuid = killer;
	}

	public String getInviteRecipient(String loser)
	{
		PlayerTeam l = uuidToData(loser);
		if(l == null) return "";
		if(System.currentTimeMillis() / 1000L - l.previousLossTime > 60) return "";
		return l.previousLossUuid;
	}

	public void debugTeamDataDump() {
		String output = "";
        //output = output + "Team data structure state:\nNext available team id " + teamAvailable + "\n";
		output = output + "playerTeam count " + teamData.size + "\n";
		for(PlayerTeam pt : teamData) {
			output = output + "\tuuid: " + pt.uuid + " name: " + pt.name + "\n";
			output = output + "\t\t[white]Self team: " + pt.team.id + " member team: " + pt.member.team.id + "\n";
			output = output + "\t\tfallen? " + pt.fallen + " nucleus? " + pt.nucleus + "\n";
			output = output + "\t\tscore: " + pt.score + " players on team: " + pt.players + "\n";
			for(String uuid : pt.victims) {
				output = output + "\t\t\tvictim: " + uuid + "\n";
			} 
		}
		Log.info(output);
	}

	public String gameStateString() {
		String output = "";
		for(PlayerTeam pt : teamData) {
			output += pt.team.id + ") ";
			output += "leader: " + pt.name + " ";
			output += "[white]players: " + pt.players + " ";
			output += "fallen? " + pt.fallen + " ";
			output += "score: " + pt.score + "\n";
			output += "team name: |" + pt.team.name + "|\n";
		}
		return output;
	}

	public long getGameLength() { return gameLength; }

	public long getPlayerJoin(Player player) {
		return playerToData(player).joinTime; }

	private PlayerTeam playerToData(Player player) {
		for(PlayerTeam pt : teamData) {
                        if(pt.uuid.equals(player.uuid())) {
                                return pt; } }
		return null;
	}

	private PlayerTeam uuidToData(String uuid) {
		for(PlayerTeam pt : teamData) {
			if(pt.uuid.equals(uuid)) { return pt; } } 
		return null;
	}

	public String uuidToName(String uuid) {
		PlayerTeam pt = uuidToData(uuid);
		return pt.name;
	}

	public String teamidToUuid(int teamid) {
		for(PlayerTeam pt : teamData) {
			if(pt.team.id == teamid) { return pt.uuid; } }
		return "";
	}

	public void initFallen() {
		Log.info("initFallen");
        useTeams = new Seq<>(intArray);
        useTeams.shuffle();
		teamAvailable = useTeams.remove(0);
		PlayerTeam pt = new PlayerTeam();
		pt.tier = 0;
        pt.uuid = "";
		pt.name = "Fallen";	
        pt.team = Team.get(14);
		pt.team.name = "Fallen";
		pt.member = pt;
        pt.score = 0;
		pt.fallen = true;
		pt.nucleus = false;
		pt.oneMinute = false;
		pt.victims = new Seq<>();
		pt.suitors = null;
		pt.players = 0;
        pt.joinTime = System.currentTimeMillis() / 1000L;
        teamData.add(pt);
		//debugTeamDataDump();
	}
	
	public Team playerConnect(Player player, int tier) {

        int teamHolder;
		PlayerTeam pt = playerToData(player);

		Log.info("player connect " + player.name + "[white]");
		if(pt != null) { return pt.team; }
        //if(teamAvailable >= 254) {
		if(useTeams.size <= 0) {
			//we're out of teams. try not to crash, eh?
			pt = teamData.first();
			return pt.team; }

		pt = new PlayerTeam();
		//pt.originalName = player.name.replace("~","");		
		pt.uuid = player.uuid();
		pt.tier = tier;
		pt.name = player.name;
        teamHolder = useTeams.remove(0);
		pt.team = Team.get(teamHolder);
		pt.team.name = player.name;
        //teamAvailable++;
        //while(skipTeams.contains(teamAvailable)) teamAvailable++;
		pt.member = pt;
		pt.score = 0;
		pt.fallen = false;
		pt.nucleus = false;
		pt.oneMinute = false;
		pt.players = 1;
		pt.victims = new Seq<>();
		pt.suitors = null;
		pt.joinTime = System.currentTimeMillis() / 1000L;
		teamData.add(pt);
		//debugTeamDataDump();
		return pt.team;
	}

	public void createTeam(String name) {
		Log.info("start player debug create " + name);
                //debugTeamDataDump();
        int teamHolder = useTeams.remove(0);

		PlayerTeam pt = new PlayerTeam();

		pt.uuid = UUID.randomUUID().toString();
		pt.name = name;
		pt.tier = 0;
		pt.team = Team.get(teamHolder);
		pt.team.name = name;
		//teamAvailable++;
        //while(skipTeams.contains(teamAvailable)) teamAvailable++;
		pt.member = pt;
		pt.score = 0;
		pt.fallen = false;
		pt.nucleus = false;
		pt.oneMinute = false;
		pt.players = 1;
		pt.victims = new Seq<>();

		spawnNucleus(pt.uuid);

		teamData.add(pt);
		Log.info("start player debug create");
                //debugTeamDataDump();
	}

	public Boolean hasNucleus(Player player) {
		PlayerTeam pt = playerToData(player);

                if(pt == null) return false;
                return pt.member.nucleus;
	}

	public Boolean spawnNucleus(String uuid) {
		Log.info("spawn nucleus for " + uuid);
		PlayerTeam pt = uuidToData(uuid);

		if(pt == null) {
			Log.info("error: spawnNucleus didn't find player uuid in team data list");
			return false; }
		pt = pt.member;

		Seq<Parcel> copy = parcels().copy();	
		copy.retainAll(h -> h.vacant());
		if(copy.isEmpty()) {
			Log.info("actually, no space for it");
			return false;
		}
        int adjacent_min = copy.min(h -> h.adjacents.size < 2 ? h.adjacents.size + 10 : h.adjacents.size).adjacents.size;
		copy.retainAll(h -> h.adjacents.size == adjacent_min);
        copy.shuffle();
		Parcel spawn = copy.first();
		spawn.removeBuildings();
		spawn.removeUnits();

		spawn.status = Parcel.fallen;
		if(!pt.fallen) { spawn.status = Parcel.capital;	}	
		pt.nucleus = true;
 
		if(ec.planet == ec.serpulo) {
			Stile coreTile = ec.serpPlayerStart.tiles.find(s -> s.block instanceof CoreBlock);
            if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
            int ox = spawn.mapx() - coreTile.x, oy = spawn.mapy() - coreTile.y;
			final Team lambdaTeam = pt.team;
            ec.serpPlayerStart.tiles.each(st -> {
                Tile tile = world.tile(st.x + ox, st.y + oy);
                if(tile == null) return;
	            if(tile.block() != Blocks.air) { tile.removeNet(); }
	            tile.setNet(st.block, lambdaTeam, st.rotation);
	            if(st.config != null) { tile.build.configureAny(st.config); }
	
	            if(tile.block() instanceof CoreBlock) {
	                for(ItemStack stack : state.rules.loadout) {
	                    Call.setItem(tile.build, stack.item, stack.amount); } }      
	        });
		}
		else {  //erekir spawn
			Tile tile = world.tile(spawn.mapx(), spawn.mapy());
			tile.setNet(Blocks.coreCitadel, pt.team, 0);
			for(ItemStack stack : state.rules.loadout) {
                Call.setItem(tile.build, stack.item, stack.amount); } }

                for(Parcel adjacent : spawn.adjacents) {
                	if(adjacent.vacant()){ adjacent.vacantToFallen(Team.get(14)); } }
	
		return true;
	}

	public boolean suicideRespawn(String uuid)
	{
		Log.info("suicide respawn for " + uuid);
        PlayerTeam pt = uuidToData(uuid);

        if(pt == null) {
            Log.info("error: suicideRespawn didn't find player uuid in team data list");
            return false; }
        pt = pt.member;
 
        Seq<Parcel> copy = parcels().copy();
        copy.retainAll(h -> h.vacant());
        if(copy.isEmpty()) {
            Log.info("actually, no space for it");
            return false; }
		
		int adjacent_min = copy.min(h -> h.adjacents.size < 2 ? h.adjacents.size + 10 : h.adjacents.size).adjacents.size;
        copy.retainAll(h -> h.adjacents.size == adjacent_min);
        copy.shuffle();
        Parcel spawn = copy.first();
        spawn.removeBuildings();
        spawn.removeUnits();

		spawn.status = Parcel.capital; 
        pt.nucleus = true;

		Tile tile = world.tile(spawn.mapx(), spawn.mapy());
		if(ec.planet == ec.serpulo) {
 			tile.setNet(Blocks.coreShard, pt.team, 0); }
		else {  //erekir spawn
            tile.setNet(Blocks.coreBastion, pt.team, 0); }
                
		for(Parcel adjacent : spawn.adjacents) {
                        if(adjacent.vacant()){ adjacent.vacantToFallen(Team.get(14)); } }

		return true;
	}

	public int countTeams() {
		return teamData.size; }

	public Boolean isFallen(String uuid) {
		PlayerTeam p = uuidToData(uuid);
		return p.fallen; }

	public void playerInitTeam(Player player, Boolean fallen) {
		Log.info("player init team " + player.name + " to fallen? " + fallen);
		PlayerTeam ft = teamData.first();
		PlayerTeam pt = playerToData(player);

		if(pt == null) { Log.info("error: playerInitTeam player uuid not in data"); return; }

		if(fallen) {
		    pt.fallTime = System.currentTimeMillis() / 1000L;
			pt.member = ft;
            pt.fallen = true;
            pt.players = 0;
			pt.oneMinute = true;
            ft.players++; }

		player.clearUnit();
		player.team(pt.member.team);	

		if(ft.team.cores().size == 0) {
			Seq<Parcel> copy = parcels.copy();
            copy.retainAll(h -> h.vacant());
            if(copy.size > 0) {
                copy.shuffle();
                copy.first().vacantToFallen(ft.team); } }

		//debugTeamDataDump();
	}

	public String getClaim(String uuid) {
		PlayerTeam pt = uuidToData(uuid);
		
		if(!pt.fallen) return "You are still alive.";

		for(PlayerTeam member : teamData) {
			if(member.victims.contains(uuid)) return "You are currently claimed by " + member.name + "[white]."; }

		return "You are not claimed by another team.";
	}

	public Boolean isBeforeTenMinutes() {
		long gameCurrent = System.currentTimeMillis() / 1000L;
		long diff = gameCurrent - gameStart;
		if(diff < 600) return true;
		return false;
	}

	public Boolean isSlave(String uuid) {
		PlayerTeam pt = uuidToData(uuid);

		if(pt.member != pt && pt.member.team.id != 14) return true;
		return false;
	}

	public Boolean isAlive(String uuid) {
		PlayerTeam pt = uuidToData(uuid);
		if(pt.fallen) return false;
		if(pt.member != pt) return false;
		return true;
	}

	public Boolean isClaimed(String uuid) {
		PlayerTeam pt = uuidToData(uuid);

		if(!pt.fallen) return false;
		
		for(PlayerTeam member : teamData) {
                        if(member.victims.contains(uuid)) return true; }
		return false;
	}

	public void buildSuitorList(String uuid) {
		PlayerTeam pt = uuidToData(uuid);

		Log.info("Build suitor list for " + pt.name);

		if(pt.suitors == null) { pt.suitors = new Seq<>(); }
		pt.suitors.clear();

		for(Player p : Groups.player) {
			PlayerTeam st = uuidToData(p.uuid());	
			Log.info("Check against player " + st.name);
			if(isAlive(p.uuid())) { 
				Log.info("They are alive, add to list");
				pt.suitors.add(p.uuid()); }
		}
	}
	
	public String getSuitorListString(String uuid) {
		PlayerTeam pt = uuidToData(uuid);

		if(pt.suitors == null) return "No joinable team list available";

		int counter = 1;
        String output = "";

        if(pt.suitors.size == 0) { output = output + "No active teams with leader present."; }
        else {
            output = output + "Use /invite again but add the number before a player in this list to request to join their team.\n";
            for(String suitUuid : pt.suitors) {
			    PlayerTeam st = uuidToData(suitUuid);
                output = output + " (" + counter + ") " + st.name + "[white]\n";
                counter++; } }
        return output;  	
	}

	public Boolean requestJoin(String uuid, int cardinal) {
		PlayerTeam pt = uuidToData(uuid);
		if(pt.suitors == null) { buildSuitorList(uuid); }	
		
        if(!pt.member.uuid.equals("")) { return false; }
		if(isClaimed(uuid)) { return false; }
        String v = "";
        try { v = pt.suitors.get(cardinal - 1); }
        catch (Exception e) { return false; }

		PlayerTeam st = uuidToData(v);
		st.victims.add(uuid);
		
		for(Player p : Groups.player) {
			if(p.uuid().equals(v)) {
				p.sendMessage(pt.name + "[white] has requested to join your team. Use /invite to add them to your team."); } }

        return true;
	}

	private Seq<PlayerTeam> getMemberTeams(String uuid) {
		Seq<PlayerTeam> output = new Seq<>();
		PlayerTeam pt = uuidToData(uuid);

		for(PlayerTeam member : teamData) {
			if(member.member == pt) { output.add(member); } }		

		return output;
	}

	private Seq<String> getVictimList(String loser) {
		Seq<String> l = new Seq<>();

		PlayerTeam lt = uuidToData(loser);

		l.add(loser);
		l.addAll(lt.victims);
		
		for(PlayerTeam loserMember : getMemberTeams(loser)) {
            if(loserMember.uuid.equals(loser)) { continue; }
			l.add(loserMember.uuid); }

		return l;
	}

	public Boolean toggleFullAssault(String t) {
		PlayerTeam pt = uuidToData(t).member;

		if(pt.fullAssault) {
			pt.fullAssault = false; }
		else {
			pt.fullAssault = true; }
		return pt.fullAssault;
	}

	public Seq<Team> getFullAssaultTeams() {
		Seq<Team> output = new Seq<>();

		for(PlayerTeam pt : teamData) {
			if(pt.fullAssault) {
				output.add(pt.team); } } 

		return output;
	}

	public void setFullAssault() {
		for(PlayerTeam pt: teamData) {
			pt.fullAssault = true; }
	}

	public void uuidKillsUuid(String winner, String loser) {	
		Log.info("uuid kills uuid " + loser + " loses, winner " + winner);

		if(loser.equals("")) { return; }

		PlayerTeam lt = uuidToData(loser);
		PlayerTeam wt = uuidToData(winner);
		PlayerTeam ft = teamData.first();

		if(lt.fallTime == 0) lt.fallTime = System.currentTimeMillis() / 1000L;
		lt.member = ft;	
		lt.fallen = true;
		if(lt.score < 1) lt.oneMinute = true;
		ft.players += lt.players;
		lt.players = 0;
		
		if(!winner.equals("")) { wt.victims.addAll(getVictimList(loser)); }
		lt.victims = lt.victims.clear();
		
		for(Player p : Groups.player) {
			if(p.team().id == lt.team.id) {
				p.clearUnit();
                p.team(lt.member.team); } }

		//find and adjust any other members of this team
		for(PlayerTeam loserMember : getMemberTeams(loser)) {
			if(loserMember.uuid.equals(loser)) { continue; }
			if(loserMember.fallTime == 0) loserMember.fallTime = System.currentTimeMillis() / 1000L;
			loserMember.member = teamData.first();
			loserMember.fallen = true;
			loserMember.players = 0;
			loserMember.victims = loserMember.victims.clear(); }

		//check for nucleus
		if(ft.nucleus == false) { spawnNucleus(ft.uuid); }

		//debugTeamDataDump();
	} 

	public String uuidInviteString(String playerUuid) {
		int counter = 1;
		String output = "";
		PlayerTeam pt = uuidToData(playerUuid);

		if(pt.victims.size == 0) { output = output + "No players to invite. Go kill someone first."; }
		else {
			output = output + "Use /invite again but add the number before a player in this list to get them on your team.\n";
			for(String uuid : pt.victims) {
				for(PlayerTeam vt : teamData) {
					if(vt.uuid.equals(uuid)) {
						output = output + " (" + counter + ") " + vt.name + "[]\n";	
						counter++;
						break; } } } }
		return output;
	}
 
	public void uuidInviteUuid(String king, String peon) {
		Log.info("player invite player " + king + " invites " + peon);

		PlayerTeam kt = uuidToData(king);
		PlayerTeam pp = uuidToData(peon);	
		PlayerTeam ft = teamData.first();

		kt.players++;
		kt.victims.remove(peon);
		pp.member = kt;
		ft.players--;
		pp.fallTime = 0;

		for(Player p : Groups.player) {
			if(p.uuid().equals(peon)) {
				p.clearUnit();
        	    p.team(pp.member.team); } }
	
		//check for nucleus
        if(kt.nucleus == false) { spawnNucleus(kt.uuid); }
	}

	public Boolean inviteByCardinal(String uuid, int cardinal) {
		PlayerTeam pt = uuidToData(uuid);
		if(pt.member != pt) { return false; }
		String v = "";
		try { v = pt.victims.get(cardinal - 1); }
		catch (Exception e) { return false; }
		uuidInviteUuid(pt.uuid, v);
		return true;
	}

	public void initParcels() {
	    for(int x = 0; x < ec.hexLayers * 2 + 1; x++) {
            for(int y = 0; y < ec.hexLayers * 2 + 1; y++) { 
				for (int z = 0; z < ec.hexLayers * 2 + 1; z++) {
                    Parcel parcel = new Parcel(x - ec.hexLayers, y - ec.hexLayers, z - ec.hexLayers, ec);
				    parcel.status = Parcel.vacant;
                    parcels.add(parcel);
                    parcelIndex[x][y][z] = parcel;
                }
            }
        }

        
	    for(int x = 0; x < ec.hexLayers * 2 + 1; x++) {
            for(int y = 0; y < ec.hexLayers * 2 + 1; y++) { 
				for (int z = 0; z < ec.hexLayers * 2 + 1; z++) {
				    Parcel parcel = parcelIndex[x][y][z];
				    if(parcel == null || parcel.inactive()) continue;
					
					if (x > 0) {
						Parcel east = parcelIndex[x - 1][y][z];
						if (east != null && !east.inactive())
							Parcel.pairAdjacent(parcel, east);
					}
					if (y > 0) {
						Parcel northeast = parcelIndex[x][y - 1][z];
						if (northeast != null && !northeast.inactive())
							Parcel.pairAdjacent(parcel, northeast);
					}
					if (z > 0) {
						Parcel northwest = parcelIndex[x][y][z - 1];
						if (northwest != null && !northwest.inactive())
							Parcel.pairAdjacent(parcel, northwest);
					}
				}
			}
		}

        //TODO separate this out for defined vs proc gen maps
		for(Parcel p : parcels) { p.rollBorders(); }
	}

	public static String timeString(long ut) {
		String out = "";
		long currentTime = System.currentTimeMillis() / 1000L;
		long delta = currentTime - ut;
		long seconds = delta % 60L;
		delta = (delta - seconds) / 60L;
		long minutes = delta % 60L;
		long hours = (delta - minutes) / 60L;
		if(hours > 0) { out = out + String.valueOf(hours) + " hours"; }
		if(minutes > 0) { out = out + " " + String.valueOf(minutes) + " minutes"; } 
        out = out + " " + String.valueOf(seconds) + " seconds ";
		return out;
	}

	public static String timeToEndString(long ut) {
		String out = "";
        long currentTime = System.currentTimeMillis() / 1000L;
        long delta = currentTime - ut;
		delta = gameLength - delta > 0 ? gameLength - delta : 0; 
        long seconds = delta % 60L;
        delta = (delta - seconds) / 60L;
        long minutes = delta % 60L;
        long hours = (delta - minutes) / 60L;
        if(hours > 0) { out = out + String.valueOf(hours) + " hours"; }
        if(minutes > 0) { out = out + " " + String.valueOf(minutes) + " minutes"; }
        out = out + " " + String.valueOf(seconds) + " seconds ";
        return out;
	}

	public String welcomeString() {
		String out = "Off world reinforcments will arrive in " + timeToEndString(gameStart);
		out = out + "\nCapture as many cores as you can before they arrive.";		
		return out;
	}

	public void setGameTier() {
		Log.info("setting game tier");
		gameTier = 1;
		peerUuid.clear();
		for(int i = 0; i < participants.length; i++) { participants[i] = 0; }

		//find participant uuids
		for(PlayerTeam pt : teamData) {
			Log.info("checking team " + pt.name);
			if(!isAlive(pt.uuid)) { continue; }	
			if(pt.score < 1) { continue; }
			Log.info("participating at tier " + pt.tier);
			participants[pt.tier]++;
			peerUuid.add(pt.uuid); } 

		//find highest tier at or below 2 players
		int sum = 0;
		for(int i = participants.length - 1; i >= 1; i--) {
			if(sum + participants[i] > 1) {
				gameTier = i;
				break;	}
			sum += participants[i]; 
		}
		Log.info("setting game tier to " + gameTier);
	}

	public String getParticipantsString() {
		if(gameTier == 0) { return "The tier of this game will be determined at ten minutes.\n"; }

		String output = "This is a ";
		if(gameTier == 1 ) { output = output + "Scrap"; }
		if(gameTier == 2 ) { output = output + "Copper"; }
		if(gameTier == 3 ) { output = output + "Lead"; }
		if(gameTier == 4 ) { output = output + "Titanium"; }
		if(gameTier == 5 ) { output = output + "Plastanium"; }
		if(gameTier == 6 ) { output = output + "Surge"; }
		output = output + " tier game.\n";

		if(peerUuid.isEmpty()) { output = output + "No competitors.\n"; }
		else { output = output + "Competing players are:\n"; }
		for(String s : peerUuid) {
			output = output + uuidToData(s).name + "[white]\n";
		}
		return output;
	}

	public Seq<Parcel> parcels(){
		return parcels;
	}

	public Parcel getParcelNearPos(int mapx, int mapy){
		for(Parcel parcel : parcels){
			float diff_x = parcel.center_x - mapx;
			float diff_y = parcel.center_y - mapy;
			if(diff_x * diff_x + diff_y * diff_y < ec.parcelRsquare) return parcel;			
		}
		return null;
	}

	public String fallenVictoryCard(String uuid) {
        String output = "Game over. Fallen victory.\nResults:\n";

		//every player still connected except the final living player gets 1 prestige
        for(Player p : Groups.player) {
		    if(uuid.equals(p.uuid())) { continue; }
            PlayerTeam pt = uuidToData(p.uuid());
			if(pt.tier < 2) { continue; }
            output = output + p.name + " +1 prestige\n"; }
        return output;
    }

	public void fallenVictoryRecord(String uuid) {
		try {
            debugTeamDataDump();
            FileWriter fw;
			if(state.rules.env == Planets.serpulo.defaultEnv) { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_v7/update.csv", false); }
			else { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_erekir/update.csv", false); }
            BufferedWriter bw = new BufferedWriter(fw);
            for(Player p : Groups.player) {
                if(uuid.equals(p.uuid())) { continue; }
                PlayerTeam pt = uuidToData(p.uuid());
              	if(pt.tier < 2) { continue; }
                String output = "";
                output += "\"" + pt.uuid + "\"";
				
                output += ",\"" + pt.name.substring(6) + "\"";
                output += ",\"0\"";
                output += ",\"1\"";
                bw.write(output);
                bw.newLine(); }
            bw.close();
            } catch (Exception e) {
                Log.info("Exception failed to write fallen game results. " + e.getMessage()); }
	}

	public void playerVictoryRecord(String uuid) {
	
		PlayerTeam pt = uuidToData(uuid);
                try {
			String output = "";
                        debugTeamDataDump();
                        FileWriter fw;
			if(state.rules.env == Planets.serpulo.defaultEnv) { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_v7/update.csv", false); }
			else { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_erekir/update.csv", false); }
                        BufferedWriter bw = new BufferedWriter(fw);

			//first win upgrades from scrap to copper
                	if(pt.tier < 2) {
				output = "\"" + pt.uuid + "\"";
                                output += ",\"" + pt.name.substring(6) + "\"";
                                output += ",\"20\"";
                                output += ",\"0\"";
				bw.write(output);
                        	bw.newLine();
                	//if this is a peer or outmatched tier game, winner gets progress to next tier
                	} else if(pt.tier <= gameTier) {
                        	int peers = 0;
                        	for(int i = 0; i < participants.length; i++) { if(i >= pt.tier) { peers += participants[i]; } }
				if(pt.tier == gameTier) { peers -= 1; }
				output = "\"" + pt.uuid + "\"";
                                output += ",\"" + pt.name.substring(6) + "\"";
                                output += ",\"" + peers + "\"";
                                output += ",\"0\"";
                                bw.write(output);
                                bw.newLine();
                	//then the player has won a mentor game which is worth 1 prestige
                	} else {
				output = "\"" + pt.uuid + "\"";
                                output += ",\"" + pt.name.substring(6) + "\"";
                                output += ",\"" + 0 + "\"";
                                output += ",\"1\"";
                                bw.write(output);
                                bw.newLine(); }
			
			//1 prestige for any mentor participants
                	for(String peer : peerUuid) {
                	        PlayerTeam mentor = uuidToData(peer);
				if(mentor == null) { continue; }
                	        if(mentor.tier <= gameTier) { continue; }
				output = "\"" + mentor.uuid + "\"";
                                output += ",\"" + mentor.name.substring(6) + "\"";
                                output += ",\"" + 0 + "\"";
                                output += ",\"1\"";
                                bw.write(output);
                                bw.newLine(); }
	
        	        //1 prestige for any connected assist players
        	        for(Player p: Groups.player) {
        	                PlayerTeam lt = uuidToData(p.uuid());
				if(lt == null) { continue; }
        	                if(lt.tier < 2) { continue; }
				if(lt.member == lt) { continue; }
        	                if(lt.member == pt) {
					output = "\"" + lt.uuid + "\"";
                                	output += ",\"" + lt.name.substring(6) + "\"";
                                	output += ",\"" + 0 + "\"";
                                	output += ",\"1\"";
                                	bw.write(output);
                                	bw.newLine(); } }
 
                        bw.close();
                } catch (Exception e) {
                        Log.info("Exception failed to write fallen game results. " + e.getMessage()); }
        }


	public String playerVictoryCard(String uuid) {
		PlayerTeam pt = uuidToData(uuid);
		String output = "Game over. Victory for " + pt.name + ".\nResults:\n";
		
		//first win upgrades from scrap to copper
		if(pt.tier < 2) {
			output = output + pt.name + " first win. Ranking unlocked.\n";
		//if this is a peer or outmatched tier game, winner gets progress to next tier
		} else if(pt.tier <= gameTier) {
			int peers = 0;
			for(int i = 0; i < participants.length; i++) { if(i >= pt.tier) { peers += participants[i]; } }
			if(pt.tier == gameTier) { peers -= 1; }
			output = output + pt.name + " +" + (peers * 5) + "% progress to next tier.\n";
		//then the player has won a mentor game which is worth 1 prestige
		} else {
			output = output + pt.name + " +1 prestige for mentor victory.\n";
		}

		//1 prestige for any mentor participants
		for(String peer : peerUuid) {
			PlayerTeam mentor = uuidToData(peer);
			if(mentor == null) { continue; }
			if(mentor.tier <= gameTier) { continue; }
			if(mentor.tier < 2) { continue; }
			output = output + mentor.name + " +1 prestige for mentor participation\n";
		}

		//1 prestige for any connected assist players
		for(Player p: Groups.player) {
			PlayerTeam lt = uuidToData(p.uuid());
			if(lt == null) { continue; }
			if(lt.tier < 2) { continue; }
			if(lt.member == lt) { continue; }
			if(lt.member == pt) {
				output = output + lt.name + " +1 prestige for invite victory assist.\n";
			}
		}

		return output;
	}

	public void writeGameResults(String uuidWin) {
		try {
 			debugTeamDataDump();
			FileWriter fw;
			if(state.rules.env == Planets.serpulo.defaultEnv) { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_v7/results.csv", true); }
			else { fw = new FileWriter(System.getProperty("user.home") + "/GameRecords_erekir/results.csv", true); }
    		BufferedWriter bw = new BufferedWriter(fw);
			for(PlayerTeam pt : teamData) {
			    float bookend = pt.fallTime;
				if(bookend == 0) bookend = System.currentTimeMillis() / 1000L;
				String output = "";
				output += "\"" + pt.uuid + "\"";
				output += ",\"" + pt.name + "\"";
				output += ",\"" + pt.score + "\"";
				double minutes = (bookend - pt.joinTime) / 60; 
				if(minutes < 1) minutes = 1;
				if(pt.score < 1) minutes = 1;
				if(pt.oneMinute) minutes = 1;
				output += ",\"" + minutes + "\"";
				if(pt.uuid.equals(uuidWin)) {
					output += ",\"1\"";
				} else {
					output += ",\"0\"";
				}
    			bw.write(output);
    			bw.newLine();	
			}	
    		bw.close();
		} catch (Exception e) {
			Log.info("Exception failed to write game results. " + e.getMessage()); }
	}

	public static class RestartEvent { }
}

