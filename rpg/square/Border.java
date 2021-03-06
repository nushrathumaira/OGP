package rpg.square;

import rpg.exceptions.*;
import rpg.util.Couple;

import be.kuleuven.cs.som.annotate.*;

/**
 * A class of borders that border squares.
 * Note that there is a strong existensial dependency with squares: a 
 * border cannot exist without being linked to at least one square (unless 
 * the border is terminated).
 * 
 * @invar
 * No border has the same square on both sides.
 *   | hasNoDuplicateSquares()
 * @invar
 * Each border has proper associated squares.
 *   | hasProperSquares()
 * @invar
 * No border has terminated squares associated with itself.
 *   | hasNoTerminatedSquares()
 *
 * @author Roald Frederickx
 */
public abstract class Border {
    
    /** 
     * Create a new border that replaces the given border.
     * 
     * @param border 
     * The border to replace with this new border.
     * @pre
     *   | border != null  &amp;&amp;  !border.isTerminated()
     * @post
     *   | (new border).isTerminated()
     * @effect
     *   | for each square in old.border.getSquares() :
     *   |      square.updateBorder(border, this)
     */
    @Raw
    public Border(Border border)
                throws IllegalArgumentException, BorderConstraintsException {
        assert border != null  &&  !border.isTerminated();
        this.squares = new Couple<Square>(border.squares);
        for (Square square : getSquares())
            square.updateBorder(border, this);
    }

    /** 
     * Create a new border that is attached to the given square.
     * 
     * @param square 
     * The square to attach this border to.
     * @post
     *   | new.bordersOnSquare(square)
     * @note
     * Note that this will <b>not</b> initialize the bidirectional 
     * association from the square to this border! It is your duty to make 
     * that link with this newly created border.
     * Hence, the newly created border will still be raw!
     */
    @Raw
    Border(Square square) {
        squares = new Couple<Square>(square);
    }

    /** 
     * Returns whether or not this border is open, thus connecting the 
     * interior of its bordering squares.
     */
    @Basic
    public boolean isOpen(){
        return false;
    }

    /** 
     * Returns whether or not this border is a door.
     */
    @Basic
    public boolean isDoor(){
        return false;
    }

    /** 
     * Returns whether or not this border is a wall.
     */
    @Basic
    public boolean isWall(){
        return false;
    }

    /** 
     * Returns whether or not this border is slippery.
     */
    @Basic
    public boolean isSlippery(){
        return false;
    }

    /** 
     * Merge this border with the given border. 
     * 
     * @param other 
     * The border to merge with this one.
     * @pre
     *   | other != null
     * @pre
     *   | !this.isTerminated() &amp;&amp; !other.isTerminated()
     * @post
     * This old border and the given old border get merged into a single 
     * new border that borders both squares associated with both old 
     * borders. That new border is the least open border of the old 
     * borders. The other border gets updated to that least open border.
     * @return
     * The new border that results from merging this old border and the 
     * given old border.
     * @throws BorderConstraintsException
     * Merging the borders would violate a border constraint.
     * @throws BorderMergeException
     * One of the two involved (non-equal) borders is already shared by two 
     * squares.
     */
    Border mergeWith(Border other) throws BorderConstraintsException,
                                        BorderMergeException {
        assert other != null;
        assert !this.isTerminated() && !other.isTerminated();
        
        if (this.equals(other)  &&  isSharedByTwoSquares())
            return this; //this border is already merged

        if (this.isSharedByTwoSquares() || other.isSharedByTwoSquares()
                        || this.getASquare().equals(other.getASquare()))
            throw new BorderMergeException(this, other);

        //Keep the least open border
        Border newBorder;       //the border to keep
        Border otherBorder;     //the other border
        Square foreignSquare;   //the square that goes with this other border
        if (this.openness() <= other.openness()){
            newBorder = this;
            otherBorder = other;
        } else {
            newBorder = other;
            otherBorder = this;
        }
        foreignSquare = otherBorder.getASquare();
        newBorder.addSquare(foreignSquare);
        foreignSquare.updateBorder(otherBorder, newBorder);

        return newBorder;
        /* Yes, I am aware that I'm doing some complex mutation-like 
         * operations *and* returning a value, but this saves some spurious 
         * code further down the road and returning the value isn't 
         * something new that require any extra calculations anyway. */
    }

    /**
     * Equilibrate the temperatures and humidities of the squares that this 
     * border is attached to.
     *
     * @effect
     * If this border is shared by two squares, then their temperatures and 
     * humidities get merged.
     */
    @Model
    @Deprecated
    protected void equilibrateSquares() 
                        throws MergingTemperaturesViolatesLimitsException {
        if (!isSharedByTwoSquares())
            return;
        Square square1 = getASquare();
        Square square2 = getNeighbour(square1);
        square1.mergeTemperatures(square2);
        square1.mergeHumidities(square2);
    }

    /** 
     * Returns whether or not this border borders on the given square. 
     * 
     * @param square 
     * The square to check.
     * @pre
     *   | square != null
     */
    @Basic
    public boolean bordersOnSquare(Square square) {
        assert square != null;
        return squares.contains(square);
    }

    /** 
     * Returns a square along this border. 
     * 
     * @pre
     *   | !isTerminated()
     */
    @Basic
    public Square getASquare() {
        assert !isTerminated();
        return squares.getAnElement();
    }

    /**
     * Returns an iterable of the squares along this border.
     *
     * @pre
     *   | !isTerminated()
     */
    @Basic
    public Iterable<Square> getSquares() {
        assert !isTerminated();
        return squares;
    }

    /** 
     * Returns the neighbouring square of the given square along this 
     * border. 
     * 
     * @param square 
     * The square to get the neighbour of.
     * @pre
     *   | !isTerminated()
     * @return 
     * Null if the given square is not part of this border, otherwise 
     * return the neighbour, or null if this border has only one assocated 
     * square (the given one).
     */
    @Basic
    public Square getNeighbour(Square square) {
        assert !isTerminated();
        return squares.getPartner(square);
    }

    /** 
     * Check whether this border has no duplicate squares.
     * 
     * @return 
     * If this border is terminated, or if this border is not shared by two 
     * squares, then the result is true. Otherwise, the result is the 
     * negation of the equality of the two associated squares of this 
     * border.
     */
    @Raw
    public boolean hasNoDuplicateSquares() {
        if (isTerminated() || !isSharedByTwoSquares())
            return true;
        if (squares == null)
            return true;
        Square square1 = squares.getAnElement();
        Square square2 = squares.getPartner(square1);
        return !square1.equals(square2);
    }

    /** 
     * Returns whether this border is shared by two squares. 
     * 
     * @pre
     *   | !isTerminated()
     * @return 
     * Whether this border is shared by two squares. 
     */
    public boolean isSharedByTwoSquares() {
        assert !isTerminated();
        return squares.getNbElements() == 2;
    }

    /** 
     * Detatch the given square from this border.
     *
     * @param square 
     * The square to detatch from this border
     * @pre
     *   | !isTerminated() &amp;&amp; !square.hasBorder(this)
     * @post
     *   | !new.bordersOnSquare(square)
     * @post
     *   | if (!old.isSharedByTwoSquares())
     *   | then (new.isTerminated())
     * @throws IllegalArgumentException
     *   | !bordersOnSquare(square)
     */
    void detatchFromSquare(@Raw Square square)
                                        throws IllegalArgumentException {
        assert !isTerminated() && !square.hasBorder(this);

        if (!bordersOnSquare(square))
            throw new IllegalArgumentException();

        if (!isSharedByTwoSquares()) {
            terminate();
            return;
        }
        squares.delete(square);
    }

    /** 
     * Check whether this border has proper squares.
     * 
     * @return 
     * True if this border is not terminated and all of the squares it 
     * borders to, also have this border as a border.
     */
    @Raw
    public boolean hasProperSquares() {
        if (isTerminated())
            return (squares == null);
        
        for (Square square : squares)
            if (!square.hasBorder(this))
                return false;
        return true;
    }

    /** 
     * Check whether this border has no terminated squares associated with 
     * it.
     * 
     * @return 
     * True iff this border is not terminated or all of the squares it 
     * borders are not terminated.
     */
    @Raw
    public boolean hasNoTerminatedSquares() {
        if (squares == null)
            return true;
        for (Square square : squares)
            if (square.isTerminated())
                return false;
        return true;
    }

    /**
     * Add the given square to this border.
     *
     * @param square
     * The square to add to this border.
     * @pre
     *   | !isSharedByTwoSquares()
     * @post
     *   | new.getNeighbour(old.getASquare()).equals(square)
     */
    private void addSquare(Square square) {
        assert !isSharedByTwoSquares();
        squares.add(square);
    }

    /** 
     * Variable referencing the square(s) that border(s) on this border.
     */
    private Couple<Square> squares;

    /**
     * Return the termination status for this border.
     */
    @Basic @Raw
    public boolean isTerminated() {
        return isTerminated;
    }
    
    /** 
     * Terminate this border.
     *
     * @pre
     * None of the associated squares may still have this border as a 
     * border.
     * @post
     *   | new.isTerminated()
     * @post
     *   | new.hasProperSquares()
     */
    @Raw
    private void terminate(){
        if (isTerminated())
            return;

        isTerminated = true;
        
        if (squares == null)
            return;

        for (Square square : squares)
            assert !square.hasBorder(this);
        squares = null;
    }
    
    /**
     * Variable registering the termination status for this border.
     */
    private boolean isTerminated = false;

    /**
     * Return a small string as a symbol for this border.
     */
    @Basic
    public abstract String symbol();

    /** 
     * Returns the level of 'openness' of a border.
     *
     * @return
     * The openness of a border.
     * @return
     * This value imposes a total ordering on the different types of 
     * borders.
     * @note
     * The actual value may change in subsequent versions. Don't rely on 
     * the actual value, but merely on relative differences between 
     * different borders to impose an ordering.
     * Never let this leak out to the public.
     */
    protected abstract int openness();
}

// vim: ts=4:sw=4:expandtab:smarttab

