package com.braintreepayments.http;

import com.braintreepayments.http.serializer.Deserializable;
import com.braintreepayments.http.serializer.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Zoo implements Serializable, Deserializable {

	public String name;
	public Integer numberOfAnimals;
	public Animal animal;

	public Zoo(String name, Integer numberOfAnimals, Animal animal) {
		this.name = name;
		this.numberOfAnimals = numberOfAnimals;
		this.animal = animal;
	}

	public Zoo() {}

	@Override
	public void serialize(Map<String, Object> map) {
		if (this.name != null) {
			map.put("name", this.name);
		}
		if (this.numberOfAnimals != null) {
			map.put("number_of_animals", this.numberOfAnimals);
		}
		if (this.animal != null) {
			map.put("animal", this.animal);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deserialize(Map<String, Object> fields) {
		if (fields.containsKey("name")) {
			this.name = (String) fields.get("name");
		}
		if (fields.containsKey("number_of_animals")) {
			this.numberOfAnimals = (Integer) fields.get("number_of_animals");
		}
		if (fields.containsKey("animal")) {
			this.animal = new Animal();
			this.animal.deserialize((Map<String, Object>) fields.get("animal"));
		}
	}

	public static class Appendage implements Serializable, Deserializable {

		public String location;
		public Integer size;

		public Appendage() {}

		public Appendage(String location, Integer size) {
			this.location = location;
			this.size = size;
		}

		@Override
		public void serialize(Map<String, Object> map) {
			if (this.location != null) {
				map.put("location", this.location);
			}
			if (this.size != null) {
				map.put("size", this.size);
			}
		}

		@Override
		public void deserialize(Map<String, Object> fields) {
			if (fields.containsKey("location")) {
				this.location = (String) fields.get("location");
			}
			if (fields.containsKey("size")) {
				this.size = (Integer) fields.get("size");
			}
		}
	}

	public static class Animal implements Serializable, Deserializable {

		public String kind;
		public Integer age;
		public Integer weight;
		public Fins appendages;
		public List<String> locales;
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

		@Override
		public void serialize(Map<String, Object> map) {
			if (this.kind != null) {
				map.put("kind", this.kind);
			}
			if (this.age != null) {
				map.put("age", this.age);
			}
			if (this.weight != null) {
				map.put("weight", this.weight);
			}
			if (this.appendages != null) {
				map.put("appendages", this.appendages);
			}

			if (this.locales != null) {
				map.put("locales", this.locales);
			}

			if (this.carnivorous != null) {
				map.put("carnivorous", this.carnivorous);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void deserialize(Map<String, Object> fields) {
			if (fields.containsKey("kind")) {
				this.kind = (String) fields.get("kind");
			}
			if (fields.containsKey("age")) {
				this.age = (Integer) fields.get("age");
			}
			if (fields.containsKey("weight")) {
				this.weight = (int) fields.get("weight");
			}
			if (fields.containsKey("appendages")) {
				this.appendages = new Fins();
				this.appendages.deserialize((Map<String, Object>) fields.get("appendages"));
			}

			if (fields.containsKey("locales")) {
				this.locales = new ArrayList<>();
				List<String> nestedValues = (List<String>) fields.get("locales");
				for (String nestedValue : nestedValues) {
					this.locales.add(nestedValue);
				}
			}

			if (fields.containsKey("carnivorous")) {
				this.carnivorous = (Boolean) fields.get("carnivorous");
			}
		}
	}

	public static class Fins implements Deserializable, Serializable {

		public Appendage dorsalFin;
		public Appendage ventralFin;

		public Fins() {
			dorsalFin = new Appendage("Dorsal fin", 1);
			ventralFin = new Appendage("Ventral fin", 1);
		}

		@Override
		public void serialize(Map<String, Object> map) {
			if (this.dorsalFin != null) {
				map.put("Dorsal fin", this.dorsalFin);
			}
			if (this.ventralFin != null) {
				map.put("Ventral fin", this.ventralFin);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void deserialize(Map<String, Object> fields) {
			if (fields.containsKey("Dorsal fin")) {
				this.dorsalFin = new Appendage();
				this.dorsalFin.deserialize((Map<String, Object>) fields.get("Dorsal fin"));
			}

			if (fields.containsKey("Ventral fin")) {
				this.ventralFin = new Appendage();
				this.ventralFin.deserialize((Map<String, Object>) fields.get("Ventral fin"));
			}
		}
	}
}

