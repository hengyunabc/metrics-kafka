package io.github.hengyunabc.metrics.test;

import io.github.hengyunabc.metrics.KafkaReporter;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import kafka.producer.ProducerConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.codahale.metrics.MetricRegistry;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-test.xml")
public class SpringTest {
	
	@Autowired
	AnnotationObject annotationObject;
	
	@Autowired
	MetricRegistry metrics;
	
	@Before
	public void before(){
		startKafkaReporter();
	}
	
	public void startKafkaReporter(){
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

		kafkaReporter.start(3, TimeUnit.SECONDS);
	}
	
	@Test
	public void test() throws InterruptedException{
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					annotationObject.call();
					annotationObject.userLogin();
				}
			}
		});
		t.start();
		
		TimeUnit.SECONDS.sleep(500);
	}
}
