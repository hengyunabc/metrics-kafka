package io.github.hengyunabc.metrics.test;

import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class TestProducer {
	public static void main(String[] args) throws InterruptedException {
		long events = 10;
		Random rnd = new Random();

		Properties props = new Properties();
		props.put("metadata.broker.list", "192.168.90.147:9091");
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		props.put("partitioner.class",
				"io.github.hengyunabc.metrics.SimplePartitioner");
		props.put("request.required.acks", "1");

		ProducerConfig config = new ProducerConfig(props);

		Producer<String, String> producer = new Producer<String, String>(config);

		for (long nEvents = 0; nEvents < events; nEvents++) {
			long runtime = new Date().getTime();
			String ip = "192.168.2." + rnd.nextInt(255);
			String msg = runtime + ",www.example.com," + ip;
			KeyedMessage<String, String> data = new KeyedMessage<String, String>(
					"test_page_visits", ip, msg);
			try {
				producer.send(data);
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println(e);
			}

			System.err.println("success:" + nEvents);

			TimeUnit.SECONDS.sleep(2);
		}
		producer.close();
	}
}