package com.braintreepayments.http.serializer;

import com.braintreepayments.http.Zoo;
import com.braintreepayments.http.annotations.Model;
import com.braintreepayments.http.annotations.SerializedName;
import com.braintreepayments.http.exceptions.JsonParseException;
import com.braintreepayments.http.exceptions.SerializeException;
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

        String expected = "{\"name\":\"Monterey Bay Aquarium\",\"animal\":{\"locales\":[\"ocean\",\"lake\"],\"kind\":\"swimmy\",\"carnivorous\":false,\"weight\":10,\"age\":3,\"appendages\":{\"Dorsal fin\":{\"size\":2,\"location\":\"back\"},\"Ventral fin\":{\"size\":2,\"location\":\"front\"}}},\"number_of_animals\":1}";

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

    @Test(expectedExceptions = JsonParseException.class)
    public void testJson_deserialize_errorsForNoOpenKeyQuote() throws IOException {
        String noStartQuote = "{\"my_key: \"value\"}";
		new Json().deserialize(noStartQuote, Map.class);
    }

	@Test(expectedExceptions = JsonParseException.class)
	public void testJson_deserialize_errorsForNoEndKeyQuote() throws IOException {
		String noStartQuote = "{my_key\": \"value\"}";
		new Json().deserialize(noStartQuote, Map.class);
	}

    @Test(expectedExceptions = JsonParseException.class)
    public void testJson_deserialize_throwsForJSONWithoutStartingBracket() throws IOException {
        String noStartBracket = "\"my_key\":\"my_value\"}";
		new Json().deserialize(noStartBracket, Map.class);
    }

	@Test(expectedExceptions = JsonParseException.class)
	public void testJson_deserialize_throwsForJSONWithoutEndingBracket() throws IOException {
		String noStartBracket = "{\"my_key\":\"my_value\"";
		new Json().deserialize(noStartBracket, Map.class);
	}

    @Test(expectedExceptions = JsonParseException.class)
    public void testJson_deserialize_throwsForJSONWithoutColon() throws IOException {
        String noColon = "{\"my_key\"\"my_value\"}";
        new Json().deserialize(noColon, Map.class);
    }

    @Test()
    public void testJson_deserialize_parsesScientificNotationCorrectly() throws IOException {
		String json = "{\"key\": 1.233e10}";

		Map<String, Double> deserialized = new Json().deserialize(json, Map.class);

		assertEquals(deserialized.get("key"), 1.233e10);
    }

    @Test()
    public void testJson_deserialize_parsesNull() throws IOException {
		String json = "{\"key\": null}";
		Map<String, Object> deserialized = new Json().deserialize(json, Map.class);

		assertNull(deserialized.get("key"));
    }

    @Test()
    public void testJson_deserialize_throwsForInvalidBoolean() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		String json = "{\"is_false\": faslee}";
    	Json j = new Json();

		try {
			Map<String, Object> a = j.deserialize(json, Map.class);
			fail("Expected IOException");
		} catch (IOException ite) {
			assertTrue(ite instanceof JsonParseException);
		}
    }

    @Test()
    public void testJson_deserialize_throwsForArrayMissingCommas() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Json j = new Json();
		String json = "{\"locales\":[\"ocean\" \"lake\"]}";

		try {
			Zoo.Animal a = j.deserialize(json, Zoo.Animal.class);
			fail("Expected IOException");
		} catch (IOException ite) {
			assertTrue(ite instanceof JsonParseException);
		}
    }

    @Test()
    public void testJson_deserialize_throwsForArrayMissingClose() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
    	Json j = new Json();
    	String json = "{\"locales:[\"ocean\"}";

        try {
			Zoo.Animal a = j.deserialize(json, Zoo.Animal.class);
            fail("Expected IOException");
        } catch (IOException ite) {
        	assertTrue(ite instanceof JsonParseException);
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

		Zoo zoo  = new Json().deserialize(serializedZoo, Zoo.class);

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

       Zoo zoo  = new Json().deserialize(serializedZoo, Zoo.class);

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

    @Test
    public void testJson_deserialize_deserializesRawList() throws IOException {
		String serializedZoo = "[\"name\",\"Monterey Bay Aquarium\"]";

		List<String> strings = new Json().deserialize(serializedZoo, List.class);
		assertEquals(strings.size(), 2);
		assertEquals(strings.get(0), "name");
		assertEquals(strings.get(1), "Monterey Bay Aquarium");
	}

	@Test
	public void testJson_deserialize_deserializesEmptyObject() throws IOException {
    	String empty = "{}";
    	Map<String, Object> deserialized = new Json().deserialize(empty, Map.class);

    	assertEquals(deserialized.size(), 0);
	}

	@Test
	public void testJson_deserialize_deserializesEmptyList() throws IOException {
		String empty = "[]";
		List<Object> deserialized = new Json().deserialize(empty, List.class);

		assertEquals(deserialized.size(), 0);
	}

	@Test
	public void testJson_deserialize_deserializesWithCommasInValues() throws IOException {
		String empty = "{\"key\": \"one,two\", \"key_two\": \"four,five\"}";
		Map<String,Object> deserialized = new Json().deserialize(empty, Map.class);

		assertEquals(deserialized.size(), 2);
		assertEquals(deserialized.get("key"), "one,two");
		assertEquals(deserialized.get("key_two"), "four,five");
	}
	@Test
	public void testJson_deserialize_deserializeNestedList() throws IOException {
		String serializedZoo = "[[\"name\",\"Monterey Bay Aquarium\"],[\"Shedd\"]]";

		List<List<String>> strings = new Json().deserialize(serializedZoo, List.class);
		assertEquals(strings.size(), 2);
		assertEquals(strings.get(0).get(0), "name");
		assertEquals(strings.get(0).get(1), "Monterey Bay Aquarium");

		assertEquals(strings.get(1).get(0), "Shedd");
	}

	@Test
	public void testNestedEmptyObj() throws IOException {
		String empty = "{\"id\":{}}";

		Map<String, Object> obj = new Json().deserialize(empty, Map.class);
		assertNotNull(obj.get("id"));
	}

	@Test
	public void testNestedEmptyList() throws IOException {
		String empty = "{\"id\":[]}";

		Map<String,Object> obj = new Json().deserialize(empty, Map.class);
		assertNotNull(obj.get("id"));
		assertTrue(obj.get("id") instanceof List);
	}

	@Test
	public void testParsesEmptyString() throws IOException {
    	String json = "{\"name\": \"\"}";

    	Map<String, Object> deserialized = new Json().deserialize(json, Map.class);

    	assertEquals(deserialized.get("name"), "");
	}
}
