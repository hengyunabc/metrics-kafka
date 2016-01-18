package io.github.hengyunabc.metrics;

import com.alibaba.fastjson.JSONObject;

/**
 * This interface can be used to mutate the key or or otherwise customize
 * the result object composition. The default implementation simply adds the 
 * leaf the parent using the key as the property name. 
 */
public interface LeafAppender {

	/**
	 * Append the value carrying leaf object to a parent by a key. 
	 * 
	 * @param parent The node to attach a leaf, never null
	 * @param key Intermittent node path to use, never null
	 * @param leaf Leaf node to attach, never null
	 */
	public void append(JSONObject parent, String key, Object leaf);
	
}
