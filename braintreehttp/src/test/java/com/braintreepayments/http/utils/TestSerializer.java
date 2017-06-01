package com.braintreepayments.http.utils;


import com.google.gson.annotations.SerializedName;

public class TestSerializer {

	@SerializedName("name")
	private String name;

	@SerializedName("value")
	private int value;

	@SerializedName("is_good")
	private boolean isGood;

	public String getName() { return name; }
	public int getValue() { return value; }
	public boolean isGood() { return isGood; }
}
