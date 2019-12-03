package com.Cardinal.CommandPackage.Util;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class ColorUtils {

	public static Color getRandomColor() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
	}
	
}
