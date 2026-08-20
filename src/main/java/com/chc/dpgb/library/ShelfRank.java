package com.chc.dpgb.library;

public final class ShelfRank {

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BASE = ALPHABET.length();
	private static final int MAX_LENGTH = 128;

	private ShelfRank() {
	}

	public static String initial() {
		return String.valueOf(ALPHABET.charAt(BASE / 2));
	}

	public static String before(String next) {
		return between(null, next);
	}

	public static String after(String prev) {
		return between(prev, null);
	}

	public static String between(String prev, String next) {
		if (prev != null && next != null && prev.compareTo(next) >= 0) {
			throw new IllegalArgumentException(
					"prev는 next보다 사전식으로 앞서야 합니다: prev=" + prev + ", next=" + next);
		}

		StringBuilder result = new StringBuilder();
		boolean upperUnbounded = (next == null);
		int i = 0;
		while (true) {
			int low = (prev != null && i < prev.length()) ? ALPHABET.indexOf(prev.charAt(i)) : 0;
			int high = upperUnbounded ? BASE : ((i < next.length()) ? ALPHABET.indexOf(next.charAt(i)) : 0);

			if (high - low > 1) {
				int mid = low + (high - low) / 2;
				result.append(ALPHABET.charAt(mid));
				return result.toString();
			}

			result.append(ALPHABET.charAt(low));
			if (high - low == 1) {
				upperUnbounded = true;
			}
			i++;
			if (result.length() >= MAX_LENGTH) {
				throw new ShelfRankExhaustedException();
			}
		}
	}

	public static String[] rebalancedSequence(int count) {
		if (count <= 0) {
			return new String[0];
		}

		int digitCount = 1;
		long capacity = BASE;
		while (capacity < (long) count * 4) {
			digitCount++;
			capacity *= BASE;
		}

		String[] result = new String[count];
		for (int i = 0; i < count; i++) {
			long position = ((long) (i + 1) * capacity) / (count + 1);
			result[i] = toKey(position, digitCount);
		}
		return result;
	}

	private static String toKey(long position, int digitCount) {
		char[] chars = new char[digitCount];
		long remaining = position;
		for (int i = digitCount - 1; i >= 0; i--) {
			chars[i] = ALPHABET.charAt((int) (remaining % BASE));
			remaining /= BASE;
		}
		int end = chars.length;
		while (end > 1 && chars[end - 1] == ALPHABET.charAt(0)) {
			end--;
		}
		return new String(chars, 0, end);
	}
}
