package extermination;

import arc.struct.*;
import arc.util.*;
import arc.*;

import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.entities.*;
import arc.struct.ObjectMap.*;
import mindustry.content.*;

import mindustry.core.NetServer.*;
import mindustry.game.Schematic.*;

import java.util.*;

import static mindustry.Vars.*;

public class Parcel{
	
    private EvictConfig ec;

	//public final static int h_spacing = 74;
	//public final static int v_spacing = 64;

	public final static int inactive = 0;
	public final static int   vacant = 1;
	public final static int   fallen = 2;
	public final static int  capital = 3;
	public final static int province = 4;

	public final static int opening = 0;
	public final static int doorway = 1;
	public final static int fence   = 2;
	public final static int barrier = 3;

	public int x, y, z;
	public float center_x, center_y;
    public Seq<Parcel> adjacents = new Seq<>();
	public int status;
	public @Nullable Team controller;

	public int northwest;
	public int northeast;
	public int west;

	public Parcel(int x, int y, int z, EvictConfig ec){
        this.ec = ec;
		this.x = x;
		this.y = y;
        this.z = z;
		center_x = this.mapx();
		center_y = this.mapy();
		status = inactive;
		controller = null;
		northwest = opening;
		northeast = opening;
		west = opening;
	}

	public void rollBorders()
	{
        double o = ec.borderOpenRoll;
        double d = ec.borderDoorRoll;
        double f = ec.borderFenceRoll;
        double w = ec.borderWallRoll;

		double total = o + d + f + w;
		if(total <= 0) { total = 1; o = 1; d = 0; f = 0; w = 0; }

		o = o / total;
		d = d / total;
		f = f / total;
		w = w / total;		

		if(ec.rand.chance(o)) { west = opening; }
		else if(ec.rand.chance(d / (1.0 - o))) { west = doorway; }	
		else if(ec.rand.chance(f / (1.0 - o - d))) { west = fence; }	
		else { west = barrier; }	

		if(ec.rand.chance(o)) { northwest = opening; }
		else if(ec.rand.chance(d / (1.0 - o))) { northwest = doorway; }	
		else if(ec.rand.chance(f / (1.0 - o - d))) { northwest = fence; }	
		else { northwest = barrier; }	

		if(ec.rand.chance(o)) { northeast = opening; }
		else if(ec.rand.chance(d / (1.0 - o))) { northeast = doorway; }	
		else if(ec.rand.chance(f / (1.0 - o - d))) { northeast = fence; }	
		else { northeast = barrier; }	
	}

	public boolean isWestActive() {
        for(Parcel p : adjacents) {
		    if(p.x == x - 1 && p.y == y && p.z == z) { return p.active(); } }	
		return false;
	}

	public boolean isNorthWestActive() {
		for(Parcel p : adjacents) {
			if(p.x == x && p.y == y - 1 && p.z == z) { return p.active(); } }
		return false;
	}

	public boolean isNorthEastActive() {
        for(Parcel p : adjacents) {
            if(p.x == x && p.y == y && p.z == z - 1) { return p.active(); } }
                return false;
    }

	public boolean active() {
		return status != inactive;
	}

	public boolean inactive() {
        return status == inactive;
    }

	public boolean vacant() {
		return status == vacant;
	}

	public static void pairAdjacent(Parcel n1, Parcel n2){
		n1.adjacents.add(n2);
		n2.adjacents.add(n1);
	}

	public int mapx(){
        return ec.mapTileCenter + x * ec.parcelHorizOffset + y * ec.parcelVertOffsetX - z * ec.parcelVertOffsetX;
	}

	public int mapy(){
        return ec.mapTileCenter + y * ec.parcelVertOffsetY + z * ec.parcelVertOffsetY;
	}
	
	public void vacantToFallen(Team fallenTeam){
		removeUnits();	
		removeBuildings();
		
		if(ec.planet == ec.serpulo) 
		{
			Stile coreTile = ec.fallenShard.tiles.find(s -> s.block instanceof CoreBlock);
        		if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        		int ox = Math.round(center_x) - coreTile.x, oy = Math.round(center_y) - coreTile.y;
        		ec.fallenShard.tiles.each(st -> {
            		Tile tile = world.tile(st.x + ox, st.y + oy);
            		if(tile == null) return;
		
        			if(tile.block() != Blocks.air) { tile.removeNet(); } 
 	        	   	tile.setNet(st.block, fallenTeam, st.rotation);
 
            		if(st.config != null){ tile.build.configureAny(st.config); }
        		});
		} else { //erekir
			int ox = Math.round(center_x), oy = Math.round(center_y);
			Tile tile = world.tile(ox, oy);
			tile.setNet(Blocks.coreBastion, fallenTeam, 0);
		}

		status = fallen;
	}

	public void fail() {
		if(status == inactive) { return; }
		if(status == fallen) { return; }
		removeUnits();
		for(int rx = -1 * ec.parcelRadius; rx < ec.parcelRadius; rx++) {
            for(int ry = -1 * ec.parcelRadius; ry < ec.parcelRadius; ry++) {
				if(rx * rx + ry * ry > ec.parcelRsquare) { continue; }
                Tile tile = world.tile(Math.round(center_x) + rx, Math.round(center_y) + ry);
				if(tile.block() instanceof CoreBlock) {
				    if(tile.build != null) { Time.run(1f, tile.build::kill); }
				}
            }
		}
	}

	public void removeBuildings()
    {
		for(int rx = -1 * ec.parcelRadius - 12; rx < ec.parcelRadius + 12; rx++)
        {
            for(int ry = -1 * ec.parcelRadius - 12; ry < ec.parcelRadius + 12; ry++)
            {
                //if(rx * rx + ry * ry > r_square_10) { continue; }
                Tile tile = world.tile(Math.round(center_x) + rx, Math.round(center_y) + ry);
				if(tile == null) { continue; }				
                if(rx * rx + ry * ry < ec.parcelRsquare) 
                { 
					if(tile.block() instanceof CoreBlock) { continue; }
					if(Math.abs(rx) < 2 && Math.abs(ry) < 2) { continue; }
                    if(tile.build != null) { Time.run(1f, tile.build::kill); }
				} else {	
                    if(tile.build != null && (tile.build.block.name.equals(Blocks.afflict.name) || tile.build.block.name.equals(Blocks.titan.name))) { Time.run(1f, tile.build::kill); }
				}
            }
        }
	}

    //TODO configurable remove radius
	public void removeUnits() {
		Units.nearby(8*(center_x-ec.parcelRadius), 8*(center_y-ec.parcelRadius), 8*ec.parcelDiameter, 8*ec.parcelDiameter, unit -> {
            if(unit.isPlayer()) return;
            float xs = unit.x-center_x*8;
            float ys = unit.y-center_y*8;
            //Log.info(unit.x + unit.y + center_x*8 + center_y*8);
            if((xs*xs)+(ys*ys) < ec.parcelRsquare * 64) {
                Core.app.post(() -> Call.unitDestroy(unit.id)); }
        });
	}

	public Team countTeamUnits(int notThisTeam){
		Team maxTeam = null;
		int maxUnits = 0;
		ObjectMap<Team, Integer> teamCount = new ObjectMap<>();		
        //Log.info("count team x " + center_x + " y " + center_y + " radius " + radius + " ec.parcelDiameter " + ec.parcelDiameter);
		Units.nearby(8*(center_x-ec.parcelRadius), 8*(center_y-ec.parcelRadius), 8*ec.parcelDiameter, 8*ec.parcelDiameter, unit -> {
            if(unit.isPlayer()) return;
			float xs = unit.x-center_x*8;
			float ys = unit.y-center_y*8;
			//Log.info(unit.x + unit.y + center_x*8 + center_y*8);
			if((xs*xs)+(ys*ys) < ec.parcelRsquare * 64) {
				int c = teamCount.get(unit.team, 0);
				teamCount.put(unit.team, c+1); 	
				if(ec.rand.chance(ec.unitSurvival(unit))) { Core.app.post(() -> Call.unitDestroy(unit.id)); }
			}
        });

		//Log.info("count team units, not this team " + notThisTeam);
		for(Entry<Team, Integer> entry : teamCount.entries()) {
			//Log.info("units " + entry.value + " team " + entry.key);
			if(entry.key.id == notThisTeam) continue;
			if(entry.value > maxUnits) {
				maxUnits = entry.value;
				maxTeam = entry.key; } }

		return maxTeam;
	}
}
