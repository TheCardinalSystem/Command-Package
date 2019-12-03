package com.Cardinal.CommandPackage.Command;

public class ArgumentTypes {

	public static ArgumentTypes BOOLEAN = new ArgumentTypes("BOOLEAN"), INTEGER = new ArgumentTypes("INTEGER"),
			LONG = new ArgumentTypes("LONG"), CHARACTER = new ArgumentTypes("CHARACTER"),
			FLOAT = new ArgumentTypes("FLOAT"), DOUBLE = new ArgumentTypes("DOUBLE"),
			STRING = new ArgumentTypes("STRING"), STRING_ARRAY = new ArgumentTypes("STRING_ARRAY"),
			INT_ARRAY = new ArgumentTypes("INTEGER_ARRAY"), USER_MENTION = new ArgumentTypes("USER_MENTION"),
			CHANNEL_MENTION = new ArgumentTypes("CHANNEL_MENTION"), URL = new ArgumentTypes("URL"),
			/**
			 * If you want your command to process its own arguments, use this for
			 * {@link ICommand#getArgumentTypes()}. A string array will be passed to
			 * {@linkplain ICommand#execute(MessageReceivedEvent, CommandRegistry, String, Object...)},
			 * and non of the arguments will run through the default processor.
			 */
			CUSTOM;

	private String s;

	private ArgumentTypes(String name) {
		s = name;
	}

	private ArgumentTypes(String name, boolean optional) {
		this.optional = optional;
		s = name;
	}

	private boolean optional = false;

	public ArgumentTypes optional() {
		return new ArgumentTypes(s, true);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ArgumentTypes ? ((ArgumentTypes) obj).toString().equals(this.toString()) : false;
	}

	public boolean isOptional() {
		return optional;
	}

	@Override
	public String toString() {
		return s;
	}
}
