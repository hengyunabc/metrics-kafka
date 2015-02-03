package io.github.hengyunabc.metrics;

import com.alibaba.fastjson.JSONObject;

/**
 * not thread safe.
 * 
 * @author hengyunabc
 *
 */
public interface MessageListener {

	/**
	 * receive zabbix message, not threadsafe.
	 * 
	 * @param message
	 */
	public void onMessage(JSONObject message);
}
