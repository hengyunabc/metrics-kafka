package io.github.hengyunabc.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MetricsKafkaConsumer {
	private static final Logger logger = LoggerFactory
			.getLogger(MetricsKafkaConsumer.class);
	String zookeeper;
	String group;
	String topic;

	int threadNumber = 1;

	int zookeeperSessionTimeoutMs = 4000;
	int zookeeperSyncTimeMs = 2000;
	int autoCommitIntervalMs = 1000;

	MessageListener messageListener;

	ExecutorService executor;

	@SuppressWarnings("rawtypes")
	public void init() {
		Properties props = new Properties();
		props.put("zookeeper.connect", zookeeper);
		props.put("group.id", group);
		props.put("zookeeper.session.timeout.ms", "" + zookeeperSessionTimeoutMs);
		props.put("zookeeper.sync.time.ms", "" + zookeeperSyncTimeMs);
		props.put("auto.commit.interval.ms", "" + autoCommitIntervalMs);

		ConsumerConfig config = new ConsumerConfig(props);

		ConsumerConnector consumer = kafka.consumer.Consumer
				.createJavaConsumerConnector(config);

		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topic, threadNumber);
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicCountMap);
		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);

		// now launch all the threads
		//
		executor = Executors.newFixedThreadPool(
				threadNumber,
				new ThreadFactoryBuilder().setNameFormat(
						"kafka-zabbixconsumer-%d").build());

		for (final KafkaStream stream : streams) {
			executor.submit(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					
					ConsumerIterator<byte[], byte[]> it = stream.iterator();
					while (it.hasNext()) {
						try {
							messageListener.onMessage((JSONObject) JSON.parse(it
									.next().message()));
						} catch (RuntimeException e) {
							logger.error("consumer kafka zabbix message error!", e);
						}
					}
				}
			});
		}

	}

	public void desotry() {
		if(executor != null){
			executor.shutdown();
		}
	}

	public String getZookeeper() {
		return zookeeper;
	}

	public void setZookeeper(String zookeeper) {
		this.zookeeper = zookeeper;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}

	public int getZookeeperSessionTimeoutMs() {
		return zookeeperSessionTimeoutMs;
	}

	public void setZookeeperSessionTimeoutMs(int zookeeperSessionTimeoutMs) {
		this.zookeeperSessionTimeoutMs = zookeeperSessionTimeoutMs;
	}

	public int getZookeeperSyncTimeMs() {
		return zookeeperSyncTimeMs;
	}

	public void setZookeeperSyncTimeMs(int zookeeperSyncTimeMs) {
		this.zookeeperSyncTimeMs = zookeeperSyncTimeMs;
	}

	public int getAutoCommitIntervalMs() {
		return autoCommitIntervalMs;
	}

	public void setAutoCommitIntervalMs(int autoCommitIntervalMs) {
		this.autoCommitIntervalMs = autoCommitIntervalMs;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}
}
