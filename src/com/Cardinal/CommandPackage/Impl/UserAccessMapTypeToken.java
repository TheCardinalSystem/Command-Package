package com.Cardinal.CommandPackage.Impl;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

public class UserAccessMapTypeToken extends TypeToken<Map<Long, List<String>>> {
	public UserAccessMapTypeToken() {
		super();
	}
}
