package org.das2.graph;

public final class CustomizerKey implements Comparable<CustomizerKey> {
	public static CustomizerKey of(String label) {
		return new CustomizerKey(label);
	}

	private final String label;

	private CustomizerKey(String label) {
		this.label = label;
	}

	@Override
	public int compareTo(CustomizerKey other) {
		return label.compareTo(other.label);
	}

	@Override
	public String toString() {
		return label;
	}
}
