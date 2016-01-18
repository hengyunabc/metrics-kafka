package io.github.hengyunabc.metrics;

import com.alibaba.fastjson.JSONObject;

/**
 * This appender checks for a separator in the key name and nests
 * the leaf in a path split by the same separator. Eg, with the default
 * separator '.', appending a leaf with the key "category.key" to an
 * empty root will look like this:
 * 
 * <pre>
 * {
 *   "category": {
 *     "key": [leaf]
 *   }
 * }
 * </pre>
 */
public class NestingLeafAppender implements LeafAppender {

	private String separator;
	
	public NestingLeafAppender() {
		this(".");
	}
	
	public NestingLeafAppender(String separator) {
		this.separator = separator;
	}

	@Override
	public void append(JSONObject parent, String key, Object leaf) {
		int index = key.indexOf(separator);
		if(index == -1) {
			parent.put(key, leaf);
		} else {
			String prefix = key.substring(0, index);
			String rest = key.substring(index + 1);
			JSONObject nextParent = (JSONObject) parent.get(prefix);
			if(nextParent == null) {
				nextParent = new JSONObject();
				parent.put(prefix, nextParent);
			}
			append(nextParent, rest, leaf);
		}
	}
}
