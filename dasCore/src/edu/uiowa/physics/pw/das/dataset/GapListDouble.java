/* File: DoubleList.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on December 30, 2003, 4:26 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.dataset;

/**
 *
 * @author  Edward West
 */
class GapListDouble {

    private static final int INITIAL_ARRAY_SIZE = 128;
    private double[] array = new double[INITIAL_ARRAY_SIZE];
    private int gapStart = 0;
    private int gapEnd = INITIAL_ARRAY_SIZE;

    GapListDouble() {
    }

    public int add(double d) {
        int index = indexOf(d);
        if (index < 0) {
            index = ~index;
        }
        if (isFull()) {
            resizeArray();
        }
        if (index != gapStart) {
            moveGap(index);
        }
        array[gapStart] = d;
        gapStart++;
        return index;
    }
    
    public double get(int index) {
        if (index < gapStart) {
            return array[index];
        }
        else {
            return array[index + (gapEnd - gapStart)];
        }
    }
    
    /* TODO: document output: what is ~? */
    public int indexOf(double d) {
        if (gapStart != 0 && array[gapStart - 1] >= d) {
            return binarySearch(d, array, 0, gapStart);
        }
        else if (gapEnd != array.length && array[gapEnd] < d) {
            int index = binarySearch(d, array, gapEnd, array.length);
            if (index >= 0) {
                return index - (gapEnd - gapStart);
            }
            else {
                return ~( ~index - (gapEnd - gapStart));
            }
        }
        else {
            return ~gapStart;
        }
    }
    
    public boolean isEmpty() {
        return gapStart == 0 && gapEnd == array.length;
    }
    
    private boolean isFull() {
        return gapStart == gapEnd;
    }
    
    public int size() {
        return gapStart + array.length - gapEnd;
    }
    
    public double[] toArray() {
        double[] out = new double[size()];
        System.arraycopy(array, 0, out, 0, gapStart);
        System.arraycopy(array, gapEnd, out, gapStart, array.length - gapEnd);
        return out;
    }
    
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuffer buffer = new StringBuffer("[");
        int size = size();
        for (int i = 0; i < size-1; i++) {
            buffer.append(get(i)).append(", ");
        }
        buffer.append(get(size - 1)).append("]");
        return buffer.toString();
    }
    
    private void resizeArray() {
        double[] temp = new double[array.length << 1];
        System.arraycopy(array, 0, temp, 0, gapStart);
        int l2 = array.length - gapEnd;
        System.arraycopy(array, gapEnd, temp, temp.length - l2, l2);
        array = temp;
        gapEnd = temp.length - l2;
    }
    
    private void moveGap(int position) {
        if (position < gapStart) {
            int chunkSize = gapStart - position;
            int gapSize = gapEnd - gapStart;
            System.arraycopy(array, position, array, position + gapSize, chunkSize);
            gapStart = position;
            gapEnd = gapStart + gapSize;
        }
        else if (position > gapStart) {
            int chunkSize = position - gapStart;
            int gapSize = gapEnd - gapStart;
            System.arraycopy(array, gapEnd, array, gapStart, chunkSize);
            gapStart = position;
            gapEnd = gapStart + gapSize;
        }
    }
    
    private static int binarySearch(final double d, final double[] array, final int start, final int end) {
	int low = start;
	int high = end-1;
	while (low <= high) {
	    int mid = (low + high) >> 1;
	    if (array[mid] < d) {
		low = mid + 1;
            }
	    else if (array[mid] > d) {
		high = mid - 1;
            }
	    else {
		return mid; 
            }
	}
	return ~low;
    }
    
}