package com.jihox.composition.model.book.org.example.photobook;

import java.util.List;

public class WeBook {
	private float thickness;
	private List<WeBookPage> pages;
	
	public float getThickness() {
		return thickness;
	}
	public void setThickness(float thickness) {
		this.thickness = thickness;
	}
	public List<WeBookPage> getPages() {
		return pages;
	}
	public void setPages(List<WeBookPage> pages) {
		this.pages = pages;
	}
	
}
