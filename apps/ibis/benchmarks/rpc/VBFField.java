/*
* ################################################################
*
*                            Jem3D: 
* A Java(TM) library for Sequential and Parallel 3D Electromagnetism
* 
*      An object-oriented time domain finite volume solver
*                for the 3D Maxwell equations
*
* Copyright (C) 2003-2004 INRIA/University of Nice-Sophia Antipolis
* Contact: CAIMAN and OASIS Project
*          INRIA, 2004 Rt. des Lucioles,  BP 93
*          F-06902 Sophia Antipolis Cedex
* http://www.inria.fr/caiman/ http://www.inria.fr/oasis
*      
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
* USA
*
*  Initial developer(s):         CAIMAN and OASIS Projects
*                     
*  Contributor(s):
*
* ################################################################
*/
// package caiman.activejem3D.util;

import java.io.Serializable;
/**
 *  sert a transporter un champ local vers une VBF<br>
 * genere par une face virtuelle
 * @author ngama
 * 28 juil. 2003
 */
final
public class VBFField implements Serializable {
	/** index de la VBF jumelle */
	public int indexOfRemoteCopy;
	/** champ a transmettre */
	public double[] field;
	/** sous-domaine destinataire (sert uniquement au VBFFieldCollector local)*/
	transient public int indexOfSubDomain;

	// Require creation of the double[3].
	public VBFField() {
	    field = new double[3];
	}

	/**
	 * Constructeur 
	 * @param field champ a transmettre
	 * @param indexOfRemoteCopy VBF destinataire
	 * @param subDomIndex Sous-domaine destinataire
	 */	
	public VBFField(int subDomIndex,int indexOfRemoteCopy, double[] field) {
		this.indexOfRemoteCopy = indexOfRemoteCopy;
		this.field = field;
		this.indexOfSubDomain=subDomIndex;
	}
}
