package com.Cardinal.CommandPackage.Util;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A class with utility methods related to {@linkplain Color}s.
 * 
 * @author Cardinal System
 *
 */
public class ColorUtils {

	/**
	 * Gets a random {@link Color} using {@link ThreadLocalRandom}.
	 * 
	 * @return a random color.
	 */
	public static Color getRandomColor() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
	}

}
