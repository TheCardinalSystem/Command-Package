package com.Cardinal.CommandPackage.Impl;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Updater {

	/**
	 * This constant marks the version of the current command package
	 * implementation.
	 */
	public static final String CURRENT_VERSION = "2.1.2";

	/**
	 * Checks for any available updates for this command package. If an update is
	 * available, this will return its URL. Otherwise, this will return null.
	 * 
	 * @return the update URL, or null.
	 * @throws IOException on error
	 */
	public static String checkForUpdates() throws IOException {
		Document doc = Jsoup.connect("https://github.com/TheCardinalSystem/Command-Package/releases/latest").userAgent(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36")
				.get();
		Elements elements = doc.select(
				"body > div.application-main > div > main > div.container-xl.clearfix.new-discussion-timeline.px-3.px-md-4.px-lg-5 > div > div.border-top > div > div.d-none.d-md-block.flex-wrap.flex-items-center.col-12.col-md-3.col-lg-2.px-md-3.pb-1.pb-md-4.pt-md-4.float-left.text-md-right.v-align-top > ul > li:nth-child(1) > a > span");
		Element element = elements.get(0);

		String version = element.ownText();
		int parsed = Integer.parseInt(version.replaceAll("\\.", ""));
		int currentParsed = Integer.parseInt(CURRENT_VERSION.replaceAll("\\.", ""));
		return parsed > currentParsed ? doc.location() : null;
	}

}
