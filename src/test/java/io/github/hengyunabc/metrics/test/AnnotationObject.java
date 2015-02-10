package io.github.hengyunabc.metrics.test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.annotation.Timed;
import com.ryantenney.metrics.annotation.Counted;

public class AnnotationObject {

	Random random = new Random();

	/**
	 * stat call times and time.
	 */
	@Timed
	public void call() {
		try {
			TimeUnit.MILLISECONDS.sleep(random.nextInt(3000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * stat user login times.
	 */
	@Counted
	public void userLogin(){
		
	}
}
