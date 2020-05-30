package com.Cardinal.CommandPackage.Command.Category;

import com.Cardinal.CommandPackage.Impl.CommandClient;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

/**
 * A nice little collection of default command categories.
 * 
 * @author Cardinal System
 *
 */
public class DefaultCategories {

	public static final ICategory DEVELOPER = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			return CommandClient.DEVELOPER_IDS.contains(member.getUser().getId());
		}

		@Override
		public int getLevel() {
			return 12;
		}

		@Override
		public String getName() {
			return "developer";
		}
	}, MANAGEMENT = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			return member.hasPermission(Permission.getPermissions(2013274160))
					|| member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
		}

		@Override
		public int getLevel() {
			return 1;
		}

		@Override
		public String getName() {
			return "management";
		}

	}, MODERATION = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			// Checks for the permissions KICK_MEMBERS, BAN_MEMBERS, MESSAGE_MANAGE
			return member.hasPermission(Permission.getPermissions(8198))
					|| member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
		}

		@Override
		public int getLevel() {
			return 2;
		}

		@Override
		public String getName() {
			return "moderation";
		}

	}, INFORMATION = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			return member.hasPermission(Permission.getPermissions(1024));
		}

		@Override
		public int getLevel() {
			return 8;
		}

		@Override
		public String getName() {
			return "information";
		}

	}, MEMBER = new ICategory() {

		@Override
		public String getName() {
			return "member";
		}

		@Override
		public boolean canAccess(Member member) {
			return true;
		}

		@Override
		public int getLevel() {
			return 10;
		}

	}, FUN = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			return true;
		}

		@Override
		public int getLevel() {
			return 11;
		}

		@Override
		public String getName() {
			return "fun";
		}

	}, SUPPORT = new ICategory() {

		@Override
		public boolean canAccess(Member member) {
			return true;
		}

		@Override
		public int getLevel() {
			return 13;
		}

		@Override
		public String getName() {
			return "support";
		}

	};

}
