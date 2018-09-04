package org.das2.graph;

import java.util.Objects;

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
	public boolean equals(Object obj) {
		if ( obj instanceof CustomizerKey ) {
                    return label.equals(((CustomizerKey)obj).label);
                } else {
                    return false;
                }
	}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.label);
        return hash;
    }
        
	@Override
	public String toString() {
		return label;
	}
}
