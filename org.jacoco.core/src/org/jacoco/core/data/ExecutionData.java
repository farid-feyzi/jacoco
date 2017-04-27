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

import java.io.IOException;

import org.jacoco.core.internal.data.CompactDataInput;
import org.jacoco.core.internal.data.CompactDataOutput;
import org.jacoco.core.internal.instr.IInstrSupport;

/**
 * Execution data for a single Java class. While instances are immutable care
 * has to be taken about the probe data array of type <code>boolean[]</code>
 * which can be modified.
 */
public final class ExecutionData {
	/**
	 * dataType indicates which type to be used to store/the strategy to collect probes
	 * whether to only mark covered (use Boolean array) or count the covered frequency (use Integer array).
	 */
	private static ProbesType probesType = ProbesType.INTEGER; // as default
	private IExecutionData execData;

	/**
	 * Creates a new {@link ExecutionData} object with the given probe data.
	 * 
	 * @param id
	 *            class identifier
	 * @param name
	 *            VM name
	 * @param probes
	 *            probe data
	 */
	@Deprecated
	public ExecutionData(final long id, final String name,
			final boolean[] probes) {
		execData = new OrgExecutionData(id, name, probes);
	}

	/**
	 * Creates a new {@link ExecutionData} object with the given probe data
	 * length. All probes are set to <code>false</code>.
	 * 
	 * @param id
	 *            class identifier
	 * @param name
	 *            VM name
	 * @param probeCount
	 *            probe count
	 */
	public ExecutionData(final long id, final String name,
			final int probeCount) {
		switch (probesType) {
		case INTEGER:
			execData = new ExtExecutionData(id, name, probeCount);
			break;
		default:
			execData = new OrgExecutionData(id, name, probeCount);
			break;
		}
	}

	/**
	 * create an ExecutionData wrapper from data
	 * 
	 * @param execData the execution data needs to be wrapped.
	 */
	public ExecutionData(final IExecutionData execData) {
		this.execData = execData;
	}

	/**
	 * Return the unique identifier for this class. The identifier is the CRC64
	 * checksum of the raw class file definition.
	 * 
	 * @return class identifier
	 */
	public long getId() {
		return execData.getId();
	}

	/**
	 * The VM name of the class.
	 * 
	 * @return VM name
	 */
	public String getName() {
		return execData.getName();
	}

	/**
	 * Returns the execution data probes. A value of <code>true</code> indicates
	 * that the corresponding probe was executed.
	 * 
	 * @return probe data
	 */
	public boolean[] getProbes() {
		return execData.getProbes();
	}

	/**
	 * Sets all probes to <code>false</code>.
	 */
	public void reset() {
		execData.reset();
	}

	/**
	 * Checks whether any probe has been hit.
	 * 
	 * @return <code>true</code>, if at least one probe has been hit
	 */
	public boolean hasHits() {
		return execData.hasHits();
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
	public void merge(final ExecutionData other) {
		execData.merge(other.execData);
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
	 * For <code>flag==true</code> this can be considered as a subtraction
	 * 
	 * <pre>
	 * A and not B
	 * </pre>
	 * 
	 * The probe array of the other object is not modified.
	 * 
	 * @param other
	 *            execution data to merge
	 * @param flag
	 *            merge mode
	 */
	public void merge(final ExecutionData other, final boolean flag) {
		execData.merge(other.execData, flag);
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
			final int probecount) throws IllegalStateException {
		execData.assertCompatibility(id, name, probecount);
	}

	@Override
	public String toString() {
		return execData.toString();
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
	public static ExecutionData read(final CompactDataInput in)
			throws IOException {
		final IExecutionData execData;
		if (probesType == ProbesType.INTEGER) {
			execData = ExtExecutionData.read(in);
		} else {
			execData = OrgExecutionData.read(in);
		}

		return new ExecutionData(execData);
	}

	/**
	 * get the real data need to write in instrumented code.
	 * 
	 * @return the probes object of execution data (boolean[] by default)
	 */
	public Object getRawProbes() {
		return execData.getRawProbes();
	}

	/**
	 * Write execution data section
	 * 
	 * @param out output stream to write execution data.
	 * @throws IOException might be thrown by the underlying output stream
	 */
	public void write(final CompactDataOutput out) throws IOException {
		execData.write(out);
	}

	/**
	 * @return specific instrumentation supporter which create instrumentation
	 *         byte code base on execution data structure.
	 */
	public static IInstrSupport getInstrSupport() {
		if (probesType == ProbesType.INTEGER) {
			return ExtExecutionData.getInstrSupport();
		}
		return OrgExecutionData.getInstrSupport();
	}
	
	/**
	 * @param probesType the probesType to set
	 */
	public static void setProbesType(final ProbesType probesType) {
		ExecutionData.probesType = probesType;
	}

	/**
	 * @author LLT
	 *
	 */
	public static enum ProbesType {
		/**
		 * the original one of jacoco which store probes as a boolean array.
		 */
		BOOLEAN,
		/**
		 * the extended one which store probes as a integer array.
		 */
		INTEGER
	}

}
