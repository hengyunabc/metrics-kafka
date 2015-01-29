package io.github.hengyunabc.metrics;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class KafkaReporter extends ScheduledReporter {
	private static final Logger logger = LoggerFactory
			.getLogger(KafkaReporter.class);

	int queueSize = 1000;
	// EvictingQueue<String> metricsQueue = EvictingQueue.create(queueSize);

	String topic;
	ProducerConfig config;
	Producer<String, String> producer;
	ExecutorService kafkaExecutor;

	private String prefix;
	private String hostName;
	private String ip;

	private KafkaReporter(MetricRegistry registry, String name,
			TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter,
			String topic, ProducerConfig config, String prefix,
			String hostName, String ip) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.topic = topic;
		this.config = config;
		this.prefix = prefix;
		this.hostName = hostName;
		this.ip = ip;
		producer = new Producer<String, String>(config);

		kafkaExecutor = Executors
				.newSingleThreadExecutor(new ThreadFactoryBuilder()
						.setNameFormat("kafka-producer-%d").build());
	}

	public static Builder forRegistry(MetricRegistry registry) {
		return new Builder(registry);
	}

	public static class Builder {
		private final MetricRegistry registry;
		private String name = "kafka-reporter";
		private TimeUnit rateUnit;
		private TimeUnit durationUnit;
		private MetricFilter filter;

		private String prefix = "";
		private String hostName;
		private String ip;

		private String topic;
		private ProducerConfig config;

		public Builder(MetricRegistry registry) {
			this.registry = registry;

			this.rateUnit = TimeUnit.SECONDS;
			this.durationUnit = TimeUnit.MILLISECONDS;
			this.filter = MetricFilter.ALL;
		}

		/**
		 * Convert rates to the given time unit.
		 *
		 * @param rateUnit
		 *            a unit of time
		 * @return {@code this}
		 */
		public Builder convertRatesTo(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return this;
		}

		/**
		 * Convert durations to the given time unit.
		 *
		 * @param durationUnit
		 *            a unit of time
		 * @return {@code this}
		 */
		public Builder convertDurationsTo(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return this;
		}

		/**
		 * Only report metrics which match the given filter.
		 *
		 * @param filter
		 *            a {@link MetricFilter}
		 * @return {@code this}
		 */
		public Builder filter(MetricFilter filter) {
			this.filter = filter;
			return this;
		}

		/**
		 * default register name is "kafka-reporter".
		 * 
		 * @param name
		 * @return
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder topic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder config(ProducerConfig config) {
			this.config = config;
			return this;
		}

		public Builder prefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public Builder hostName(String hostName) {
			this.hostName = hostName;
			return this;
		}

		public Builder ip(String ip) {
			this.ip = ip;
			return this;
		}

		/**
		 * Builds a {@link KafkaReporter} with the given properties.
		 *
		 * @return a {@link KafkaReporter}
		 */
		public KafkaReporter build() {
			if (hostName == null) {
				hostName = HostUtil.getHostName();
			}
			if (ip == null) {
				ip = HostUtil.getHostAddress();
			}

			return new KafkaReporter(registry, name, rateUnit, durationUnit,
					filter, topic, config, prefix, hostName, ip);
		}
	}

	private void addSnapshotJSONObject(String key, Snapshot snapshot,
			JSONObject result) {
		result.put(prefix + key + ".min", snapshot.getMin());
		result.put(prefix + key + ".max", snapshot.getMax());
		result.put(prefix + key + ".mean", snapshot.getMean());
		result.put(prefix + key + ".stddev", snapshot.getStdDev());
		result.put(prefix + key + ".median", snapshot.getMedian());
		result.put(prefix + key + ".75%", snapshot.get75thPercentile());
		result.put(prefix + key + ".95%", snapshot.get95thPercentile());
		result.put(prefix + key + ".98%", snapshot.get98thPercentile());
		result.put(prefix + key + ".99%", snapshot.get99thPercentile());
		result.put(prefix + key + ".99.9%", snapshot.get999thPercentile());
	}

	private void addSnapshotJSONObject(String key, Metered meter,
			JSONObject result) {
		result.put(prefix + key + ".count", meter.getCount());
		result.put(prefix + key + ".meanRate", meter.getMeanRate());
		result.put(prefix + key + ".1-minuteRate", meter.getOneMinuteRate());
		result.put(prefix + key + ".5-minuteRate", meter.getFiveMinuteRate());
		result.put(prefix + key + ".15-minuteRate",
				meter.getFifteenMinuteRate());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges,
			SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms,
			SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final JSONObject result = new JSONObject();

		result.put("hostName", hostName);
		result.put("ip", ip);
		result.put("reteUnit", getRateUnit());
		result.put("durationUnit", getDurationUnit());

		JSONArray gaugeArray = new JSONArray();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			JSONObject gauge = new JSONObject();
			gauge.put(prefix + entry.getKey(), entry.getValue().getValue()
					.toString());
			gaugeArray.add(gauge);
		}
		result.put("gauges", gaugeArray);

		JSONArray couterArray = new JSONArray();
		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			JSONObject couter = new JSONObject();
			couter.put(prefix + entry.getKey(), entry.getValue().getCount());
			couterArray.add(couter);
		}
		result.put("couters", couterArray);

		JSONArray histogramArray = new JSONArray();
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = entry.getValue();
			Snapshot snapshot = histogram.getSnapshot();
			JSONObject object = new JSONObject();
			addSnapshotJSONObject(entry.getKey(), snapshot, object);
			histogramArray.add(object);
		}
		result.put("histograms", histogramArray);

		JSONArray meterArray = new JSONArray();
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			Meter meter = entry.getValue();
			JSONObject object = new JSONObject();
			addSnapshotJSONObject(entry.getKey(), meter, object);
			meterArray.add(object);
		}
		result.put("meters", meterArray);

		JSONArray timerArray = new JSONArray();
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = entry.getValue();
			JSONObject object = new JSONObject();
			addSnapshotJSONObject(entry.getKey(), timer, object);
			addSnapshotJSONObject(entry.getKey(), timer.getSnapshot(), object);
			timerArray.add(object);
		}
		result.put("timers", timerArray);

		kafkaExecutor.execute(new Runnable() {
			@Override
			public void run() {
				KeyedMessage<String, String> message = new KeyedMessage<String, String>(
						topic, new Object().toString(), result.toJSONString());
				try {
					producer.send(message);
				} catch (Exception e) {
					logger.error("send metrics to kafka error!", e);
				}
			}
		});
	}

}
