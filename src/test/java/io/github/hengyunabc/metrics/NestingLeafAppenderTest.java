package io.github.hengyunabc.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class NestingLeafAppenderTest {

	@Test
	public void testTrivialAppend() {
		JSONObject root = new JSONObject();
		JSONObject leaf = new JSONObject();
		leaf.put("leafKey", "leafValue");
		new NestingLeafAppender().append(root, "category.key", leaf);
		JSONObject cat = (JSONObject) root.get("category");
		assertNotNull(cat);
		assertEquals(leaf, cat.get("key"));
	}
	
	@Test
	public void testRecursiveAppend() {
		JSONObject root = new JSONObject();
		JSONObject leaf1 = new JSONObject();
		leaf1.put("leafKey", "leafValue");
		JSONObject leaf2 = new JSONObject();
		leaf2.put("leafKey", "leafValue");
		new NestingLeafAppender().append(root, "category.sub.key1", leaf1);
		new NestingLeafAppender().append(root, "category.sub.key2", leaf2);
		JSONObject cat = (JSONObject) root.get("category");
		assertNotNull(cat);
		JSONObject sub = (JSONObject) cat.get("sub");
		assertNotNull(sub);
		assertEquals(leaf1, sub.get("key1"));
		assertEquals(leaf2, sub.get("key2"));
	}
}
