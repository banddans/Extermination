package extermination;

import mindustry.maps.filters.*;
import mindustry.content.*;
import mindustry.world.*;

public class LiquidFilter extends GenerateFilter{
    	public float scl = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f, tilt = 0f;
    	public Block floor = Blocks.stone, block = Blocks.stoneWall, target = Blocks.air;

	@Override
    	public FilterOption[] options(){ return null; };
	@Override
	public char icon() { return ' '; };

	public void set(float scl, float threshold, float octaves, float falloff, Block floor, Block block, Block target) {
		this.scl = scl;
		this.threshold = threshold;
		this.octaves = octaves;
		this.falloff = falloff;
		this.floor = floor;
		this.block = block;
		this.target = target; }

	@Override
    	public void apply(GenerateInput in){
        	float noise = noise(in.x, in.y, scl, 1f, octaves, falloff);

        	if(noise > threshold && (target == Blocks.air || in.floor == target || in.block == target)){
            		if(floor != Blocks.air) in.floor = floor;
            		if(block != Blocks.air && in.block != Blocks.air) in.block = block;
        	}
    	}	
}
