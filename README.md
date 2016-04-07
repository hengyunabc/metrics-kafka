# metrics-kafka
Dropwizard Metrics reporter for kafka.


https://github.com/dropwizard/metrics

Report json metrics data to kafka. Kafka comsumer can process metrics data.


## Example

### Environment Setup

http://kafka.apache.org/082/documentation.html#quickstart

### Reporter

```java
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

import io.github.hengyunabc.metrics.KafkaReporter;
import kafka.producer.ProducerConfig;

public class KafkaReporterSample {
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
		String topic = "test-kafka-reporter";
		Properties props = new Properties();
		props.put("metadata.broker.list", "127.0.0.1:9092");
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
    "timers": {
        "test.test-timer": {
            "count": 43,
            "max": 505.33599999999996,
            "mean": 502.585391215306,
            "min": 500.191,
            "p50": 502.443,
            "p75": 504.046,
            "p95": 505.291,
            "p98": 505.33599999999996,
            "p99": 505.33599999999996,
            "p999": 505.33599999999996,
            "stddev": 1.6838970975560197,
            "m15_rate": 0.8076284847453551,
            "m1_rate": 0.8883929708459906,
            "m5_rate": 0.8220236458023953,
            "mean_rate": 0.9799289583409866,
            "duration_units": "milliseconds",
            "rate_units": "calls/second"
        }
    },
    "durationUnit": "milliseconds",
    "meters": {},
    "clock": 1453287302764,
    "hostName": "localhost",
    "rateUnit": "second",
    "histograms": {
        "test.response-sizes": {
            "count": 43,
            "max": 142,
            "mean": 123.29413148075862,
            "min": 100,
            "p50": 124,
            "p75": 134,
            "p95": 141,
            "p98": 142,
            "p99": 142,
            "p999": 142,
            "stddev": 12.28197980813012
        }
    },
    "counters": {},
    "gauges": {
        "test.jvm.mem.pools.Code-Cache.used": {
            "value": 769088
        },
        "test.jvm.mem.pools.Code-Cache.usage": {
            "value": 0.015280405680338541
        },
        "test.jvm.mem.heap.committed": {
            "value": 128974848
        },
        "test.jvm.mem.pools.PS-Old-Gen.usage": {
            "value": 0.00048653738839285715
        },
        "test.jvm.mem.non-heap.used": {
            "value": 17222048
        },
        "test.jvm.gc.PS-MarkSweep.count": {
            "value": 0
        },
        "test.jvm.mem.pools.Code-Cache.init": {
            "value": 2555904
        },
        "test.jvm.mem.pools.PS-Survivor-Space.usage": {
            "value": 0.99683837890625
        },
        "test.jvm.mem.pools.PS-Eden-Space.max": {
            "value": 705691648
        },
        "test.jvm.mem.pools.PS-Perm-Gen.init": {
            "value": 22020096
        },
        "test.jvm.mem.total.init": {
            "value": 158793728
        },
        "test.jvm.mem.heap.max": {
            "value": 1908932608
        },
        "test.jvm.mem.heap.init": {
            "value": 134217728
        },
        "test.jvm.mem.pools.PS-Eden-Space.usage": {
            "value": 0.039622597318878856
        },
        "test.jvm.mem.pools.PS-Survivor-Space.used": {
            "value": 5226304
        },
        "test.jvm.mem.pools.Code-Cache.committed": {
            "value": 2555904
        },
        "test.jvm.mem.pools.PS-Old-Gen.committed": {
            "value": 89128960
        },
        "test.jvm.mem.non-heap.max": {
            "value": 136314880
        },
        "test.jvm.gc.PS-Scavenge.count": {
            "value": 1
        },
        "test.jvm.mem.pools.PS-Survivor-Space.init": {
            "value": 5242880
        },
        "test.jvm.mem.pools.PS-Perm-Gen.committed": {
            "value": 22020096
        },
        "test.jvm.mem.pools.PS-Eden-Space.used": {
            "value": 27961336
        },
        "test.jvm.mem.pools.PS-Old-Gen.used": {
            "value": 696384
        },
        "test.jvm.mem.pools.Code-Cache.max": {
            "value": 50331648
        },
        "test.jvm.mem.pools.PS-Perm-Gen.usage": {
            "value": 0.19135079732755336
        },
        "test.jvm.mem.total.committed": {
            "value": 153550848
        },
        "test.jvm.mem.non-heap.init": {
            "value": 24576000
        },
        "test.jvm.mem.pools.PS-Eden-Space.committed": {
            "value": 34603008
        },
        "test.jvm.mem.total.max": {
            "value": 2045247488
        },
        "test.jvm.mem.pools.PS-Survivor-Space.committed": {
            "value": 5242880
        },
        "test.jvm.gc.PS-MarkSweep.time": {
            "value": 0
        },
        "test.jvm.mem.heap.used": {
            "value": 33884024
        },
        "test.jvm.mem.heap.usage": {
            "value": 0.017750246319853318
        },
        "test.jvm.mem.pools.PS-Perm-Gen.max": {
            "value": 85983232
        },
        "test.jvm.mem.pools.PS-Survivor-Space.max": {
            "value": 5242880
        },
        "test.jvm.mem.pools.PS-Old-Gen.init": {
            "value": 89128960
        },
        "test.jvm.mem.total.used": {
            "value": 51106240
        },
        "test.jvm.mem.pools.PS-Perm-Gen.used": {
            "value": 16453128
        },
        "test.jvm.mem.pools.PS-Eden-Space.init": {
            "value": 34603008
        },
        "test.jvm.mem.non-heap.committed": {
            "value": 24576000
        },
        "test.jvm.gc.PS-Scavenge.time": {
            "value": 19
        },
        "test.jvm.mem.pools.PS-Old-Gen.max": {
            "value": 1431306240
        },
        "test.jvm.mem.non-heap.usage": {
            "value": 0.12634142362154446
        }
    },
    "ip": "192.158.1.113"
}
```

### KafkaConsumer

```java
import java.io.IOException;

import io.github.hengyunabc.metrics.MessageListener;
import io.github.hengyunabc.metrics.MetricsKafkaConsumer;

public class MetricsKafkaConsumerSample {

	String zookeeper;
	String topic;
	String group;

	MetricsKafkaConsumer consumer;

	public static void main(String[] args) throws IOException {

		String zookeeper = "localhost:2181";
		String topic = "test-kafka-reporter";
		String group = "consumer-test";

		MetricsKafkaConsumer consumer = new MetricsKafkaConsumer();

		consumer = new MetricsKafkaConsumer();
		consumer.setZookeeper(zookeeper);
		consumer.setTopic(topic);
		consumer.setGroup(group);
		consumer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(String message) {
				System.err.println(message);
			}
		});
		consumer.init();

		System.in.read();

		consumer.desotry();
	}
}
```

## Maven dependency

```xml
<dependency>
    <groupId>io.github.hengyunabc</groupId>
    <artifactId>metrics-kafka</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Others

https://github.com/hengyunabc/zabbix-api

https://github.com/hengyunabc/zabbix-sender

https://github.com/hengyunabc/metrics-zabbix

https://github.com/hengyunabc/kafka-zabbix

## License

Apache License V2
