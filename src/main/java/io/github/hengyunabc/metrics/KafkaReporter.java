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

public class KafkaReporter extends ScheduledReporter {
	private static final Logger logger = LoggerFactory
			.getLogger(KafkaReporter.class);

	/**
	 * in some system, do not support '%', so will replace '%' to other string.
	 * default is empty.
	 */
	String replacePercentSign = "";
	
	String topic;
	ProducerConfig config;
	Producer<String, String> producer;
	ExecutorService kafkaExecutor;

	private String prefix;
	private String hostName;
	private String ip;

	int count = 0;

	private LeafAppender appender;

	private KafkaReporter(MetricRegistry registry, String replacePercentSign, String name,
			TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter,
			String topic, ProducerConfig config, String prefix,
			String hostName, String ip, LeafAppender appender) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.replacePercentSign = replacePercentSign;
		this.topic = topic;
		this.config = config;
		this.prefix = prefix;
		this.hostName = hostName;
		this.ip = ip;
		this.appender = appender;
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
		
		private String replacePercentSign = "";

		private String prefix = "";
		private String hostName;
		private String ip;

		private String topic;
		private ProducerConfig config;
		
		private LeafAppender appender = new TrivialLeafAppender();

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
		 * Use leaf appender. The default appender does
		 * not mutate the keys. 
		 *  
		 * @param app Appender to use, must not be null
		 */
		public Builder appender(LeafAppender app) {
			this.appender = app;
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
		
		public Builder replacePercentSign(String replacePercentSign) {
			this.replacePercentSign = replacePercentSign;
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
				logger.info(name + " detect hostName: " + hostName);
			}
			if (ip == null) {
				ip = HostUtil.getHostAddress();
				logger.info(name + " detect ip: " + ip);
			}

			return new KafkaReporter(registry, replacePercentSign, name, rateUnit, durationUnit,
					filter, topic, config, prefix, hostName, ip, appender);
		}
	}

	/**
	 * for histogram.
	 * @param snapshot
	 * @return
	 */
	private JSONObject snapshotToJSONObject(Snapshot snapshot) {
		JSONObject result = new JSONObject(16);
		appender.append(result, "min", snapshot.getMin());
		appender.append(result, "max", snapshot.getMax());
		appender.append(result, "mean", snapshot.getMean());
		appender.append(result, "stddev", snapshot.getStdDev());
		appender.append(result, "median", snapshot.getMedian());
		appender.append(result, "75" + replacePercentSign, snapshot.get75thPercentile());
		appender.append(result, "95" + replacePercentSign, snapshot.get95thPercentile());
		appender.append(result, "98" + replacePercentSign, snapshot.get98thPercentile());
		appender.append(result, "99" + replacePercentSign, snapshot.get99thPercentile());
		appender.append(result, "99.9" + replacePercentSign, snapshot.get999thPercentile());
		return result;
	}
	
	/**
	 * for timer.
	 * @param snapshot
	 * @return
	 */
	private JSONObject snapshotToJSONObjectWithConvertDuration(Snapshot snapshot) {
		JSONObject result = new JSONObject(16);
		appender.append(result, "min", convertDuration(snapshot.getMin()));
		appender.append(result, "max", convertDuration(snapshot.getMax()));
		appender.append(result, "mean", convertDuration(snapshot.getMean()));
		appender.append(result, "stddev", convertDuration(snapshot.getStdDev()));
		appender.append(result, "median", convertDuration(snapshot.getMedian()));
		appender.append(result, "75" + replacePercentSign, convertDuration(snapshot.get75thPercentile()));
		appender.append(result, "95" + replacePercentSign, convertDuration(snapshot.get95thPercentile()));
		appender.append(result, "98" + replacePercentSign, convertDuration(snapshot.get98thPercentile()));
		appender.append(result, "99" + replacePercentSign, convertDuration(snapshot.get99thPercentile()));
		appender.append(result, "99.9" + replacePercentSign, convertDuration(snapshot.get999thPercentile()));
		return result;
	}

	private JSONObject meterToJSONObject(Metered meter) {
		JSONObject result = new JSONObject(16);
		appender.append(result, "count", meter.getCount());
		appender.append(result, "meanRate", convertRate(meter.getMeanRate()));
		appender.append(result, "1-minuteRate", convertRate(meter.getOneMinuteRate()));
		appender.append(result, "5-minuteRate", convertRate(meter.getFiveMinuteRate()));
		appender.append(result, "15-minuteRate", convertRate(meter.getFifteenMinuteRate()));
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges,
			SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms,
			SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final JSONObject result = new JSONObject();

		appender.append(result, "hostName", hostName);
		appender.append(result, "ip", ip);
		appender.append(result, "rateUnit", getRateUnit());
		appender.append(result, "durationUnit", getDurationUnit());

		JSONObject gaugesJSONObject = new JSONObject();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			appender.append(gaugesJSONObject, prefix + entry.getKey(), entry.getValue().getValue());
		}
		appender.append(result, "gauges", gaugesJSONObject);

		JSONObject coutersJSONObject = new JSONObject();
		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			appender.append(coutersJSONObject, prefix + entry.getKey(), entry.getValue());
		}
		appender.append(result, "counters", coutersJSONObject);

		JSONObject histogramsJSONObject = new JSONObject();
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = entry.getValue();
			Snapshot snapshot = histogram.getSnapshot();
			appender.append(histogramsJSONObject, prefix + entry.getKey(), snapshotToJSONObject(snapshot));
		}
		appender.append(result, "histograms", histogramsJSONObject);

		JSONObject metersJSONObject = new JSONObject();
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			appender.append(metersJSONObject, prefix + entry.getKey(), meterToJSONObject(entry.getValue()));
		}
		appender.append(result, "meters", metersJSONObject);

		JSONObject timersJSONObject = new JSONObject();
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = entry.getValue();
			JSONObject timerJSONObjet = meterToJSONObject(timer);
			for (Map.Entry<String, Object> subEntry: snapshotToJSONObjectWithConvertDuration(timer.getSnapshot()).entrySet()) {
				appender.append(timerJSONObjet, subEntry.getKey(), subEntry.getValue());
			}
			appender.append(timersJSONObject, prefix + entry.getKey(), timerJSONObjet);
		}
		appender.append(result, "timers", timersJSONObject);

		appender.append(result, "clock", System.currentTimeMillis());

		kafkaExecutor.execute(new Runnable() {
			@Override
			public void run() {
				KeyedMessage<String, String> message = new KeyedMessage<String, String>(
						topic, "" + count++, result.toJSONString());
				try {
					producer.send(message);
				} catch (Exception e) {
					logger.error("send metrics to kafka error!", e);
				}
			}
		});
	}

}
