package com.Cardinal.CommandPackage.Util;

import com.Cardinal.CommandPackage.Handle.Entity.ListEmbedManager;
import com.Cardinal.CommandPackage.Handle.Event.EventAdapter;

import net.dv8tion.jda.api.entities.Message;

/**
 * This class is used by the {@link EventAdapter} class to handle reaction
 * events.
 * 
 * @author Cardinal System
 * @see ListEmbedManager#changeEmbed(Message, int)
 */
public class ReactionUtils {

	public static int getListEmbedOption(String reactionUnicode) {
		return reactionUnicode.equals("\u23EA") ? 0
				: reactionUnicode.equals("\u2B05") ? 1
						: reactionUnicode.equals("\u27A1") ? 2 : reactionUnicode.equals("\u23E9") ? 3 : 4;
	}

}
