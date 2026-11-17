package extermination;

import static mindustry.Vars.*;
import mindustry.content.*;
import arc.*;
import arc.struct.*;
import arc.util.*;
import java.io.*;

public class Leaderboard {
	private Boolean valid = false;
	
	private ObjectMap<String, Integer> uuidToRank = new ObjectMap<>();
	private ObjectMap<String, String>  uuidToName = new ObjectMap<>();
	private ObjectMap<String, Integer> uuidToTier = new ObjectMap<>();
	private ObjectMap<String, Integer> uuidToPrestige = new ObjectMap<>();
	private ObjectMap<String, Integer> uuidToProgress = new ObjectMap<>();
	private Seq<String> shortReports = new Seq<>();
	private Seq<String> rankReports = new Seq<>();

	int entries = 0;
	
	public Leaderboard() {
		try {
			BufferedReader br;
			Log.info("leaderboard load: " + state.rules.env + " & " + Planets.serpulo.defaultEnv);
			if(state.rules.env == Planets.serpulo.defaultEnv) { Log.info("loading serpulo"); br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/GameRecords_v7/serverInput.data")); }
			else { Log.info("loading erekir"); br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/GameRecords_erekir/serverInput.data")); }
			String line; 
			int counter = 0;
			while((line = br.readLine()) != null) {
				String prefix = "[]|| ";
				String[] parts = line.split("~", 0);
				uuidToRank.put(parts[0], new Integer(counter)); counter++;
				uuidToName.put(parts[0], new String(parts[1]));
				uuidToTier.put(parts[0], new Integer(parts[2]));
				uuidToProgress.put(parts[0], new Integer(parts[3]));
				uuidToPrestige.put(parts[0], new Integer(parts[4]));

				int tier = Integer.parseInt(parts[2]);
                		if(tier == 2) { prefix = "[]|| "; }
                		if(tier == 3) { prefix = "[]|| "; }
                		if(tier == 4) { prefix = "[]|| "; }
                		if(tier == 5) { prefix = "[]|| "; }
                		if(tier == 6) { prefix = "[]|| "; }

				shortReports.add("[white]" + prefix + " " + parts[1]);
				rankReports.add("[white]  " + String.valueOf(counter) + ") " + parts[4] + "   " + prefix + parts[1]);
			}
			valid = true;
			entries = counter;
		} catch (Exception e) {
			Log.info(e.getMessage());
			e.printStackTrace();
		}
	}

	public Boolean loaded() { return valid; }

	public void debugDumpLeaderboard() {
		if(valid) {
			uuidToRank.each((uuid, rank) -> {
				String o = uuid + rankReports.get(rank.intValue());
				Log.info(o);
			});
		} else {
			Log.info("leaderboard not loaded");
		}
	}

	public String topCardString() {
		if(valid) {
			int listRanks = 5;
			if(entries < listRanks)  listRanks = entries;
			String output = "Top " + listRanks + " players:\n\n";
			for(int x = 0; x < listRanks; x++) {
				output = output + shortReports.get(x) + "\n";
			}
			return output;	
		}
		return "Leaderboard not loaded.";
	}

	public String topCommandString() {
		if(valid) {
			int listRanks = 5;
                	if(entries < listRanks) listRanks = entries;
                	String output = "Rank: Prestige: Name:\n";
                	for(int x = 0; x < listRanks; x++) {
                	        output = output + rankReports.get(x) + "\n";
                	}
                	return output;
		}
		return "leaderboard not loaded";
	}

	public String statsFromUuid(String uuid) {
		if(valid) {
			Integer rank = uuidToRank.get(uuid);
			if(rank == null) return "[white]Not yet ranked.";	
			int start = rank.intValue() - 3;
			if(start < 0) start = 0;
			int end = rank.intValue() + 3;
			if(end > entries) end = entries;
                	String output = "[white]Progress to next tier: " + uuidToProgress.get(uuid) + "/ 20\n"; 
			output = output + "[white]Rank: Prestige: Name:\n";
                	for(int x = start; x < end; x++) {
				output = output + "[white]";
				if(x == rank.intValue()) output = output + "[red]";
                	        output = output + rankReports.get(x) + "\n";
                	}
                	return output;
		}
		return "[white]leaderboard not loaded";
	}

	public int tierFromUuid(String uuid) {
		if(valid) {
			Integer tier = uuidToTier.get(uuid);
			if(tier == null) tier = new Integer(1);
			return tier.intValue();
		}
		return 1;
	}
}
