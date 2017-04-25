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

import org.jacoco.core.internal.data.CompactDataOutput;

/**
 * @author LLT
 *
 */
public interface IExecutionData {

	/**
	 * Return the unique identifier for this class. The identifier is the CRC64
	 * checksum of the raw class file definition.
	 * 
	 * @return class identifier
	 */
	long getId();

	/**
	 * The VM name of the class.
	 * 
	 * @return VM name
	 */
	String getName();

	/**
	 * Returns the execution data probes. A value of <code>true</code> indicates
	 * that the corresponding probe was executed.
	 * 
	 * @return probe data
	 */
	boolean[] getProbes();

	/**
	 * Sets all probes to <code>false</code>.
	 */
	void reset();

	/**
	 * Checks whether any probe has been hit.
	 * 
	 * @return <code>true</code>, if at least one probe has been hit
	 */
	boolean hasHits();

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
	 * @param execData
	 *            execution data to merge
	 */
	void merge(IExecutionData execData);

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
	 * @param execData
	 *            execution data to merge
	 * @param flag
	 *            merge mode
	 */
	void merge(IExecutionData execData, boolean flag);

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
	void assertCompatibility(long id, String name, int probecount);

	/**
	 * Write execution data section
	 * 
	 * @param out output stream to write execution data.
	 * @throws IOException might be thrown by the underlying output stream
	 */
	void write(CompactDataOutput out) throws IOException;

	/**
	 * get the real data need to write in instrumented code.
	 * 
	 * @return the probes object of execution data.
	 */
	Object getRawProbes();

}
