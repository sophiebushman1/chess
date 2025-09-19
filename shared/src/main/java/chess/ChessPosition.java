package chess;


/**
 * Represents a single square position on a chess board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPosition {
    private final int row;
    private final int column;
    //Rows and columns are 1-8 where column 1 is left and row 1 is bottom. This sets up our row and column variable that will eventually be returned

    public ChessPosition(int row, int col) {
        //Make sure the row and col passed in fit the board format
        if (row<1 || col<1) {
            throw new IllegalArgumentException("rows and columns must be 1 or less than 1");
        }
        if (row>8 || col>8) {
            throw new IllegalArgumentException("rows and columns must be 8 or less than 8");
        }
        this.row = row;
        this.column = col;
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {

        return row;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left row
     */
    public int getColumn() {

        return column;
    }
}
