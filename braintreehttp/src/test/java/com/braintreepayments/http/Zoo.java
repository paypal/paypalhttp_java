package com.braintreepayments.http;

import com.braintreepayments.http.annotations.Model;
import com.braintreepayments.http.annotations.SerializedName;

import java.util.List;

@Model
public class Zoo {

	@SerializedName("name")
	public String name;

	@SerializedName("number_of_animals")
	public Integer numberOfAnimals;

	@SerializedName("animal")
	public Animal animal;

	public Zoo(String name, Integer numberOfAnimals, Animal animal) {
		this.name = name;
		this.numberOfAnimals = numberOfAnimals;
		this.animal = animal;
	}

	public Zoo() {}

	@Model
	public static class Appendage {

		@SerializedName("location")
		public String location;

		@SerializedName("size")
		public Integer size;

		public Appendage() {}

		public Appendage(String location, Integer size) {
			this.location = location;
			this.size = size;
		}
	}

	@Model
	public static class Animal {

		@SerializedName("kind")
		public String kind;

		@SerializedName("age")
		public Integer age;

		@SerializedName("weight")
		public Integer weight;

		@SerializedName("appendages")
		public Fins appendages;

		@SerializedName(value = "locales", listClass = String.class)
		public List<String> locales;

		@SerializedName("carnivorous")
		public Boolean carnivorous;

		public Animal(
				String kind,
				Integer age,
				int weight,
				Fins appendages,
				List<String> locales,
				Boolean carnivorous
		) {
			this.kind = kind;
			this.age = age;
			this.weight = weight;
			this.appendages = appendages;
			this.locales = locales;
			this.carnivorous = carnivorous;
		}

		public Animal() {}
	}

	@Model
	public static class Fins {

		@SerializedName("Dorsal fin")
		public Appendage dorsalFin;

		@SerializedName("Ventral fin")
		public Appendage ventralFin;

		public Fins() {
			dorsalFin = new Appendage("Dorsal fin", 1);
			ventralFin = new Appendage("Ventral fin", 1);
		}
	}
}

