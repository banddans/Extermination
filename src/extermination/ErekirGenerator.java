package extermination;

import arc.func.*;
import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.world.*;
import static mindustry.Vars.*;

public class ErekirGenerator implements Cons<Tiles>{

	private GameData data;
    private EvictConfig ec;
	
	private static Rand rand = new Rand();

	public ErekirGenerator(GameData data){
		this.data = data;
        this.ec = data.ec;
	}

	@Override
	public void get(Tiles tiles){

		for(int x = 0; x < ec.mapSizeTiles; x++){
			for(int y = 0; y < ec.mapSizeTiles; y++){
				tiles.set(x, y, new Tile(x, y, Blocks.crystallineStone.id, Blocks.air.id, Blocks.duneWall.id));
			}
		}

	 	for(Parcel parcel : data.parcels()) {
			if(parcel.active()) {
				int mapx = parcel.mapx();
				int mapy = parcel.mapy();
				drawParcel(tiles, mapx, mapy); } }

	 	for(Parcel parcel : data.parcels()) {
			if(parcel.active()) {         
				if(parcel.isWestActive()) {
					drawWestBorder(tiles, parcel); }
				if(parcel.isNorthWestActive()) {
					drawNorthWestBorder(tiles, parcel); }
				if(parcel.isNorthEastActive()) {
					drawNorthEastBorder(tiles, parcel); }
			}
		}	

		GenerateInput in = new GenerateInput();

                //first generate dune walls
		TerrainFilter tf = new TerrainFilter();
                        tf.scl = 17f;
                        tf.magnitude = 0.69f;
                        tf.threshold = 0.59f;
                        tf.circleScl = 0f;
                        tf.octaves = 1f;
                        tf.falloff = 0.11f;
                        tf.tilt = 0f;
                        tf.block = Blocks.duneWall;
                tf.randomize();
                for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);

                                Block floor = tile.floor();
                                Block wall = tile.block();
                                Block ore = tile.overlay();

                                if(tile.floorID() == Blocks.metalFloor.id) { continue; }

                                in.floor = floor; in.block = wall; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                tf.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, in.block.id)); } }

		//generate slag puddles
		NoiseFilter nf = new NoiseFilter();
			nf.scl = 40f;
                        nf.threshold = 0.85f;
                        nf.octaves = 2f;
                        nf.falloff = 0.5f;
                        nf.tilt = 0f;
			nf.block = Blocks.air;
			nf.floor = Blocks.slag;
			nf.target = Blocks.air;
 			nf.randomize();
                for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);
                                
                                Block floor = tile.floor();
                                Block wall = tile.block();
                                Block ore = tile.overlay();
                                
                                if(tile.floorID() == Blocks.metalFloor.id || tile.blockID() == Blocks.duneWall.id) { continue; }
                                
                                in.floor = floor; in.block = wall; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                nf.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, in.block.id)); } }

		//arkycite puddles
		nf.floor = Blocks.arkyciteFloor;
		nf.randomize();
		for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);

                                Block floor = tile.floor();
                                Block wall = tile.block();
                                Block ore = tile.overlay();

                                if(tile.floorID() == Blocks.metalFloor.id || tile.blockID() == Blocks.duneWall.id) { continue; }

                                in.floor = floor; in.block = wall; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                nf.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, in.block.id)); } }

		//next add wall thorium
		OreFilter of = new OreFilter();
			of.scl = 70f;
                        of.threshold = 0.7f;
                        of.octaves = 2f;
                        of.falloff = 0.6f;
                        of.tilt = 0f;
			of.target = Blocks.crystallineStone;
                        of.ore = Blocks.wallOreThorium;
                of.randomize(); 
		for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);
                                
                                Block floor = tile.floor();
                                Block ore = tile.overlay();
                                
                                if(tile.blockID() != Blocks.duneWall.id) { continue; }
                                
                                in.floor = floor; in.block = Blocks.air; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                of.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, Blocks.duneWall.id)); } }

		//wall Tungsten
		of.ore = Blocks.wallOreTungsten;
		of.randomize();
		for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);

                                Block floor = tile.floor();
                                Block ore = tile.overlay();

                                if(tile.blockID() != Blocks.duneWall.id) { continue; }

                                in.floor = floor; in.block = Blocks.air; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                of.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, Blocks.duneWall.id)); } }
 
		//wall Beryllium
                of.ore = Blocks.wallOreBeryllium;
		of.scl = 30f;
                of.threshold = 0.6f;
                of.randomize();
                for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);

                                Block floor = tile.floor();
                                Block ore = tile.overlay();

                                if(tile.blockID() != Blocks.duneWall.id) { continue; }

                                in.floor = floor; in.block = Blocks.air; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                of.apply(in);
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, Blocks.duneWall.id)); } }

		//first graphite walls
                //TerrainFilter tf = new TerrainFilter();
                        tf.scl = 22.5f;
                        tf.magnitude = 0.72f;
                        tf.threshold = 0.6f;
                        tf.circleScl = 0f;
                        tf.octaves = 3f;
                        tf.falloff = 0.3f;
                        tf.tilt = 0f;
                        tf.block = Blocks.graphiticWall;
                tf.randomize();
                for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
                                Tile tile = tiles.get(x, y);

                                Block floor = tile.floor();
                                Block wall = tile.block();
                                Block ore = tile.overlay();

                                if(tile.floorID() == Blocks.metalFloor.id) { continue; }

                                in.floor = floor; in.block = wall; in.overlay = ore;
                                in.x = x; in.y = y;
                                in.width = in.height = ec.mapSizeTiles;
                                tf.apply(in);
				if(in.block.id == Blocks.graphiticWall.id) { in.overlay = Blocks.air; } 
                                tiles.set(x, y, new Tile(x, y, in.floor.id, in.overlay.id, in.block.id)); } }

		//generate floor ores
		Seq<GenerateFilter> ores = new Seq<>();
                ores.insert(0, new OreFilter(){{
                        ore = Blocks.oreTungsten;
                        scl = 40f;
			threshold = 0.7f;
			octaves = 5f;
			falloff = 0.6f;
                }});
                ores.insert(0, new OreFilter(){{
                        ore = Blocks.oreBeryllium;
                        scl = 55f;
			threshold = 0.75f;
			octaves = 3.5f;
			falloff = 0.6f;
                }});
                ores.insert(0, new OreFilter(){{
                        ore = Blocks.oreThorium;
                        scl = 55f;
			threshold = 0.75f;
			octaves = 3.5f;
			falloff = 0.6f;
                }});
		ores.each(GenerateFilter::randomize);

                for(int x = 0; x < ec.mapSizeTiles; x++) {
                        for(int y = 0; y < ec.mapSizeTiles; y++) {
				Tile tile = tiles.get(x, y);
				if(tile.floorID() != Blocks.crystallineStone.id || tile.blockID() != Blocks.air.id) { continue; }

                                Block floor = tile.floor();
                                Block wall = tile.block();
                                Block ore = Blocks.air;

                                for(GenerateFilter f : ores) {
                                        in.floor = floor;
                                        in.block = wall;
                                        in.overlay = ore;
                                        in.x = x;
                                        in.y = y;
                                        in.width = in.height = ec.mapSizeTiles;
                                        f.apply(in);
                                        if(in.overlay != Blocks.air) { ore = in.overlay; }
                                        floor = in.floor; }
                                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id)); } }

		 for(Parcel parcel : data.parcels()) {
                        if(parcel.active()) {
                                int mapx = parcel.mapx();
                                int mapy = parcel.mapy();
				drawVent(tiles, mapx, mapy);
				drawVent(tiles, mapx, mapy);
				drawVent(tiles, mapx, mapy);
				drawVent(tiles, mapx, mapy); } }

	        state.map = new Map(StringMap.of("name", "Evict"));
	}

	private void drawParcel(Tiles tiles, int pos_x, int pos_y)
	{
             	for(int x = -1 * ec.parcelRadius; x <= ec.parcelRadius; x++){
                        if(pos_x + x < 0 || pos_x + x >= ec.mapSizeTiles) continue;
                        for(int y = -1 * ec.parcelRadius; y <= ec.parcelRadius; y++){
                                if(pos_y + y < 0 || pos_y + y >= ec.mapSizeTiles) continue;
                                if(Mathf.dst2(x, y) > ec.parcelRsquare) continue;

                                Tile tile = tiles.get(pos_x + x, pos_y + y);
                                Block wall = Blocks.air;
                                Block floor = tile.floor();
                                Block overlay = tile.overlay();

                                if(x < 5 && x > -5 && y < 5 && y > -5)
                                {
                                      	floor = Blocks.metalFloor;
                                       	overlay = Blocks.air;
					wall = Blocks.air;
                                }

                              	tiles.set(pos_x + x, pos_y + y, new Tile(pos_x + x, pos_y + y, floor.id, overlay.id, wall.id));
                     	}
             	}
	}

	private void drawVent(Tiles tiles, int pos_x, int pos_y)
        {
		int limit = 1000;
		while(limit-- > 0) {
			int x = rand.random(-1 * ec.parcelRadius, ec.parcelRadius);
			int y = rand.random(-1 * ec.parcelRadius, ec.parcelRadius);
			if(Mathf.dst2(x, y) > (ec.parcelRadius - 4) * (ec.parcelRadius - 4)) continue;

			Tile tile;
			Boolean success = true;
			for(int ox = -2; ox < 3; ox++) {
				for(int oy = -2; oy < 3; oy++) {
					tile = tiles.get(pos_x + ox + x, pos_y + oy + y);
					if(tile.blockID() != Blocks.air.id || tile.floorID() != Blocks.crystallineStone.id || tile.overlayID() != Blocks.air.id) { success = false; continue; } } }

			if(success) {
				for(int ox = -1; ox < 2; ox++) {
                                	for(int oy = -1; oy < 2; oy++) {
                                        	tile = tiles.get(pos_x + ox + x, pos_y + oy + y);
						tile.setFloor(Blocks.crystallineVent.asFloor());
						limit = 0; } } } }
	}


	private void drawWestBorder(Tiles tiles, Parcel parcel)
	{
		if(parcel.west == Parcel.opening) { return; }
		int mapx = parcel.mapx();
		int mapy = parcel.mapy();

		int xstart = -1 * ec.parcelRadius + 2;
		int xwide = 1;
 
		if(parcel.west == Parcel.barrier || parcel.west == Parcel.doorway) {
			xstart = xstart - 2;
			xwide = 5; }

		for(int x = xstart; x < xstart + xwide; x++) {
			for(int y = -1 * ec.parcelRadius + 14; y <= ec.parcelRadius - 14; y++) {
				if(parcel.west == Parcel.doorway && y < 3 && y > - 3) { continue; }
				Tile tile = tiles.get(mapx + x, mapy + y);
				Block wall = Blocks.duneWall;
				Block floor = tile.floor();
				Block overlay = tile.overlay();
			
				tiles.set(mapx + x, mapy + y, new Tile(mapx + x, mapy + y, floor.id, overlay.id, wall.id));
			} 
		}
	}

	private void drawNorthWestBorder(Tiles tiles, Parcel parcel) {
		if(parcel.northwest == Parcel.opening) { return; }
		int mapx = parcel.mapx();
                int mapy = parcel.mapy();

                int ystart = ec.parcelRadius - 16;
                int yheight = 1;

                if(parcel.northwest == Parcel.barrier || parcel.northwest == Parcel.doorway) {
                        ystart = ystart - 2;
                        yheight = 5; }

                for(int x = -1 * ec.parcelRadius + 2; x <= 0; x++) {
                        for(int y = ystart; y < ystart + yheight; y++) {
				int slanty = y + (int) Math.round((x + ec.parcelRadius) / 1.7) - 3;
                                if(parcel.northwest == Parcel.doorway && x > (int) Math.round(-1 * ec.parcelRadius / 2.0) - 3 && x < (int) Math.round(-1 * ec.parcelRadius / 2.0) + 4) { continue; }
                                Tile tile = tiles.get(mapx + x, mapy + slanty);
                                Block wall = Blocks.duneWall;
                                Block floor = tile.floor();
                                Block overlay = tile.overlay();

                                tiles.set(mapx + x, mapy + slanty, new Tile(mapx + x, mapy + slanty, floor.id, overlay.id, wall.id));
                        }
                }
	}

	private void drawNorthEastBorder(Tiles tiles, Parcel parcel) {
                if(parcel.northeast == Parcel.opening) { return; }
                int mapx = parcel.mapx();
                int mapy = parcel.mapy();

                int ystart = ec.parcelRadius - 16;
                int yheight = 1;

                if(parcel.northeast == Parcel.barrier || parcel.northeast == Parcel.doorway) {
                        ystart = ystart - 2;
                        yheight = 5; }

                for(int x = ec.parcelRadius - 2; x >= 0; x--) {
                        for(int y = ystart; y < ystart + yheight; y++) {
                                int slanty = y + (int) Math.round((ec.parcelRadius - x) / 1.7) - 3;
                                if(parcel.northeast == Parcel.doorway && x < (int) Math.round(ec.parcelRadius / 2.0) + 3 && x > (int) Math.round(ec.parcelRadius / 2.0) - 4) { continue; }
                                Tile tile = tiles.get(mapx + x, mapy + slanty);
                                Block wall = Blocks.duneWall;
                                Block floor = tile.floor();
                                Block overlay = tile.overlay();

                                tiles.set(mapx + x, mapy + slanty, new Tile(mapx + x, mapy + slanty, floor.id, overlay.id, wall.id));
                        }
                }
        }

}
	
