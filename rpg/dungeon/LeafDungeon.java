package rpg.dungeon;

import rpg.exceptions.*;
import rpg.square.*;
import rpg.util.*;

import be.kuleuven.cs.som.annotate.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A class representing a 'leaf' dungeon that consists of squares.
 *
 * @invar
 *   | canHaveSquaresAtTheirCoordinates()
 *
 * @author Roald Frederickx
 */
public abstract class LeafDungeon<S extends Square> extends Dungeon<S> {

    /** 
     * Create a new leaf dungeon with the given coordinate system as its 
     * coordinate system. 
     * 
     * @param coordinateSystem
     * The coordinate system of this new leaf dungeon.
     * @effect
     *   | super(coordinateSystem)
     */
    @Raw @Model
    protected LeafDungeon(CoordinateSystem coordinateSystem)
                                        throws IllegalArgumentException {
        super(coordinateSystem);
    }

    /** 
     * Returns wheter or not this dungeon contains the given square.
     */
    @Basic @Raw @Override
    public boolean hasSquare(Square square) {
        return squares.containsValue(square);
    }

    /** 
     * Returns the square at the given coordinate in this dungeon.
     */
    @Basic @Raw @Override
    public S getSquareAt(Coordinate coordinate) 
                                    throws IllegalArgumentException,
                                            CoordinateNotOccupiedException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();

        S result = getSquareAtRaw(coordinate);
        if (result == null)
            throw new CoordinateNotOccupiedException(coordinate, this);

        return result;
    }

    /** 
     * Returns the square at the given coordinate in this dungeon, or null 
     * if there is no such square.
     *
     * @pre
     *   | isEffectiveCoordinate(coordinate)
     */
    @Basic @Raw
    private S getSquareAtRaw(Coordinate coordinate) {
        assert isEffectiveCoordinate(coordinate);

        return squares.get(coordinate);
    }

    /** 
     * Add the given square to this leaf dungeon at the given coordinate.
     *
     * @param coordinate 
     * The coordinate to add the given square at.
     * @param square 
     * The square to add at the given coordinate.
     * @effect
     * The squares that border the given coordinate in this dungeon get 
     * merged with the given square in the appropriate direction.
     *   | for each e in getRootDungeon().getDirectionsAndNeighboursOf(
     *   |                                          coordinate).entrySet() :
     *   |      square.mergeWith(e.getValue(), e.getKey())
     * @post
     *   | new.getSquareAt(coordinate).equals(square)
     * @throws IllegalArgumentException
     *   | !canHaveAsSquareAt(coordinate, square)
     * @throws CoordinateOccupiedException
     *   | isOccupied(coordinate)
     * @throws DungeonConstraintsException
     * Adding the given square at the given coordinate would violate the 
     * constrainst as specified by squaresSatisfyConstraints().
     */
    public void addSquareAt(Coordinate coordinate, S square) 
                                        throws IllegalArgumentException,
                                                CoordinateOccupiedException,
                                                DungeonConstraintsException {
        if (!canHaveAsSquareAt(coordinate, square))
            throw new IllegalArgumentException();
        if (isOccupied(coordinate))
            throw new CoordinateOccupiedException(coordinate, this);

        squares.put(coordinate, square);

        if (!squaresSatisfyConstraints()){
            squares.remove(coordinate);
            throw new DungeonConstraintsException(square, this);
        }

        for (Map.Entry<Direction, ? super S> neighbourEntry :
                        getRootDungeon().getDirectionsAndNeighboursOf(
                                                    coordinate).entrySet()) {
            Square neighbour = (Square) neighbourEntry.getValue();
            Direction neighbourDirection = neighbourEntry.getKey();
            square.mergeWith(neighbour, neighbourDirection);
        }
    }

    /** 
     * Checks whether this leaf dungeon can have the given square at the 
     * given coordinate.
     * 
     * @param coordinate 
     * The coordinate to check.
     * @param square 
     * The square to check.
     * @return 
     *   | if (!canPossiblyHaveAsSquareAt(coordinate, square))
     *   |      then result == false
     */
    @Raw
    abstract public boolean canHaveAsSquareAt(Coordinate coordinate, S square);

    /** 
     * Checks whether all squares of this dungeon have valid coordinates.
     * 
     * @return 
     * True iff all squares of this dungeon have valid coordinates.
     *   | result == (for each e in getPositionsAndSquares() :
     *   |                  canHaveAsSquareAt(e.getKey(), e.getValue()))
     */
    public boolean canHaveSquaresAtTheirCoordinates() {
        for (Map.Entry<Coordinate, S> e : getPositionsAndSquares())
            if (!canHaveAsSquareAt(e.getKey(), e.getValue()))
                return false;
        return true;
    }

    /**
     * Checks whether this leaf dungeon can possibly have the given square 
     * at the given coordinate.
     * 
     * @param coordinate 
     * The coordinate to check.
     * @param square 
     * The square to check.
     * @return 
     *   | result == (square != null
     *   |              &amp;&amp; !square.isTerminated()
     *   |              &amp;&amp; isValidSquareCoordinate(coordinate)
     */
    @Raw
    public boolean canPossiblyHaveAsSquareAt(Coordinate coordinate, S square) {
        return square != null
            && !square.isTerminated()
            && isValidSquareCoordinate(coordinate);
    }

    /** 
     * Checks whether the given coordinate is a valid coordinate in this 
     * dungeon.
     *
     * @param coordinate 
     * The coordinate to check.
     * @return 
     * True iff the coordinate is a possible square coordinate for all 
     * dungeons, is contained within the coordinate system of this dungeon, 
     * and the coordinate values in all directions are not equal to each 
     * other.
     *   | result == (isEffectiveCoordinate(coordinate)
     *   |      &amp;&amp; containsCoordinate(coordinate)
     *   |      &amp;&amp; (coordinate.x != coordinate.y
     *   |                  || coordinate.y != coordinate.z
     *   |                  || coordinate.z != coordinate.x))
     */
    public boolean isValidSquareCoordinate(Coordinate coordinate) {
        if (!isEffectiveCoordinate(coordinate))
            return false;
        if (!containsCoordinate(coordinate))
            return false;
        return coordinate.x != coordinate.y
            || coordinate.y != coordinate.z
            || coordinate.z != coordinate.x;
    }

    /** 
     * Check whether the given coordinate lies within this leaf dungeon.
     */
    @Override
    public boolean containsCoordinate(Coordinate coordinate) {
        return getCoordSyst().contains(coordinate);
    }

    /** 
     * Deletes the square at the given coordinate and terminates it.
     */
    @Override
    public void deleteSquareAt(Coordinate coordinate) 
                                    throws IllegalArgumentException,
                                            CoordinateNotOccupiedException {
        S square = getSquareAt(coordinate);
        square.terminate(); //detatches all neighbouring squares too
        squares.remove(coordinate);
    }

    /** 
     * Return the number of squares in this dungeon.
     */
    @Basic @Raw @Override
    public int getNbSquares() {
        if (squares == null)
            return 0;
        return squares.size();
    }

    /**
     * Add the mapping of coordinates to squares of this leaf dungeon to 
     * the given map.
     */
    @Basic @Raw @Override
    public void addSquareMappingTo(Map<Coordinate, ? super S> map) 
                                            throws IllegalStateException {
        if (squares == null)
            throw new IllegalStateException();
        map.putAll(squares);
    }

    /** 
     * Returns whether or not the given coordinate is occupied in this 
     * dungeon.
     */
    @Basic @Override
    public boolean isOccupied(Coordinate coordinate)
                                        throws IllegalArgumentException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();
        return squares.containsKey(coordinate);
    }

    /**
     * Variable referencing a map of the squares of this dungeon
     */
    private Map<Coordinate, S> squares = new HashMap<Coordinate, S>();

    /**
     * Return an iterator of the squares in this leaf dungeon that satisfy 
     * the conditions as imposed by the given filter.
     */
    @Override
    public Iterator<S> getFilteredSquareIterator(
                                    final SquareFilter squareFilter) {
        return new Iterator<S>() {
            private S getNextSquare() {
                S result;
                while (squareIterator.hasNext()) {
                    result = squareIterator.next();
                    if (squareFilter.filter(LeafDungeon.this, result))
                        return result;
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

			private Iterator<S> squareIterator = squares.values().iterator();
            private S next = getNextSquare();
		};
    }

    /** 
     * Return a mapping of directions to squares that represent all 
     * neighbouring squares of the given coordinate in this dungeon. 
     */
    @Raw @Override
    public Map<Direction,S> getDirectionsAndNeighboursOf(Coordinate coordinate)
                                            throws IllegalArgumentException {
        if (!isEffectiveCoordinate(coordinate))
            throw new IllegalArgumentException();

        EnumMap<Direction, S> result =
                    new EnumMap<Direction, S>(Direction.class);

        for (Map.Entry<Direction, Coordinate> neighbourEntry :
                        getCoordSyst().neighboursOf(coordinate).entrySet()) {
            Coordinate neighbourCoordinate = neighbourEntry.getValue();
            Direction neighbourDirection = neighbourEntry.getKey();
            S neighbour = getSquareAtRaw(neighbourCoordinate);
            if (neighbour != null)
                result.put(neighbourDirection, neighbour);
        }

        return result;
    }

    /** 
     * Return a set of all containing leaf dungeons.
     * 
     * @return 
     * A singleton set with this leaf dungeon as only element.
     */
    public Set<LeafDungeon<? extends S>> getContainingLeafDungeons() {
        Set<LeafDungeon<? extends S>> result = 
                                    new HashSet<LeafDungeon<? extends S>>();
        result.add(this);
        return result;
    }

    /** 
     * Translate this leaf dungeon over the given offset.
     */
    @Override
    protected void translate(Coordinate offset)
                                    throws IllegalArgumentException,
                                            CoordinateConstraintsException {
        translateCoordSyst(offset);
        for (Map.Entry<Coordinate, S> e : getPositionsAndSquares())
            if (!canHaveAsSquareAt(e.getKey().add(offset), e.getValue())){
                translateCoordSyst(offset.mirror());
                throw new CoordinateConstraintsException(e.getValue(), this);
            }
        Map<Coordinate, S> translatedSquares = new HashMap<Coordinate, S>();
        for (Map.Entry<Coordinate, S> e : getPositionsAndSquares()) {
            translatedSquares.put(e.getKey().add(offset), e.getValue());
        }
        squares = translatedSquares;
    }

    /** 
     * Terminate this leaf dungeon.
     */
    public void terminate(){
        for (Coordinate coordinate : getSquareMapping().keySet())
            deleteSquareAt(coordinate);
        setIsTerminated();
        if (hasParentDungeon())
            getParentDungeon().deleteSubDungeon(this); 
    }

    /**
     * Check whether this leaf dungeon is not raw.
     */
    @Raw
    @Override
    public boolean isNotRaw() {
        return super.isNotRaw()
                && canHaveSquaresAtTheirCoordinates();
    }
}

// vim: ts=4:sw=4:expandtab:smarttab

