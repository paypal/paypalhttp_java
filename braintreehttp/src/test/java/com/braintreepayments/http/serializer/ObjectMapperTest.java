package com.braintreepayments.http.serializer;

import com.braintreepayments.http.annotations.Model;
import com.braintreepayments.http.annotations.SerializedName;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class ObjectMapperTest {

	@Model
	private static class TestData {

		public TestData() {}

		@SerializedName("string_data")
		private String stringData = "string data";

		@SerializedName("int_data")
		private Integer intData = 0;

		@SerializedName("bool_data")
		private Boolean boolData = true;

		@SerializedName("double_data")
		private Double doubleData = 3.14;

		@SerializedName("float_data")
		private Float floatData = 2.72f;

		@SerializedName("nested_data")
		private NestedTestData nestedData = new NestedTestData();

		@SerializedName(value = "string_list", listClass = String.class)
		private List<String> stringList = new ArrayList<String>() {{ add("one"); add("two"); }};

		@SerializedName(value = "nested_datas", listClass = NestedTestData.class)
		private List<NestedTestData> nestedTestDatas = new ArrayList<NestedTestData>() {{ add(new NestedTestData()); }};

		private String unannotated_data = "unannotated data";

		// Special cases
		@SerializedName("null_bool_data")
		private Boolean nullBoolData;

		@SerializedName("transient_int")
		private transient Integer transientInt = 10;

		@SerializedName("null_string_data")
		private String nullStringData;

		@SerializedName("null_integer_data")
		private Integer nullIntegerData;
	}

	@Model
	private static class NestedTestData {

		public NestedTestData() {}

		@SerializedName("string_data")
		private String stringData = "nested string data";

		@SerializedName("int_data")
		private Integer intData = 1;
	}

	@Test
	public void testMap() throws IllegalAccessException {
		TestData data = new TestData();

		Map<String, Object> map = ObjectMapper.map(data);

		assertEquals("string data", map.get("string_data"));
		assertEquals(0, map.get("int_data"));
		assertEquals(true, map.get("bool_data"));
		assertEquals(3.14, map.get("double_data"));
		assertEquals(2.72f, (float) map.get("float_data"));
		assertEquals(null, map.get("unannotated_data"));

		List<String> strings = (List<String>) map.get("string_list");
		assertEquals("one", strings.get(0));
		assertEquals("two", strings.get(1));

		List<Map<String,Object>> nestedTestDatas = (List<Map<String, Object>>) map.get("nested_datas");
		assertEquals("nested string data", nestedTestDatas.get(0).get("string_data"));
		assertEquals(1, nestedTestDatas.get(0).get("int_data"));

		assertFalse(map.containsKey("null_bool_data"));
		assertFalse(map.containsKey("transient_int"));

		Map<String, Object> nestedMap = (Map<String, Object>) map.get("nested_data");
		assertEquals("nested string data", nestedMap.get("string_data"));
		assertEquals(1, nestedMap.get("int_data"));
	}

	@Test
	public void testUnmap() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Map<String, Object> data = new HashMap<String, Object>() {{
			put("string_data", "another string");
			put("int_data", 2);
			put("bool_data", false);
			put("double_data", 10.11);
			put("float_data", 11.10f);
			put("unannotated_data", "more unannotated data");
			put("string_list", new ArrayList<String>(){{ add("three"); add("four"); }});

			List<Map<String,Object>> nestedDataList = new ArrayList<>();
			nestedDataList.add(new HashMap<String, Object>() {{
				put("string_data", "nested in a list");
				put("int_data", 4);
			}});

			put("nested_datas", nestedDataList);

			Map<String,Object> nestedData = new HashMap<String, Object>() {{
				put("string_data", "another nested string");
				put("int_data", 3);
			}};

			put("nested_data", nestedData);
			put("null_int_data", null);
			put("null_string_data", null);
		}};

		TestData output = ObjectMapper.unmap(data, TestData.class);

		assertEquals("another string", output.stringData);
		assertEquals(2, (int) output.intData);
		assertEquals(false, (boolean) output.boolData);
		assertEquals(10.11, output.doubleData);
		assertEquals(11.10f, output.floatData);
		assertEquals("another nested string", output.nestedData.stringData);
		assertEquals(3, (int) output.nestedData.intData);

		assertEquals(2, output.stringList.size());
		assertEquals("three", output.stringList.get(0));
		assertEquals("four", output.stringList.get(1));

		assertEquals(1, output.nestedTestDatas.size());
		assertEquals("nested in a list", output.nestedTestDatas.get(0).stringData);
		assertEquals(4, (int) output.nestedTestDatas.get(0).intData);

		assertEquals(10, (int) output.transientInt);
		assertNull(output.nullIntegerData);
		assertNull(output.nullStringData);
	}
}
