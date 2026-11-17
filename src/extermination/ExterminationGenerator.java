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

public class ExterminationGenerator implements Cons<Tiles>{

    private EvictConfig ec;
	private GameData data;

	public ExterminationGenerator(GameData data){
        this.ec = data.ec;
		this.data = data;
	}

	@Override
	public void get(Tiles tiles){

		for(int x = 0; x < ec.mapSizeTiles; x++){
			for(int y = 0; y < ec.mapSizeTiles; y++){
				tiles.set(x, y, new Tile(x, y, ec.mapFloorID, ec.mapOverlayID, ec.mapWallID)); } }

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
					drawNorthEastBorder(tiles, parcel); } } }	

        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);
        ores.each(o -> ((OreFilter)o).threshold -= 0.05f);
        ores.insert(0, new OreFilter() { {
            ore = Blocks.oreScrap;
            scl += 2 / 2.1F; } } );

		ores.insert(0, new LiquidFilter() { {
			set(20f, 0.89f, 2f, 0.5f, Blocks.darksandWater, Blocks.air, Blocks.darksand); } } );
		ores.insert(0, new LiquidFilter() { {
			set(20f, 0.925f, 2f, 0.5f, Blocks.tar, Blocks.air, Blocks.darksand); } } );

		ores.each(GenerateFilter::randomize);
        GenerateInput in = new GenerateInput();

        for(int x = 0; x < ec.mapSizeTiles; x++) {
            for(int y = 0; y < ec.mapSizeTiles; y++) {
				Tile tile = tiles.get(x, y);
				if(tile.floorID() != Blocks.darksand.id) continue;				
				
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
                                        floor = in.floor; 
                                }

                                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
                        }
                }

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

                                if(x < 3 && x > -3 && y < 3 && y > -3)
                                {
                                      	floor = Blocks.snow;
                                       	overlay = Blocks.air;
					wall = Blocks.air;
                                }

                              	tiles.set(pos_x + x, pos_y + y, new Tile(pos_x + x, pos_y + y, floor.id, overlay.id, wall.id));
                     	}
             	}
	}

        // TODO: Remove code copy-pasting
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
				Block wall = Blocks.dirtWall;
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
                        for (int y = ystart; y < ystart + yheight; y++) {
				int slanty = y + (int) Math.round((x + ec.parcelRadius) / 1.7) - 3;
                                if(parcel.northwest == Parcel.doorway && x > (int) Math.round(-1 * ec.parcelRadius / 2.0) - 3 && x < (int) Math.round(-1 * ec.parcelRadius / 2.0) + 4) { continue; }
                                Tile tile = tiles.get(mapx + x, mapy + slanty);
                                Block wall = Blocks.dirtWall;
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
                        for (int y = ystart; y < ystart + yheight; y++) {
                                int slanty = y + (int) Math.round((ec.parcelRadius - x) / 1.7) - 3;
                                if(parcel.northeast == Parcel.doorway && x < (int) Math.round(ec.parcelRadius / 2.0) + 3 && x > (int) Math.round(ec.parcelRadius / 2.0) - 4) { continue; }
                                Tile tile = tiles.get(mapx + x, mapy + slanty);
                                Block wall = Blocks.dirtWall;
                                Block floor = tile.floor();
                                Block overlay = tile.overlay();

                                tiles.set(mapx + x, mapy + slanty, new Tile(mapx + x, mapy + slanty, floor.id, overlay.id, wall.id));
                        }
                }
        }

}
	
