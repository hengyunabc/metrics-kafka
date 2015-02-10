# metrics-kafka
Dropwizard Metrics reporter for kafka.


https://github.com/dropwizard/metrics

Report json metrics data to kafka. Kafka comsumer can process metrics data.


## Example

```java
import io.github.hengyunabc.metrics.KafkaReporter;

import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import kafka.producer.ProducerConfig;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

public class GetStarted {
	static final MetricRegistry metrics = new MetricRegistry();
	static public Timer timer = new Timer();

	public static void main(String args[]) throws IOException,
			InterruptedException {
		System.err.println(Float.MIN_VALUE);
		System.in.read();
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

		String hostName = "192.168.66.30";
		String topic = "test-kafka-reporter";
		Properties props = new Properties();
		props.put("metadata.broker.list", "192.168.90.147:9091");
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		props.put("partitioner.class", "kafka.producer.DefaultPartitioner");
		props.put("request.required.acks", "1");

		String prefix = "test.";
		ProducerConfig config = new ProducerConfig(props);
		KafkaReporter kafkaReporter = KafkaReporter.forRegistry(metrics)
				.config(config).topic(topic).hostName(hostName).prefix(prefix).build();

		kafkaReporter.start(1, TimeUnit.SECONDS);

		TimeUnit.SECONDS.sleep(500);
	}
}
```

The json send to kafka will like this:
```json
{
    "clock": 1423559200818, 
    "couters": { }, 
    "durationUnit": "milliseconds", 
    "gauges": {
        "test.jvm.gc.PS-MarkSweep.count": 0, 
        "test.jvm.gc.PS-MarkSweep.time": 0, 
        "test.jvm.gc.PS-Scavenge.count": 1, 
        "test.jvm.gc.PS-Scavenge.time": 12, 
        "test.jvm.mem.heap.committed": 124780544, 
        "test.jvm.mem.heap.init": 130023424, 
        "test.jvm.mem.heap.max": 1840250880, 
        "test.jvm.mem.heap.usage": 0.00920082374855324, 
        "test.jvm.mem.heap.used": 16931824, 
        "test.jvm.mem.non-heap.committed": 20971520, 
        "test.jvm.mem.non-heap.init": 2555904, 
        "test.jvm.mem.non-heap.max": -1, 
        "test.jvm.mem.non-heap.usage": -20478648, 
        "test.jvm.mem.non-heap.used": 20478648, 
        "test.jvm.mem.pools.Code-Cache.usage": 0.014409128824869792, 
        "test.jvm.mem.pools.Compressed-Class-Space.usage": 0.002074204385280609, 
        "test.jvm.mem.pools.Metaspace.usage": 0.9787918893914473, 
        "test.jvm.mem.pools.PS-Eden-Space.usage": 0.016070189299406828, 
        "test.jvm.mem.pools.PS-Old-Gen.usage": 0.0005766570024577318, 
        "test.jvm.mem.pools.PS-Survivor-Space.usage": 0.9950164794921875, 
        "test.jvm.mem.total.committed": 145752064, 
        "test.jvm.mem.total.init": 132579328, 
        "test.jvm.mem.total.max": 1840250879, 
        "test.jvm.mem.total.used": 37410472
    }, 
    "histograms": {
        "test.response-sizes": {
            "75": 115, 
            "95": 119, 
            "98": 119, 
            "99": 119, 
            "99.9": 119, 
            "max": 119, 
            "mean": 109.99800160822247, 
            "median": 110, 
            "min": 100, 
            "stddev": 5.753306437970662
        }
    }, 
    "hostName": "192.168.66.30", 
    "ip": "127.0.0.1", 
    "meters": { }, 
    "reteUnit": "second", 
    "timers": {
        "test.test-timer": {
            "75": 500.28702699999997, 
            "95": 500.33728099999996, 
            "98": 509.056768, 
            "99": 509.056768, 
            "1-minuteRate": 0.8442398433857191, 
            "15-minuteRate": 0.8033057092356766, 
            "5-minuteRate": 0.8097541150998573, 
            "99.9": 509.056768, 
            "count": 20, 
            "max": 509.056768, 
            "mean": 500.6276814381385, 
            "meanRate": 0.9656900307647309, 
            "median": 500.25942499999996, 
            "min": 500.134142, 
            "stddev": 1.7917969316252285
        }
    }
}
```
