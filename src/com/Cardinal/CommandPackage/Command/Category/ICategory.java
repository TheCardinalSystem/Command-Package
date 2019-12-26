package com.Cardinal.CommandPackage.Command.Category;

import net.dv8tion.jda.core.entities.Member;

/**
 * A template for command categories.
 * 
 * @author Cardinal System
 *
 */
public abstract class ICategory {

	/**
	 * Gets this category's name.
	 * 
	 * @return the category name.
	 */
	public abstract String getName();

	/**
	 * Determines whether the given guild member has access to commands in this
	 * category.
	 * 
	 * 
	 * @param member the guild member.
	 * @return true - the user can access this category.<br>
	 *         false - the user cannot access this category.
	 * @see Member
	 */
	public abstract boolean canAccess(Member member);

	/**
	 * Gets this category's level. The category's level determines where it will
	 * appear in a list of commands. This method is generally used by help commands.
	 * 
	 * @return the level.
	 */
	public abstract int getLevel();

}