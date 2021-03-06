package rpg.dungeon;

import rpg.exceptions.*;
import rpg.square.Square;
import rpg.util.*;

import be.kuleuven.cs.som.annotate.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A class representing a composite dungeon, composed of other dungeons.
 *
 * @invar
 *   | hasProperSubDungeons()
 *
 * @author Roald Frederickx
 */
public class CompositeDungeon<S extends Square> extends Dungeon<S> {

    /** 
     * Create a new composite dungeon with the given coordinate system as 
     * its coordinate system.
     *
     * @param coordSyst 
     *
     * @effect
     *   | super(coordSyst)
     */
    public CompositeDungeon(CoordinateSystem coordSyst)
                                    throws IllegalArgumentException {
        super(coordSyst);
    }

    /**
     * Return the set of direct subdungeons for this composite dungeon.
     */
    @Basic @Raw
    public Set<Dungeon<? extends S>> getSubDungeons() {
        if (getSubDungeonsRaw() == null)
            return null;
        Set<Dungeon<? extends S>> result = new HashSet<Dungeon<? extends S>>();
        result.addAll(getSubDungeonsRaw());
        return result;
    }

    /**
     * Return the set of direct subdungeons for this composite dungeon, do 
     * not pass this to the end user or demons may fly out of your nose.
     */
    @Basic @Raw
    private Set<Dungeon<? extends S>> getSubDungeonsRaw() {
        return subDungeons;
    }

    /** 
     * Check whether this composite dungeon has the given dungeon as its 
     * direct subdungeon.
     *
     * @param dungeon 
     * The dungeon to check.
     * @return 
     *   | if (getSubDungeons() == null)
     *   |      then result == false
     *   |      else result == (for some subDungeon in getSubDungeons() :
     *   |                          subDungeon == dungeon)
     */
    @Raw
    public boolean hasAsSubDungeon(Dungeon<?> dungeon) {
        if (getSubDungeonsRaw() == null)
            return false;
        return getSubDungeonsRaw().contains(dungeon);
    }

    /** 
     * Add the given dungeon as a subdungeon in this composite dungeon at 
     * the given offset. 
     * 
     * @param subDungeon 
     * The dungeon to add as a subdungeon.
     * @param offset 
     * The offset at which to add the subdungeon.
     * @post
     *   | new.hasAsSubDungeon(subDungeon)
     * @effect
     *   | subDungeon.setParentDungeon(this)
     * @effect
     *   | mergeSquaresOfSubdungeonWithNeighbours(subDungeon)
     * @throws IllegalArgumentException
     *   | !isEffectiveCoordinate(offset) || subDungeon == null
     * @throws IllegalStateException
     *   | isTerminated() || subDungeon.isTerminated()
     * @throws DungeonAlreadyAssociatedException
     *   | subDungeon.hasParentDungeon()
     * @throws SubDungeonDoesNotFitException
     * The given subdungeon does not fit in this composite dungeon at the 
     * given offset, or it overlaps other subdungeons of this composite 
     * dungeon.
     */
    public void addSubDungeonAt(Coordinate offset,
                                Dungeon<? extends S> subDungeon)
                    throws IllegalArgumentException,
                           IllegalStateException,
                           DungeonAlreadyAssociatedException,
                           SubDungeonDoesNotFitException {
        if (!isEffectiveCoordinate(offset))
            throw new IllegalArgumentException("Non-effective coordinate");
        if (subDungeon == null)
            throw new IllegalArgumentException("Non-effective subDungeon");
        if (isTerminated() || subDungeon.isTerminated())
            throw new IllegalStateException();
        if (subDungeon.hasParentDungeon())
            throw new DungeonAlreadyAssociatedException(subDungeon, this);
        CoordinateSystem translatedCoordSyst = subDungeon.getCoordSyst();
        translatedCoordSyst.translate(offset);
        if (!canExpandSubDungeonTo(null, translatedCoordSyst))
            throw new SubDungeonDoesNotFitException(subDungeon, this);

        subDungeon.translate(offset);
        mergeSquaresOfSubdungeonWithNeighbours(subDungeon);
        subDungeons.add(subDungeon);
        subDungeon.setParentDungeon(this);
    }

    /** 
     * Merge the squares of the given dungeon with all its neighbouring 
     * squares in the dungeon complex of this dungeon.
     *
     * @param subDungeon 
     * The dungeon whose squares to merge with the squares in the dungeon 
     * complex of this dungeon.
     * @effect
     *   | for each enty in subDungeon.getPositionsAndSquares() :
     *   |      mergeSquareWithNeighbours(entry.getValue(), entry.getKey())
     */
    @Raw @Model
    protected void mergeSquaresOfSubdungeonWithNeighbours(
                                        Dungeon<? extends S> subDungeon) {
        for (Map.Entry<Coordinate, ? extends S> ps : 
                                        subDungeon.getPositionsAndSquares())
            mergeSquareWithNeighbours(ps.getValue(), ps.getKey());
    }

    /** 
     * Delete the given subdungeon of this composite dungeon.
     * 
     * @param subDungeon 
     * The subdungeon to delete.
     * @post
     *   | !new.hasAsSubDungeon(subDungeon)
     * @effect
     *   | if (hasAsSubDungeon(subDungeon))
     *   |      then subDungeon.setParentDungeon(null)
     * @effect
     *   | if (hasAsSubDungeon(subDungeon))
     *   |      then subDungeon.terminate()
     * @throws IllegalArgumentException
     *   | subDungeon == null
     * @throws IllegalStateException
     *   | isTerminated()
     */
    public void deleteSubDungeon(@Raw Dungeon<?> subDungeon) 
                    throws IllegalStateException, IllegalArgumentException {
        if (isTerminated())
            throw new IllegalStateException();
        if (subDungeon == null)
            throw new IllegalArgumentException();
        if (!hasAsSubDungeon(subDungeon))
            return;
        if (!subDungeon.isTerminated())
            subDungeon.terminate();
        subDungeons.remove(subDungeon);
        subDungeon.setParentDungeon(null);
    }

    /**
     * Checks whether this composite dungeon has proper subdungeons.
     *
     * @return
     *   | if (getSubDungeons() == null)
     *   |      result == false
     *   |      else result == (for each subDungeon in getSubDungeons() :
     *   |                          isProperSubDungeon(subDungeon)
     */
    @Raw
    public boolean hasProperSubDungeons() {
        if (getSubDungeonsRaw() == null)
            return false;
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            if (!isProperSubDungeon(subDungeon))
                return false;
        return true;
    }

    /** 
     * Check whether a given (possible) subdungeon of this composite 
     * dungeon can be expanded to the given coordinate system without 
     * overlapping other subdungeons of this composite dungeon or 'breaking 
     * out' of this composite dungeon.
     * 
     * @param dungeon
     * The subdungeon to check. May be null, in which case only check if 
     * the given coordinate system fits in this composite dungeon without 
     * overlapping subdungeons or 'breaking out'.
     * @param coordSyst
     * The new dimensions of the subdungeon to check.
     * @pre
     *   | coordSyst != null
     * @return 
     *   | result == (
     *   |      getCoordSyst().contains(coordSyst)
     *   |      &amp;&amp; 
     *   |      (for all subDungeon in getSubDungeons() :
     *   |                subDungeon == dungeon 
     *   |                     || !subDungeon.overlaps(coordSyst)))
     */
    public boolean canExpandSubDungeonTo(Dungeon<?> dungeon, 
                                         CoordinateSystem coordSyst) {
        assert coordSyst != null;

        if (!getCoordSyst().contains(coordSyst))
            return false;
        for (Dungeon<?> subDungeon : getSubDungeonsRaw())
            if (subDungeon != dungeon  &&  subDungeon.overlaps(coordSyst))
                return false;
        return true;
    }

    /** 
     * Checks if the given subdungeon is, or would be, a proper subdungeon 
     * for this composite dungeon.
     * 
     * @param subDungeon 
     * The subdugeon to check.
     * @return 
     *   | (subDungeon != null
     *   |          &amp;&amp; subDungeon.getParentDungeon() == this)
     */
    @Raw
    public boolean isProperSubDungeon(@Raw Dungeon<? extends S> subDungeon) {
        if (subDungeon == null)
            return false;
        return subDungeon.getParentDungeon() == this;
    }
    
    /**
     * Variable registering the set of subdungeons for this composite dungeon.
     */
    private Set<Dungeon<? extends S>> subDungeons = 
                                        new HashSet<Dungeon<? extends S>>();

    /** 
     * Check whether the given coordinate lies within this composite dungeon.
     *
     * @return
     *   | result == (getSubDungeonContaining(coordinate) != null)
     */
    @Override
    public boolean containsCoordinate(Coordinate coordinate) {
        return getSubDungeonContaining(coordinate) != null;
    }

    /** 
     * Return the subdungeon of this composite dungeon that contains the 
     * given coordinate.
     * 
     * @param coordinate
     * The coordinate whose containing dungeon to get.
     * @return
     *   | if (coordinate == null
     *   |          || (for each subDungeon in getSubDungeons() :
     *   |                  !subDungeon.containsCoordinate(coordinate)))
     *   | then result == null
     *   | else hasAsSubDungeon(result)
     *   |      &amp;&amp; result.containsCoordinate(coordinate)
     */
    public Dungeon<? extends S> getSubDungeonContaining(
                                                Coordinate coordinate) {
        if (!getCoordSyst().contains(coordinate))
            return null;
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            if (subDungeon.containsCoordinate(coordinate))
                return subDungeon;
        return null;
    }
    
    /** 
     * Returns whether or not the given coordinate is occupied in this 
     * composite dungeon.
     *
     * @return
     *   | result == (
     *   |     getSubDungeonContaining(coordinate) != null
     *   |     &amp;&amp;
     *   |     getSubDungeonContaining(coordinate).isOccupied(coordinate))
     */
	@Override
	public boolean isOccupied(Coordinate coordinate)
                                            throws IllegalArgumentException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();
        if (getSubDungeonContaining(coordinate) == null)
            return false;
        return getSubDungeonContaining(coordinate).isOccupied(coordinate);
	}

    /** 
     * Returns the square at the given coordinate in this composite dungeon.
     * 
     * @return
     *   | result ==
     *   |      getSubDungeonContaining(coordinate).getSquareAt(coordinate)
     */
	@Override
	public S getSquareAt(Coordinate coordinate)
            throws IllegalArgumentException, CoordinateNotOccupiedException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();
        if (getSubDungeonContaining(coordinate) == null)
            throw new CoordinateNotOccupiedException(coordinate, this);
        return getSubDungeonContaining(coordinate).getSquareAt(coordinate);
	}

    /** 
     * Returns whether or not this composite dungeon contains the given 
     * square.
     *
     * @return
     *   | result == (for some subDungeon in getSubDungeons() :
     *   |                              subDungeon.hasSquare(square))
     */
	@Override
	public boolean hasSquare(Square square) {
        if (square == null)
            return false;
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            if (subDungeon.hasSquare(square))
                return true;
		return false;
	}

    /** 
     * Deletes the square at the given coordinate and terminates it.
     *
     * @effect
     *   | getSubDungeonContaining(coordinate).deleteSquareAt(coordinate)
     */
	@Override
	public void deleteSquareAt(Coordinate coordinate)
			throws IllegalArgumentException, CoordinateNotOccupiedException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();
        if (getSubDungeonContaining(coordinate) == null)
            throw new CoordinateNotOccupiedException(coordinate, this);
        getSubDungeonContaining(coordinate).deleteSquareAt(coordinate);
	}

	/** 
     * Return the number of squares in this composite dungeon.
     *
     * @return
     * The sum of the number of squares in each subdungeon.
	 */
    @Raw @Override
	public int getNbSquares() {
        int result = 0;
        if (getSubDungeonsRaw() == null)
            return 0;
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            result += subDungeon.getNbSquares();
        return result;
	}

    /**
     * Add the mapping of coordinates to squares of this composite dungeon 
     * to the given map.
     *
     * @effect
     *   | if (getSubDungeons() != null)
     *   | then for each subDungeon in getSubDungeons() :
     *   |      subDungeon.addSquareMappingTo(map)
     */
    @Raw @Override
	public void addSquareMappingTo(Map<Coordinate, ? super S> map)
                                            throws IllegalStateException{
        if (getSubDungeonsRaw() == null)
            return;
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            subDungeon.addSquareMappingTo(map);
	}

    /**
     * Return an iterator of the squares in this composite dungeon that 
     * satisfy the conditions as imposed by the given filter.
     */
	@Override
	public Iterator<S> getFilteredSquareIterator(
                                        final SquareFilter squareFilter) {
        return new Iterator<S>() {
            {
                if (getSubDungeonsRaw() == null) {
                    next = null;
                } else {
                    Iterator<Dungeon<? extends S>> subDungeonIterator;
                    subDungeonIterator = getSubDungeonsRaw().iterator();
                    if (!subDungeonIterator.hasNext()) {
                        next = null;
                    } else {
                        this.subDungeonIterator = subDungeonIterator;
                        this.squareIterator = subDungeonIterator.next().
                                        getFilteredSquareIterator(squareFilter);
                        this.next = getNextSquare();
                    }
                }
            }
           
            private S getNextSquare() {
                if (squareIterator.hasNext())
                    return squareIterator.next();
                if (subDungeonIterator.hasNext()) {
                    squareIterator = subDungeonIterator.next().
                                    getFilteredSquareIterator(squareFilter);
                    return getNextSquare();
                }
                return null;
            }

			public boolean hasNext() {
                return next != null;
			}

            public S next() throws NoSuchElementException {
                if (!hasNext())
                    throw new NoSuchElementException();
                S result = next;
                next = getNextSquare();
                return result;
			}

			public void remove() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			private Iterator<? extends S> squareIterator;
            private Iterator<Dungeon<? extends S>> subDungeonIterator;
            private S next;
		};
	}

    /** 
     * Return a set of all containing leaf dungeons.
     * 
     * @return 
     * A set that results from adding the containing leaf dungeons from all 
     * subdungeons of this composite dungeon to an empty set.
     */
    @Override
    public Set<LeafDungeon<? extends S>> getContainingLeafDungeons() {
        Set<LeafDungeon<? extends S>> result =
                                new HashSet<LeafDungeon<? extends S>>();
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw())
            result.addAll(subDungeon.getContainingLeafDungeons());
        return result;
    }

    /** 
     * Return a mapping of directions to squares that represent all 
     * neighbouring squares of the given coordinate in this composite 
     * dungeon. 
     */
    @Raw @Override
    public Map<Direction,S> getDirectionsAndNeighboursOf(Coordinate coordinate)
                                            throws IllegalArgumentException {
        EnumMap<Direction, S> result =
                                new EnumMap<Direction, S>(Direction.class);
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw()) {
            result.putAll(subDungeon.getDirectionsAndNeighboursOf(coordinate));
        }
        return result;
    }

    /** 
     * Translate this composite dungeon over the given offset.
     *
     * @param offset 
     * The offset over which to translate this composite dungeon.
     * @effect
     *   | translateCoordSyst(offset)
     * @effect
     *   | for each subDungeon in getSubDungeons() :
     *   |      subDungeon.translate(offset)
     */
	@Override
	protected void translate(Coordinate offset) 
            throws IllegalArgumentException, CoordinateConstraintsException {
        translateCoordSyst(offset);
        Set<Dungeon<?>> translatedSubDungeons = new HashSet<Dungeon<?>>();
        for (Dungeon<? extends S> subDungeon : getSubDungeonsRaw()) {
            try {
                subDungeon.translate(offset);
                translatedSubDungeons.add(subDungeon);
            } catch (CoordinateConstraintsException cce) {
                //roll back everything that has been done already!
                for (Dungeon<?> translatedSubDungeon : translatedSubDungeons) {
                    try {
                        translatedSubDungeon.translate(offset.mirror());
                    } catch (CoordinateConstraintsException cce2) {
                        assert false;
                    }
                }
                translateCoordSyst(offset.mirror());
                throw cce;
            }
        }
    }

    /**
     * Checks whether the given coordinate system is a valid coordinate 
     * system for this composite dungeon.
     *
     * @return
     *   | result == canPossiblyHaveAsCoordSyst(coordSyst)
     */
    @Raw @Override
    public boolean canHaveAsCoordSyst(CoordinateSystem coordSyst) {
        return canPossiblyHaveAsCoordSyst(coordSyst);
    }

    /** 
     * Terminate this composite dungeon.
     *
     * @effect
     *   | for each subDungeon in getSubDungeons() :
     *   |      subDungeon.terminate()
     */
    @Override
    public void terminate(){
        for (Dungeon<? extends S> subDungeon : getSubDungeons())
            subDungeon.terminate();
        setIsTerminated();
        if (hasParentDungeon())
            getParentDungeon().deleteSubDungeon(this); 
    }

    /**
     * Check whether this composite dungeon is not raw.
     */
    @Raw @Override
    public boolean isNotRaw() {
        return super.isNotRaw()
                && hasProperSubDungeons();
    }
}

// vim: ts=4:sw=4:expandtab:smarttab

