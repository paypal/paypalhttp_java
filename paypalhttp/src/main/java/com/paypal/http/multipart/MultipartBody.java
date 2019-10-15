package com.paypal.http.multipart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultipartBody implements Iterable<FormData> {

	private List<FormData> partList;

	public MultipartBody(FormData... parts) {
		this.partList = new ArrayList<>();

		partList.addAll(Arrays.asList(parts));
	}

	public void addPart(FormData part) {
		partList.add(part);
	}

	@Override
	public Iterator<FormData> iterator() {
		return partList.iterator();
	}
}
