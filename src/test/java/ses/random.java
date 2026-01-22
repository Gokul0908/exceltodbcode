package ses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.netty.util.internal.ThreadLocalRandom;

public class random {
	
	
	
	public static String randomString(int length) {
	    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	    StringBuilder sb = new StringBuilder();

	    for (int i = 0; i < length; i++) {
	        int index = ThreadLocalRandom.current().nextInt(chars.length());
	        sb.append(chars.charAt(index));
	    }
	    return sb.toString();
	}

	public static String randomAlphaNumeric(int length) {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
		}
		return sb.toString();
	}

	public static String randomDOB(int startYear, int endYear) {
		LocalDate start = LocalDate.of(startYear, 1, 1);
		LocalDate end = LocalDate.of(endYear, 12, 31);

		long randomDay = ThreadLocalRandom.current().nextLong(start.toEpochDay(), end.toEpochDay());

		LocalDate randomDate = LocalDate.ofEpochDay(randomDay);

		return randomDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
	}

	public static String randomMobileNumber() {
		// Indian mobile numbers usually start from 6–9
		int firstDigit = ThreadLocalRandom.current().nextInt(6, 10);
		long remaining = ThreadLocalRandom.current().nextLong(1_000_000_000L);

		return firstDigit + String.format("%09d", remaining);
	}
}
