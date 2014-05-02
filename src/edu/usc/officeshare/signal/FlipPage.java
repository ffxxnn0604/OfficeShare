package edu.usc.officeshare.signal;

import org.alljoyn.bus.annotation.*;

public class FlipPage {
	@Position(0)
	public double velocityX; //signature is d
	
	@Position(1)
	public double velocityY; //signature is d
	
	public FlipPage(){
		velocityX = 0.0;
		velocityY = 0.0;
	}
	
}
