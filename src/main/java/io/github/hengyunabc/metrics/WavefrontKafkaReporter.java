package io.github.hengyunabc.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * class WavefrontKafkaReporter which is the extension of
 * KafkaReporter, but not by extension. The code was simply copied from
 * KafkaReporter.java, as KafkaReport does not have any public constructor of its own.
 * 
 * new features:
 * - added withPointTag method in the builder to accommodate the point tags for wavefront
 * 
 * @author yooh
 *
 */
public class WavefrontKafkaReporter extends ScheduledReporter
{
	private static final Logger logger = LoggerFactory
			.getLogger(WavefrontKafkaReporter.class);

	// when set to true, it will output the string into standard output, and will not use
	// kafka.
	private static final boolean stdout = false;
	
	String topic;
	ProducerConfig config;
	Producer<String, String> producer;
	ExecutorService kafkaExecutor;
	
	// point tags
	private Map<String, String> pointTags;

	private String prefix;
	private String hostName;
	private String ip;

	int count = 0;
	
	ObjectMapper mapper;

	private WavefrontKafkaReporter(MetricRegistry registry, String name,
			TimeUnit rateUnit, TimeUnit durationUnit, boolean showSamples, MetricFilter filter,
			String topic, ProducerConfig config, String prefix,
			String hostName, String ip, Map<String, String>pointTags) 
	{
		super(registry, name, filter, rateUnit, durationUnit);
		this.topic = topic;
		this.config = config;
		this.prefix = prefix;
		this.hostName = hostName;
		this.pointTags = pointTags;
		this.ip = ip;
		
		this.mapper = new ObjectMapper().registerModule(new MetricsModule(rateUnit,
                durationUnit,
                showSamples));

		if(!stdout)
		{
			producer = new Producer<String, String>(config);
	
			kafkaExecutor = Executors
					.newSingleThreadExecutor(new ThreadFactoryBuilder()
							.setNameFormat("kafka-producer-%d").build());
		}
	}

	public static Builder forRegistry(MetricRegistry registry) {
		return new Builder(registry);
	}

	public static class Builder 
	{
		private final MetricRegistry registry;
		private String name = "kafka-reporter";
		private TimeUnit rateUnit;
		private TimeUnit durationUnit;
		private boolean showSamples;
		private MetricFilter filter;
		private String prefix = "";
		private String hostName;
		private String ip;
		private String topic;
		private ProducerConfig config;
		private Map<String, String> pointTags;

		public Builder(MetricRegistry registry) {
			this.registry = registry;

			this.rateUnit = TimeUnit.SECONDS;
			this.durationUnit = TimeUnit.MILLISECONDS;
			this.filter = MetricFilter.ALL;
			this.pointTags = new HashMap<String, String>();
		}

		/**
	     * Set the Point Tags for this reporter.
	     *
	     * @param pointTags the pointTags Map for all metrics
	     * @return {@code this}
	     */
	    public Builder withPointTags(Map<String, String> pointTags) {
	      this.pointTags.putAll(pointTags);
	      return this;
	    }

	    /**
	     * Set a point tag for this reporter.
	     *
	     * @param ptagK the key of the Point Tag
	     * @param ptagV the value of the Point Tag
	     * @return {@code this}
	     */
	    public Builder withPointTag(String ptagK, String ptagV) {
	      this.pointTags.put(ptagK, ptagV);
	      return this;
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
		
		public Builder showSamples(boolean showSamples) {
			this.showSamples = showSamples;
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
		public WavefrontKafkaReporter build() {
			if (hostName == null) {
				hostName = HostUtil.getHostName();
				logger.info(name + " detect hostName: " + hostName);
			}
			if (ip == null) {
				ip = HostUtil.getHostAddress();
				logger.info(name + " detect ip: " + ip);
			}
			return new WavefrontKafkaReporter(registry, name, rateUnit, durationUnit, showSamples,
					filter, topic, config, prefix, hostName, ip, pointTags);
		}
	}

	private Map<String, Object> addPrefix(SortedMap<String,?> map){
		Map<String, Object> result = new HashMap<String, Object>(map.size());
		for (Entry<String, ?> entry : map.entrySet()) {
			result.put(prefix + entry.getKey(), entry.getValue());
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges,
			SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms,
			SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		
		final Map<String, Object> result = new HashMap<String, Object>(16);
		
		result.put("hostName", hostName);
		result.put("ip", ip);
		result.put("rateUnit", getRateUnit());
		result.put("durationUnit", getDurationUnit());
		result.put("pointTags", pointTags);				// add global pointTag
		
		result.put("gauges", addPrefix(gauges));
		result.put("counters", addPrefix(counters));
		result.put("histograms", addPrefix(histograms));
		result.put("meters", addPrefix(meters));
		result.put("timers", addPrefix(timers));
		
		result.put("clock", System.currentTimeMillis());
		
		if(!stdout)
		{
			kafkaExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
					KeyedMessage<String, String> message = new KeyedMessage<String, String>(
							topic, "" + count++, mapper.writeValueAsString(result));
						producer.send(message);
					} catch (Exception e) {
						logger.error("send metrics to kafka error!", e);
					}
				}
			});
		}
		else
		{
			// just pring out the mapper's output
			try
			{
				System.out.println(mapper.writeValueAsString(result));
			}
			catch(Exception e)
			{
				logger.error("send metrics to kafka error!", e);
			}
		}
	}
}
