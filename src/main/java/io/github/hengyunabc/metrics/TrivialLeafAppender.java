package io.github.hengyunabc.metrics;

import com.alibaba.fastjson.JSONObject;

public class TrivialLeafAppender implements LeafAppender {

	@Override
	public void append(JSONObject parent, String key, Object leaf) {
		parent.put(key, leaf);
	}
}
