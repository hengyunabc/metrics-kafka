package io.github.hengyunabc.metrics.test;

import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import io.github.hengyunabc.metrics.WavefrontKafkaReporter;
import kafka.producer.ProducerConfig;

public class WavefrontKafkaReporterSample {
	static final MetricRegistry metrics = new MetricRegistry();
	static public Timer timer = new Timer();

	public static void main(String args[]) throws IOException,
			InterruptedException {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		metrics.register("jvm.mem", new MemoryUsageGaugeSet());
		metrics.register("jvm.gc", new GarbageCollectorMetricSet());

		final Histogram responseSizes = metrics.histogram("response-sizes");
		final com.codahale.metrics.Timer metricsTimer = metrics
				.timer("test-timer");

		timer.schedule(new TimerTask() {
			int i = 100;

			@Override
			public void run() {
				Context context = metricsTimer.time();
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				responseSizes.update(i++);
				context.stop();
			}

		}, 1000, 1000);

		reporter.start(5, TimeUnit.SECONDS);

		String hostName = "localhost";
		String topic = "test-wf-kafka-reporter";
		Properties props = new Properties();
		props.put("metadata.broker.list", "127.0.0.1:9092");
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		props.put("partitioner.class", "kafka.producer.DefaultPartitioner");
		props.put("request.required.acks", "1");

		String prefix = "test.";
		ProducerConfig config = new ProducerConfig(props);
		
		// configure using two point tags
		WavefrontKafkaReporter wavefrontKafkaReporter = WavefrontKafkaReporter.forRegistry(metrics)
				.config(config).topic(topic).hostName(hostName).prefix(prefix).withPointTag("cluster", "c-1").withPointTag("appName", "myapp").build();

		wavefrontKafkaReporter.start(1, TimeUnit.SECONDS);

		TimeUnit.SECONDS.sleep(500);
	}
}