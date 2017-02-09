package com.jihox.composition.task;

import java.util.List;

import com.jihox.composition.common.BaseTask;
import com.jihox.composition.model.book.org.example.photobook.WeBookPage;

public class CompositionTask extends BaseTask {
	private List<WeBookPage> pages;

	public List<WeBookPage> getPages() {
		return pages;
	}

	public void setPages(List<WeBookPage> pages) {
		this.pages = pages;
	}

}
