/*******************************************************************************
 * Copyright (c) 2009, 2017 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.core.data;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Arrays;

import org.jacoco.core.internal.data.CompactDataInput;
import org.jacoco.core.internal.data.CompactDataOutput;
import org.jacoco.core.internal.instr.ExtInstrSupport;
import org.jacoco.core.internal.instr.IInstrSupport;

/**
 * @author LLT
 *
 */
public class ExtExecutionData implements IExecutionData {
	private static final IInstrSupport instrSupport = new ExtInstrSupport();
	private final long id;
	private final String name;
	private final int[] probes;

	/**
	 * Creates a new {@link OrgExecutionData} object with the given probe data
	 * length. All probes are set to <code>false</code>.
	 * 
	 * @param id
	 *            class identifier
	 * @param name
	 *            VM name
	 * @param probes
	 *            probe count
	 */
	public ExtExecutionData(final long id, final String name,
			final int[] probes) {
		this.id = id;
		this.name = name;
		this.probes = probes;
	}

	/**
	 * Creates a new {@link ExtExecutionData} object with the given probe data
	 * length. All probes are set to <code>false</code>.
	 * 
	 * @param id
	 *            class identifier
	 * @param name
	 *            VM name
	 * @param probeCount
	 *            probe count
	 */
	public ExtExecutionData(final long id, final String name,
			final int probeCount) {
		this.id = id;
		this.name = name;
		this.probes = new int[probeCount];
	}

	/**
	 * Return the unique identifier for this class. The identifier is the CRC64
	 * checksum of the raw class file definition.
	 * 
	 * @return class identifier
	 */
	public long getId() {
		return id;
	}

	/**
	 * The VM name of the class.
	 * 
	 * @return VM name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the execution data probes. A value of <code>true</code> indicates
	 * that the corresponding probe was executed.
	 * 
	 * @return probe data
	 */
	public boolean[] getProbes() {
		if (probes == null) {
			return null;
		}
		final boolean[] result = new boolean[probes.length];
		for (int i = 0; i < probes.length; i++) {
			final boolean cover = probes[i] > 0 ? true : false;
			result[i] = cover;
		}

		return result;
	}

	/**
	 * Sets all probes to <code>false</code>.
	 */
	public void reset() {
		Arrays.fill(probes, 0);
	}

	/**
	 * Checks whether any probe has been hit.
	 * 
	 * @return <code>true</code>, if at least one probe has been hit
	 */
	public boolean hasHits() {
		for (final int probe : probes) {
			if (probe > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Merges the given execution data into the probe data of this object. I.e.
	 * a probe entry in this object is marked as executed (<code>true</code>) if
	 * this probe or the corresponding other probe was executed. So the result
	 * is
	 * 
	 * <pre>
	 * A or B
	 * </pre>
	 * 
	 * The probe array of the other object is not modified.
	 * 
	 * @param other
	 *            execution data to merge
	 */
	public void merge(final IExecutionData other) {
		merge(other, true);
	}

	/**
	 * Merges the given execution data into the probe data of this object. A
	 * probe in this object is set to the value of <code>flag</code> if the
	 * corresponding other probe was executed. For <code>flag==true</code> this
	 * corresponds to
	 * 
	 * <pre>
	 * A or B
	 * </pre>
	 * 
	 * For <code>flag==false</code> this can be considered as a subtraction
	 * 
	 * <pre>
	 * remove count for B in A
	 * </pre>
	 * 
	 * The probe array of the other object is not modified.
	 * 
	 * @param execData
	 *            execution data to merge
	 * @param flag
	 *            merge mode
	 */
	public void merge(final IExecutionData execData, final boolean flag) {
		final ExtExecutionData other = (ExtExecutionData) execData;
		assertCompatibility(other.getId(), other.getName(),
				other.getExtProbes().length);
		final int[] otherData = other.getExtProbes();
		// if join mode
		if (flag) {
			for (int i = 0; i < probes.length; i++) {
				if (otherData[i] > 0) {
					probes[i] += otherData[i];
				}
			}
		} else {
			// if subtraction mode
			for (int i = 0; i < probes.length; i++) {
				if (otherData[i] > 0) {
					probes[i] -= otherData[i];
				}
			}
		}
	}

	/**
	 * Returns the execution data probes. A value of <code>number</code>
	 * indicates the number of times the corresponding probe was executed.
	 * 
	 * @return probe data
	 */
	public int[] getExtProbes() {
		return probes;
	}

	public Object getRawProbes() {
		return getExtProbes();
	}

	/**
	 * Asserts that this execution data object is compatible with the given
	 * parameters. The purpose of this check is to detect a very unlikely class
	 * id collision.
	 * 
	 * @param id
	 *            other class id, must be the same
	 * @param name
	 *            other name, must be equal to this name
	 * @param probecount
	 *            probe data length, must be the same as for this data
	 * @throws IllegalStateException
	 *             if the given parameters do not match this instance
	 */
	public void assertCompatibility(final long id, final String name,
			final int probecount) {
		if (this.id != id) {
			throw new IllegalStateException(
					format("Different ids (%016x and %016x).",
							Long.valueOf(this.id), Long.valueOf(id)));
		}
		if (!this.name.equals(name)) {
			throw new IllegalStateException(
					format("Different class names %s and %s for id %016x.",
							this.name, name, Long.valueOf(id)));
		}
		if (this.probes.length != probecount) {
			throw new IllegalStateException(
					format("Incompatible execution data for class %s with id %016x.",
							name, Long.valueOf(id)));
		}
	}

	/**
	 * read execution data section in input stream and initialize ExecutionData
	 * object.
	 * 
	 * @param in
	 *            must be at pointer of executionData section.
	 * @return the wrapper of the executionData
	 * @throws IOException
	 *             might be thrown by the underlying input stream
	 */
	public static IExecutionData read(final CompactDataInput in)
			throws IOException {
		final long id = in.readLong();
		final String name = in.readUTF();
		final int[] probes = in.readIntArray();
		return new ExtExecutionData(id, name, probes);
	}

	/**
	 * Write execution data section
	 * 
	 * @param out output stream to write execution data.
	 * @throws IOException might be thrown by the underlying output stream
	 */
	public void write(final CompactDataOutput out) throws IOException {
		out.writeLong(getId());
		out.writeUTF(getName());
		out.writeIntArray(getExtProbes());
	}

	/**
	 * @return specific instrumentation supporter which create instrumentation
	 *         byte code, for this extended version of execution data.
	 */
	public static IInstrSupport getInstrSupport() {
		return instrSupport;
	}

}
