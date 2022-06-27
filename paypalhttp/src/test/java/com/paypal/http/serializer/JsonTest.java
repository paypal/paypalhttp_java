package com.paypal.http.serializer;

import com.paypal.http.Zoo;
import com.paypal.http.annotations.ListOf;
import com.paypal.http.annotations.Model;
import com.paypal.http.annotations.SerializedName;
import com.paypal.http.exceptions.MalformedJsonException;
import com.paypal.http.exceptions.SerializeException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class JsonTest {

    @Test()
	public void testJson_serializesObjectsToJSON() throws SerializeException {
    	Zoo.Fins fishAppendages = new Zoo.Fins();
    	fishAppendages.dorsalFin = new Zoo.Appendage("back", 2);
    	fishAppendages.ventralFin = new Zoo.Appendage("front", 2);

        ArrayList<String> fishLocales = new ArrayList<>();
        fishLocales.add("ocean");
        fishLocales.add("lake");

        Zoo.Animal fish = new Zoo.Animal(
                "swimmy",
                3,
                10,
                fishAppendages,
                fishLocales,
                false
        );

        Zoo zoo = new Zoo(
                "Monterey Bay Aquarium",
                1,
                fish
        );

        String expected = "{\"name\":\"Monterey Bay Aquarium\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":false,\"weight\":10.0,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"back\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";

        String s = new Json().serialize(zoo);
        assertEquals(s, expected);
    }

    @Test()
    public void testJson_serializesNestedMaps() throws SerializeException {
        HashMap<String, Object> map = new HashMap<>();

        HashMap<String, String> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");

        HashMap<String, String> map2 = new HashMap<>();
        map2.put("key3", "value3");
        map2.put("key4", "value4");

        map.put("map1", map1);
        map.put("map2", map2);

        String expected = "{\"map2\":{\"key3\":\"value3\",\"key4\":\"value4\"},\"map1\":{\"key1\":\"value1\",\"key2\":\"value2\"}}";

		String s = new Json().serialize(map);
		assertEquals(s, expected);
    }

    @Test()
    public void testJson_serializesArrays() throws SerializeException {

    	@Model
        class Base {
        	@SerializedName(value = "array", listClass = String.class)
            private List<String> array = new ArrayList<String>() {{ add("value1"); add("value2"); }};
        }

        String expected = "{\"array\":[\"value1\",\"value2\"]}";
        String actual = new Json().serialize(new Base());
        assertEquals(actual, expected);
    }

    /* Deserialize */

	@Test(expectedExceptions = MalformedJsonException.class)
    public void testJson_deserialize_errorsForNoOpenKeyQuote() throws IOException {
        String noStartQuote = "{\"my_key: \"value\"}";
		new Json().decode(noStartQuote, Map.class);
    }

	@Test(expectedExceptions = MalformedJsonException.class)
    public void testJson_deserialize_throwsForJSONWithoutStartingBracket() throws IOException {
        String noStartBracket = "\"my_key\":\"my_value\"}";
		new Json().decode(noStartBracket, Map.class);
    }

	@Test(expectedExceptions = MalformedJsonException.class)
	public void testJson_deserialize_throwsForJSONWithoutEndingBracket() throws IOException {
		String noStartBracket = "{\"my_key\":\"my_value\"";
		new Json().decode(noStartBracket, Map.class);
	}

	@Test(expectedExceptions = MalformedJsonException.class)
    public void testJson_deserialize_throwsForJSONWithoutColon() throws IOException {
        String noColon = "{\"my_key\"\"my_value\"}";
        new Json().decode(noColon, Map.class);
    }

    @Test()
    public void testJson_deserialize_parsesScientificNotationCorrectly() throws IOException {
		String json = "{\"key\": 1.233e10}";

		Map<String, Double> deserialized = new Json().decode(json, Map.class);

		assertEquals(deserialized.get("key"), 1.233e10);
    }

    @Test()
    public void testJson_deserialize_parsesNull() throws IOException {
		String json = "{\"key\": null}";
		Map<String, Object> deserialized = new Json().decode(json, Map.class);

		assertNull(deserialized.get("key"));
    }

    @Test()
    public void testJson_deserialize_throwsForArrayMissingCommas() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Json j = new Json();
		String json = "{\"locales\":[\"ocean\" \"lake\"]}";

		try {
			Zoo.Animal a = j.decode(json, Zoo.Animal.class);
			fail("Expected IOException");
		} catch (IOException ite) {
			assertTrue(ite instanceof MalformedJsonException);
		}
    }

    @Test()
    public void testJson_deserialize_throwsForArrayMissingClose() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
    	Json j = new Json();
    	String json = "{\"locales:[\"ocean\"}";

        try {
			Zoo.Animal a = j.decode(json, Zoo.Animal.class);
            fail("Expected IOException");
        } catch (IOException ite) {
			assertTrue(ite instanceof MalformedJsonException);
        }
    }

    @Test
	public void testJson_deserialize_withWhiteSpace() throws IOException {
		String serializedZoo = "{\n" +
				"\t\"name\": \"Monterey Bay Aquarium\",\n" +
				"\t\"animal\": {\n" +
				"\t\t\"locales\": [\"ocean\", \"lake\"],\n" +
				"\t\t\"kind\": \"swimmy\",\n" +
				"\t\t\"carnivorous\": false,\n" +
				"\t\t\"weight\": 10,\n" +
				"\t\t\"age\": 3,\n" +
				"\t\t\"appendages\": {\n" +
				"\t\t\t\"Dorsal fin\": {\n" +
				"\t\t\t\t\"size\": 2,\n" +
				"\t\t\t\t\"location\": \"back\"\n" +
				"\t\t\t},\n" +
				"\t\t\t\"Ventral fin\": {\n" +
				"\t\t\t\t\"size\": 2,\n" +
				"\t\t\t\t\"location\": \"front\"\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t},\n" +
				"\t\"number_of_animals\": 1\n" +
				"}";

		Zoo zoo  = new Json().decode(serializedZoo, Zoo.class);

		assertEquals("Monterey Bay Aquarium", zoo.name);
		assertEquals(1, zoo.numberOfAnimals.intValue());
		assertEquals("swimmy", zoo.animal.kind);
		assertEquals(3, zoo.animal.age.intValue());
		assertEquals(10, zoo.animal.weight.intValue());
		assertEquals("back", zoo.animal.appendages.dorsalFin.location);
		assertEquals(2, zoo.animal.appendages.dorsalFin.size.intValue());
		assertEquals("front", zoo.animal.appendages.ventralFin.location);
		assertEquals(2, zoo.animal.appendages.ventralFin.size.intValue());
		assertEquals("ocean", zoo.animal.locales.get(0));
		assertEquals("lake", zoo.animal.locales.get(1));
		assertEquals(false, zoo.animal.carnivorous.booleanValue());
	}

    @Test()
    public void testJson_deserialize_createsAnObjectFromJSON() throws IOException {
       String serializedZoo = "{\"name\":\"Monterey Bay Aquarium\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":false,\"weight\":10,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"back\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";

       Zoo zoo  = new Json().decode(serializedZoo, Zoo.class);

       assertEquals("Monterey Bay Aquarium", zoo.name);
       assertEquals(1, zoo.numberOfAnimals.intValue());
       assertEquals("swimmy", zoo.animal.kind);
       assertEquals(3, zoo.animal.age.intValue());
       assertEquals(10.0, zoo.animal.weight);
       assertEquals("back", zoo.animal.appendages.dorsalFin.location);
       assertEquals(2, zoo.animal.appendages.dorsalFin.size.intValue());
       assertEquals("front", zoo.animal.appendages.ventralFin.location);
       assertEquals(2, zoo.animal.appendages.ventralFin.size.intValue());
       assertEquals("ocean", zoo.animal.locales.get(0));
       assertEquals("lake", zoo.animal.locales.get(1));
       assertEquals(false, zoo.animal.carnivorous.booleanValue());
    }

	@Test()
	public void testJson_deserialize_withBracketInString() throws IOException {
		String serializedZoo = "{\"name\":\"Monterey Bay Aquarium\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":false,\"weight\":10,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"ba}ck\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";

		Zoo zoo  = new Json().decode(serializedZoo, Zoo.class);

		assertEquals("ba}ck", zoo.animal.appendages.dorsalFin.location);
	}

	@Test()
	public void testJson_deserialize_createsAnObjectWithNullValues() throws IOException {
		String serializedZoo = "{\"name\":null, \"animal\":{\"locales\":[\"ocean\",\"lake\"]}}";

		Zoo zoo  = new Json().decode(serializedZoo, Zoo.class);

		assertEquals(null, zoo.name);
		assertEquals("ocean", zoo.animal.locales.get(0));
		assertEquals("lake", zoo.animal.locales.get(1));
	}

    @Test()
    public void testJson_createsAnObjectWithEscapeCharacter() throws IOException {
		ArrayList<String> fishLocales = new ArrayList<>();
		fishLocales.add("ocean");
		fishLocales.add("lake");

		Zoo.Animal fish = new Zoo.Animal(
				null,
				0,
				0,
				null,
				fishLocales,
				false
		);

		// create a object with input value of double quote and three back slash
		Zoo zoo = new Zoo(
				"test \\\", ",
				1,
				fish
		);

		String serializedZoo = new Json().serialize(zoo);
        zoo  = new Json().decode(serializedZoo, Zoo.class);

        assertEquals(zoo.name, "test \\\", ");
        assertEquals( zoo.animal.locales.get(0),"ocean");
        assertEquals(zoo.animal.locales.get(1),"lake");
    }

	@Model
	@ListOf(listClass = Zoo.class)
	public static class ZooList extends ArrayList<Zoo> {

		public ZooList() {
			super();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJson_deserialize_list() throws IOException {
		String montereyBay = "{\"name\":\"Monterey Bay Aquarium\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":false,\"weight\":10,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"back\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";
		String shedd = "{\"name\":\"Shedd\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":true,\"weight\":10,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"back\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";
		String serializedZoo = String.format("[%s, %s]", montereyBay, shedd);

		ZooList zoos = new Json().decode(serializedZoo, ZooList.class);

		assertEquals(zoos.size(), 2);

		Zoo monterey = zoos.get(0);

		assertEquals("Monterey Bay Aquarium", monterey.name);
		assertEquals(1, monterey.numberOfAnimals.intValue());
		assertEquals("swimmy", monterey.animal.kind);
		assertEquals(3, monterey.animal.age.intValue());
		assertEquals(10.0, monterey.animal.weight);
		assertEquals("back", monterey.animal.appendages.dorsalFin.location);
		assertEquals(2, monterey.animal.appendages.dorsalFin.size.intValue());
		assertEquals("front", monterey.animal.appendages.ventralFin.location);
		assertEquals(2, monterey.animal.appendages.ventralFin.size.intValue());
		assertEquals("ocean", monterey.animal.locales.get(0));
		assertEquals("lake", monterey.animal.locales.get(1));
		assertEquals(false, monterey.animal.carnivorous.booleanValue());

		Zoo sheddAq = zoos.get(1);
		assertEquals("Shedd", sheddAq.name);
		assertEquals(1, sheddAq.numberOfAnimals.intValue());
		assertEquals("swimmy", sheddAq.animal.kind);
		assertEquals(3, sheddAq.animal.age.intValue());
		assertEquals(10.0, sheddAq.animal.weight);
		assertEquals("back", sheddAq.animal.appendages.dorsalFin.location);
		assertEquals(2, sheddAq.animal.appendages.dorsalFin.size.intValue());
		assertEquals("front", sheddAq.animal.appendages.ventralFin.location);
		assertEquals(2, sheddAq.animal.appendages.ventralFin.size.intValue());
		assertEquals("ocean", sheddAq.animal.locales.get(0));
		assertEquals("lake", sheddAq.animal.locales.get(1));
		assertEquals(true, sheddAq.animal.carnivorous.booleanValue());
	}

    @Test
    public void testJson_deserialize_deserializesRawList() throws IOException {
		String serializedZoo = "[\"name\",\"Monterey Bay Aquarium\"]";

		List<String> strings = new Json().decode(serializedZoo, List.class);
		assertEquals(strings.size(), 2);
		assertEquals(strings.get(0), "name");
		assertEquals(strings.get(1), "Monterey Bay Aquarium");
	}

	@Test
	public void testJson_deserialize_deserializesEmptyObject() throws IOException {
    	String empty = "{}";
    	Map<String, Object> deserialized = new Json().decode(empty, Map.class);

    	assertEquals(deserialized.size(), 0);
	}

	@Test
	public void testJson_deserialize_deserializesEmptyList() throws IOException {
		String empty = "[]";
		List<Object> deserialized = new Json().decode(empty, List.class);

		assertEquals(deserialized.size(), 0);
	}

	@Test
	public void testJson_deserialize_deserializesWithCommasInValues() throws IOException {
		String empty = "{\"key\": \"one,two\", \"key_two\": \"four,five\"}";
		Map<String,Object> deserialized = new Json().decode(empty, Map.class);

		assertEquals(deserialized.size(), 2);
		assertEquals(deserialized.get("key"), "one,two");
		assertEquals(deserialized.get("key_two"), "four,five");
	}
	@Test
	public void testJson_deserialize_deserializeNestedList() throws IOException {
		String serializedZoo = "[[\"name\",\"Monterey Bay Aquarium\"],[\"Shedd\"]]";

		List<List<String>> strings = new Json().decode(serializedZoo, List.class);
		assertEquals(strings.size(), 2);
		assertEquals(strings.get(0).get(0), "name");
		assertEquals(strings.get(0).get(1), "Monterey Bay Aquarium");

		assertEquals(strings.get(1).get(0), "Shedd");
	}

	@Test
	public void testNestedEmptyObj() throws IOException {
		String empty = "{\"id\":{}}";

		Map<String, Object> obj = new Json().decode(empty, Map.class);
		assertNotNull(obj.get("id"));
	}

	@Test
	public void testNestedEmptyList() throws IOException {
		String empty = "{\"id\":[]}";

		Map<String,Object> obj = new Json().decode(empty, Map.class);
		assertNotNull(obj.get("id"));
		assertTrue(obj.get("id") instanceof List);
	}

	@Test
	public void testParsesEmptyString() throws IOException {
    	String json = "{\"name\": \"\"}";

    	Map<String, Object> deserialized = new Json().decode(json, Map.class);

    	assertEquals(deserialized.get("name"), "");
	}
}
