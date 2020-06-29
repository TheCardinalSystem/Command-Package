package com.Cardinal.CommandPackage.Entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.Cardinal.CommandPackage.Util.ColorUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

/**
 * I class used to create embed objects in an ordered list/paged fashion. If
 * ever you wish to customize the look of the embed, the builder can be obtained
 * via {@link ListEmbed#getBuilder()}.
 * 
 * @author Cardinal System
 *
 */
public class ListEmbed {

	private String title;
	private List<List<String>> pages;
	private List<List<Field>> fields;
	private List<EmbedBuilder> embeds;
	private boolean flag = false, flag2 = false, rand = false;;
	private LocalTime lastInteraction;

	private EmbedBuilder b = new EmbedBuilder();
	private int index = 0, pageCount;

	/**
	 * Constructs a {@link ListEmbed} object using the using the given
	 * {@linkplain EmbedBuilder}s as individual pages.
	 * 
	 * @param builders the builders.
	 */
	public ListEmbed(EmbedBuilder... builders) {
		embeds = new ArrayList<EmbedBuilder>();
		embeds.addAll(Arrays.asList(builders));
		pageCount = embeds.size();
		flag2 = true;
		for (int i = 0; i < embeds.size(); i++) {
			embeds.get(i).addField("Page", i + 1 + "/" + pageCount, false);
		}
	}

	/**
	 * Constructs a {@link ListEmbed} object using the using the given
	 * {@linkplain EmbedBuilder}s as individual pages.
	 * 
	 * @param builders     the builders.
	 * @param unusedSwitch an unused argument. This exists to avoid ambiguity.
	 */
	public ListEmbed(List<EmbedBuilder> builders, int unusedSwitch) {
		embeds = builders;
		pageCount = embeds.size();
		flag2 = true;
		for (int i = 0; i < embeds.size(); i++) {
			embeds.get(i).addField("Page", i + 1 + "/" + pageCount, false);
		}
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given fields into groups for
	 * pages.
	 * 
	 * @param fields the fields.
	 */
	public ListEmbed(Field... fields) {
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given fields into groups for
	 * pages.
	 * 
	 * @param fields     the fields.
	 * @param unusedFlag an unused argument. This exists to avoid ambiguity.
	 */
	public ListEmbed(List<Field> fields, boolean unusedFlag) {
		this.fields = chopped(fields, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given fields into groups for
	 * pages, and using the given title.
	 * 
	 * @param title  the title.
	 * @param fields the fields.
	 */
	public ListEmbed(String title, Field... fields) {
		b.setTitle(this.title = title);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given fields into groups for
	 * pages, and using the given title.
	 * 
	 * @param title      the title
	 * @param fields     the fields
	 * @param unusedFlag an unused argument. This exists to avoid ambiguity.
	 */
	public ListEmbed(String title, List<Field> fields, boolean unusedFlag) {
		b.setTitle(this.title = title);
		List<Field> content = fields;
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author, and separating the
	 * given fields into groups for pages.
	 * 
	 * @param author  the author name.
	 * @param iconURL the author's icon URL.
	 * @param fields  the fields.
	 */
	public ListEmbed(String author, String iconURL, Field... fields) {
		b.setAuthor(author, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author, and separating the
	 * given fields into groups for pages.
	 * 
	 * @param author  the author name.
	 * @param url     the author URL (website).
	 * @param iconURL the author's icon URL.
	 * @param fields  the fields.
	 */
	public ListEmbed(String author, String url, String iconURL, Field... fields) {
		b.setAuthor(author, url, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author and title, and
	 * separating the given fields into groups for pages.
	 * 
	 * @param unusedFlag an unused argument. This exists to avoid ambiguity.
	 * @param title      the title.
	 * @param author     the author name.
	 * @param iconURL    the author's icon URL.
	 * @param fields     the fields.
	 */
	public ListEmbed(boolean unusedFlag, String title, String author, String iconURL, List<Field> fields) {
		b.setTitle(this.title = title);
		b.setAuthor(author, iconURL);

		this.fields = chopped(fields, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author and title, and
	 * separating the given fields into groups for pages.
	 * 
	 * @param title   the title.
	 * @param author  the author name.
	 * @param url     the author's URL (website).
	 * @param iconURL the author's icon URL.
	 * @param fields  the fields.
	 */
	public ListEmbed(String title, String author, String url, String iconURL, Field... fields) {
		b.setTitle(this.title = title);
		b.setAuthor(author, url, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given elements into groups for
	 * pages.
	 * 
	 * @param content the list elements.
	 */
	public ListEmbed(List<String> content) {
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed}, separating the given elements into groups for
	 * pages.
	 * 
	 * @param content the list elements.
	 */
	public ListEmbed(String... content) {
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given title and separating the given
	 * elements into groups for pages.
	 * 
	 * @param title   the title.
	 * @param content the list elements.
	 * 
	 */
	public ListEmbed(String title, List<String> content) {
		b.setTitle(this.title = title);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given title and separating the given
	 * elements into groups for pages.
	 * 
	 * @param title   the title.
	 * @param content the list elements.
	 * 
	 */
	public ListEmbed(String title, String... content) {
		b.setTitle(this.title = title);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author and separating the
	 * given elements into groups for pages.
	 * 
	 * @param author  the author name.
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 * 
	 */
	public ListEmbed(String author, String iconURL, String... content) {
		b.setAuthor(author, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author and separating the
	 * given elements into groups for pages.
	 * 
	 * @param author  the author name.
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 * 
	 */
	public ListEmbed(String author, String iconURL, List<String> content) {
		b.setAuthor(author, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given author and title, and
	 * separating the given elements into groups for pages.
	 * 
	 * @param unusedFlag an unused argument. This exists to avoid ambiguity.
	 * @param title      the title.
	 * @param author     the author name.
	 * @param iconURL    the author's icon URL.
	 * @param content    the list elements.
	 * 
	 */
	public ListEmbed(boolean unusedFlag, String title, String author, String iconURL, String... content) {
		b.setTitle(this.title = title);
		b.setAuthor(author, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the given title and author, and
	 * separating the given elements into groups for pages.
	 *
	 * @param title      the title.
	 * @param author     the author name.
	 * @param iconURL    the author's icon URL.
	 * @param content    the list elements.
	 * @param unusedFlag an unused argument. This exists to avoid ambiguity.
	 * 
	 */
	public ListEmbed(String title, String author, String iconURL, List<String> content, boolean unusedFlag) {
		b.setTitle(this.title = title);
		b.setAuthor(author, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the author and separating the given
	 * elements into groups for pages.
	 *
	 * @param author  the author name.
	 * @param url     the author URL (website).
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 */
	public ListEmbed(String author, String url, String iconURL, String... content) {
		b.setAuthor(author, url, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the author and separating the given
	 * elements into groups for pages.
	 *
	 * @param author  the author name.
	 * @param url     the author URL (website).
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 */
	public ListEmbed(String author, String url, String iconURL, List<String> content) {
		b.setAuthor(author, url, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the title and author, and separating the
	 * given elements into groups for pages.
	 *
	 * @param title   the title.
	 * @param author  the author name.
	 * @param url     the author URL (website).
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 */
	public ListEmbed(String title, String author, String url, String iconURL, String... content) {
		b.setTitle(this.title = title);
		b.setAuthor(author, url, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	/**
	 * Constructs a {@link ListEmbed} using the title and author, and separating the
	 * given elements into groups for pages.
	 *
	 * @param title   the title.
	 * @param author  the author name.
	 * @param url     the author URL (website).
	 * @param iconURL the author's icon URL.
	 * @param content the list elements.
	 */
	public ListEmbed(String title, String author, String url, String iconURL, List<String> content) {
		b.setTitle(this.title = title);
		b.setAuthor(author, url, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	/**
	 * Set's this ListEmbed to use a random color for every page.
	 * 
	 * @return this, for convenience.
	 */
	public ListEmbed setColorRandomized() {
		rand = true;
		return this;
	}

	/**
	 * Get's the {@link EmbedBuilder} which this list uses to construct pages.
	 * 
	 * @return the embed builder.
	 */
	public EmbedBuilder getBuilder() {
		return b;
	}

	public LocalTime getLastInterection() {
		return lastInteraction;
	}

	private MessageEmbed get() {
		lastInteraction = LocalTime.now();
		if (rand) {
			b.setColor(ColorUtils.getRandomColor());
		}
		if (flag)
			return buildEmbedAlt(fields.get(index), index + 1);
		else if (flag2)
			return embeds.get(index).build();
		else
			return buildEmbed(pages.get(index), index + 1);
	}

	/**
	 * Used to determine whether or note this {@link ListEmbed} has more than one
	 * page.
	 * 
	 * @return <b>true</b> : this list embed has multiple pages.<br>
	 *         <b>false</b> : this list embed only has one page.
	 */
	public boolean isMultiPaged() {
		return pageCount > 1;
	}

	/**
	 * Get's this {@linkplain ListEmbed}'s first page.
	 * 
	 * @return the first page.
	 */
	public MessageEmbed first() {
		index = 0;
		return get();
	}

	/**
	 * Get's this {@linkplain ListEmbed}'s last page.
	 * 
	 * @return the last page.
	 */
	public MessageEmbed last() {
		index = pageCount - 1;
		return get();
	}

	/**
	 * Get's the next page in this {@link ListEmbed}.
	 * 
	 * @return the next page.
	 */
	public MessageEmbed next() {
		index = index == pageCount - 1 ? 0 : index + 1;
		return get();
	}

	/**
	 * Get's the previous page in this {@link ListEmbed}.
	 * 
	 * @return the previous page.
	 */
	public MessageEmbed previous() {
		index = index == 0 ? pageCount - 1 : index - 1;
		return get();
	}

	private MessageEmbed buildEmbed(List<String> values, int page) {
		b.clearFields();
		b.addField(
				title == null ? "Page " + page + "/" + pageCount + ":"
						: title + "\nPage " + page + "/" + pageCount + ":",
				values.stream().collect(Collectors.joining("\n")), true);
		return b.build();
	}

	private MessageEmbed buildEmbedAlt(List<Field> values, int page) {
		b.clearFields();
		values.forEach(b::addField);
		b.setTitle(title == null ? "Page " + page + "/" + pageCount : title + "\nPage " + page + "/" + pageCount);
		return b.build();
	}

	private static <T> List<List<T>> chopped(List<T> list, final int L) {
		List<List<T>> parts = new ArrayList<List<T>>();
		final int N = list.size();
		for (int i = 0; i < N; i += L) {
			parts.add(new ArrayList<T>(list.subList(i, Math.min(N, i + L))));
		}
		return parts;
	}
}
