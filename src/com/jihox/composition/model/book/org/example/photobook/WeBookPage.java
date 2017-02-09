package com.jihox.composition.model.book.org.example.photobook;

import java.util.List;

public class WeBookPage {
	
	private String type;
	private float width;
	private float height;
	private String name;
	private List<WeBookContent> contents;
	
	private float flapwidth;
	private boolean printwithnormalpage;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public float getWidth() {
		return width;
	}
	public void setWidth(float width) {
		this.width = width;
	}
	public float getHeight() {
		return height;
	}
	public void setHeight(float height) {
		this.height = height;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<WeBookContent> getContents() {
		return contents;
	}
	public void setContents(List<WeBookContent> contents) {
		this.contents = contents;
	}
	public float getFlapwidth() {
		return flapwidth;
	}
	public void setFlapwidth(float flapwidth) {
		this.flapwidth = flapwidth;
	}
	public boolean isPrintwithnormalpage() {
		return printwithnormalpage;
	}
	public void setPrintwithnormalpage(boolean printwithnormalpage) {
		this.printwithnormalpage = printwithnormalpage;
	}

	
}
