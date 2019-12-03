package com.Cardinal.CommandPackage.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.Cardinal.CommandPackage.Util.ColorUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;

public class ListEmbed {

	private List<List<String>> pages;
	private List<List<Field>> fields;
	private boolean flag = false, rand = false;;

	private EmbedBuilder b = new EmbedBuilder();
	private int index = 0, pageCount;

	public ListEmbed(Field... fields) {
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(List<Field> fields, boolean unusedFlag) {
		this.fields = chopped(fields, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(String title, Field... fields) {
		b.setTitle(title);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(String title, List<Field> fields, boolean unusedFlag) {
		b.setTitle(title);
		List<Field> content = fields;
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(String author, String iconURL, Field... fields) {
		b.setAuthor(author, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(String author, String url, String iconURL, Field... fields) {
		b.setAuthor(author, url, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(boolean unusedFlag, String title, String author, String iconURL, Field... fields) {
		b.setTitle(title);
		b.setAuthor(author, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(String title, String author, String url, String iconURL, Field... fields) {
		b.setTitle(title);
		b.setAuthor(author, url, iconURL);
		List<Field> content = Arrays.asList(fields);
		this.fields = chopped(content, 10);
		pageCount = this.fields.size();
		flag = true;
	}

	public ListEmbed(List<String> content) {
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String... content) {
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String title, List<String> content) {
		b.setTitle(title);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String title, String... content) {
		b.setTitle(title);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String author, String iconURL, String... content) {
		b.setAuthor(author, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String author, String iconURL, List<String> content) {
		b.setAuthor(author, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(boolean unusedFlag, String title, String author, String iconURL, String... content) {
		b.setTitle(title);
		b.setAuthor(author, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String title, String author, String iconURL, List<String> content, boolean unusedFlag) {
		b.setTitle(title);
		b.setAuthor(author, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String author, String url, String iconURL, String... content) {
		b.setAuthor(author, url, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String author, String url, String iconURL, List<String> content) {
		b.setAuthor(author, url, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String title, String author, String url, String iconURL, String... content) {
		b.setTitle(title);
		b.setAuthor(author, url, iconURL);
		pages = chopped(Arrays.asList(content), 15);
		pageCount = this.pages.size();
	}

	public ListEmbed(String title, String author, String url, String iconURL, List<String> content) {
		b.setTitle(title);
		b.setAuthor(author, url, iconURL);
		pages = chopped(content, 15);
		pageCount = this.pages.size();
	}

	public ListEmbed setColorRandomized() {
		rand = true;
		return this;
	}

	public EmbedBuilder getBuilder() {
		return b;
	}

	private MessageEmbed get() {
		if (rand) {
			b.setColor(ColorUtils.getRandomColor());
		}
		if (flag)
			return buildEmbedAlt(fields.get(index), index + 1);
		else
			return buildEmbed(pages.get(index), index + 1);
	}

	public boolean isMultiPaged() {
		return pageCount > 1;
	}

	public MessageEmbed first() {
		index = 0;
		return get();
	}

	public MessageEmbed last() {
		index = pages.size() - 1;
		return get();
	}

	public MessageEmbed next() {
		index = index == pages.size() - 1 ? 0 : index + 1;
		return get();
	}

	public MessageEmbed previous() {
		index = index == 0 ? pages.size() - 1 : index - 1;
		return get();
	}

	private MessageEmbed buildEmbed(List<String> values, int page) {
		b.clearFields();
		b.addField("Page " + page + "/" + pageCount + ":", values.stream().collect(Collectors.joining("\n")), true);
		return b.build();
	}

	private MessageEmbed buildEmbedAlt(List<Field> values, int page) {
		b.clearFields();
		values.forEach(b::addField);
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
