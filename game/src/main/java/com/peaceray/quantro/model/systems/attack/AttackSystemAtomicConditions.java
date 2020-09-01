package com.peaceray.quantro.model.systems.attack;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.model.descriptors.AttackDescriptor;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QUpconvert;

/**
 * THIS CLASS HAS BEEN DEPRECIATED.  USE AttackSystemAtomicConditionsVersioned INSTEAD.
 * 
 * THIS CLASS REMAINS ONLY SO THAT SAVED GAMES PREDATING THE CHANGE CAN BE EFFECTLY LOADED
 * AND CONVERTED TO THE NEW FORMAT.
 * 
 * @author Jake
 *
 */
class AttackSystemAtomicConditions implements Serializable  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3124759759949208046L;
	
	int R, C ;
	
	AttackDescriptor attacksToPerformThisCycle ;
	
	int numIncomingAttackDescriptors ;
	ArrayList<AttackDescriptor> incomingAttackDescriptors ;
	
	int numOutgoingAttackDescriptors ;
	ArrayList<AttackDescriptor> outgoingAttackDescriptors ;
	
	int pseudorandom ;
	
	int pieceType ;
	int pieceColumn ;
	int cascadeNumber ;
	
	int [] clearRowQCombinations ;
	int [] clearRowCascadeNumber ;
	int numClearedRows ;

	
	AttackSystemAtomicConditions( int rows, int cols ) {
		throw new IllegalStateException("This class has been depreciated.  Instantiate AttackSystemAtomicConditionsVersioned instead.") ;
	}
	
	
	AttackSystemAtomicConditions( AttackSystemAtomicConditions asac ) {
		throw new IllegalStateException("This class has been depreciated.  Instantiate AttackSystemAtomicConditionsVersioned instead.") ;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// Write all our info.
		// Write constant information.
		stream.writeInt(R) ;
		stream.writeInt(C) ;
		stream.writeInt(pseudorandom) ;
		
		attacksToPerformThisCycle.writeObject(stream) ;
		
		// Write incoming attack descriptions
		stream.writeInt( this.numIncomingAttackDescriptors ) ;
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ )
			incomingAttackDescriptors.get(i).writeObject(stream) ;		
		
		// Write outgoing attack descriptions
		stream.writeInt( this.numOutgoingAttackDescriptors ) ;
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ )
			outgoingAttackDescriptors.get(i).writeObject(stream) ;
		
		// Write game state information.
		stream.writeInt(pieceType) ;
		stream.writeInt(pieceColumn) ;
		stream.writeInt(cascadeNumber) ;
		
		// Write cleared row information
		stream.writeInt(numClearedRows) ;
		// Write the full array, because why not.
		stream.writeObject(clearRowQCombinations) ;
		stream.writeObject(clearRowCascadeNumber) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read info.
		// Read constant information.
		R = stream.readInt() ;
		C = stream.readInt() ;
		pseudorandom = stream.readInt() ;
		
		attacksToPerformThisCycle = new AttackDescriptor(R,C) ;
		attacksToPerformThisCycle.readObject(stream) ;
		
		// Read incoming attack descriptions
		this.numIncomingAttackDescriptors = stream.readInt() ;
		incomingAttackDescriptors = new ArrayList<AttackDescriptor>( numIncomingAttackDescriptors ) ;
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ ) {
			incomingAttackDescriptors.add( new AttackDescriptor( R, C ) ) ;
			incomingAttackDescriptors.get(i).readObject(stream) ;
		}
		
		// Read outgoing attack descriptions
		this.numOutgoingAttackDescriptors = stream.readInt() ;
		outgoingAttackDescriptors = new ArrayList<AttackDescriptor>( numOutgoingAttackDescriptors ) ;
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ ) {
			outgoingAttackDescriptors.add( new AttackDescriptor( R, C ) ) ;
			outgoingAttackDescriptors.get(i).readObject(stream) ;
		}
		
		// Read game state information.
		pieceType = stream.readInt() ;
		pieceColumn = stream.readInt() ;
		cascadeNumber = stream.readInt() ;
		
		// Read cleared row information
		numClearedRows = stream.readInt() ;
		// Read the full array.
		clearRowQCombinations = (int [])stream.readObject() ;
		clearRowCascadeNumber = (int [])stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
	
	public AttackSystemAtomicConditions qUpconvert() {
		AttackSystemAtomicConditions asac = new AttackSystemAtomicConditions(this) ;
		
		asac.attacksToPerformThisCycle = asac.attacksToPerformThisCycle.qUpconvert() ;
		
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ ) {
			asac.incomingAttackDescriptors.set(i,
					incomingAttackDescriptors.get(i).qUpconvert()) ;
		}
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ ) {
			asac.outgoingAttackDescriptors.set(i,
					outgoingAttackDescriptors.get(i).qUpconvert()) ;
		}
		
		asac.pieceType = PieceCatalog.qUpconvert(pieceType) ;
		
		for ( int i = 0; i < clearRowQCombinations.length; i++ )
			asac.clearRowQCombinations[i] = QUpconvert.upConvert( clearRowQCombinations[i] ) ;
		
		return asac ;
	}
	
}
