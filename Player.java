import java.util.*;
import java.io.*;

public class Player {
    static double start;
    static double end;
    static boolean outOfTime = false;
    static int buffer = 50; // Through MANY trial and error tests 50ms seems to work well for an extra buffer for time constraints.
    static Random random=new Random();

    static void setupBoardState(State state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        PlayerHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        PlayerHelper.FindLegalMoves(state);
    }


    static void PerformMove(State state, int moveIndex)
    {
        PlayerHelper.PerformMove(state.board, state.movelist[moveIndex], PlayerHelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        PlayerHelper.FindLegalMoves(state);
    }

    /*
        A function that will check if my player is out of time. It uses System.currentTimeMillis() to see how long each
        move is taking in milliseconds. It gets updated everytime a min or max loop is started. If the current time taken
        is approaching the maximum time allowed (modified by a buffer variable) then immediately break out of the loop
        and choose the last move with the most success. This was implemented because in my opinion
        automatically losing by taking too long is worse than just losing normally. Even if we have to do a sub-optimal
        move, there is still a chance to win, but if we take too long and must forfeit, it is impossible to win.
     */

    static boolean isOutOfTime(double start, double end, float SecPerMove) {
        if ((end - start) >= (SecPerMove*1000 - buffer)) {
            //System.err.println("Closing early from time constraint");
            outOfTime = true;
        } else outOfTime = false;
        return outOfTime;
    }
    /*
        A function that does both min and max function as well as alpha beta pruning. This is a little bit easier
        to understand in my opinion and better to keep track of alpha and beta values. This does the same thing
        as having separate min and max functions and calling each other, it just swaps which function is called
        depending on the boolean isMaxPlayer which will alternate between true and false.

        Alpha is the lowest move value that I (maximizing player) am guaranteed of. Beta is the highest move the
        minimizing player is guaranteed of (min wants low values!). As soon as alpha is greater than or equal to beta
        (or alternatively beta is less than or equal to alpha) this means that we have reached a state that will
        NEVER be reached. To reach this state, either me or the opponent would have to make a move that is against
        itself. Basically, I might have a super good move down there, but in order to get there I need the opponent
        to make the dumbest move in existence, which it obviously would not.

        I have modified this function to also take in the SecPerMove from PlayerHelper to track time and make sure
        it never goes over time, even if that means picking a sub-optimal move.

     */
    static double minmaxAB(State state, int maxDepth, double alpha, double beta, boolean isMaxPlayer, float SecPerMove) {
        maxDepth--;
        if (isMaxPlayer) { // max loop
            if(maxDepth <= 0) return evalBoardSpecial(state);
            end = System.currentTimeMillis();
            outOfTime = isOutOfTime(start, end, SecPerMove);
            if (outOfTime) maxDepth = 0;
            double moveVal = -(Double.MAX_VALUE);
            for (int i = 0; i < state.numLegalMoves; i++) {
                State nextState = new State(state);
                PerformMove(nextState, i);
                moveVal = Math.max(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, false, SecPerMove)); // The next player will NOT be max player
                alpha = Math.max(alpha, moveVal);
                if (alpha >= beta) break;
                //alpha = Math.max(alpha, moveVal); // Update alpha to either the latest moveVal reported or keep alpha
            }
            return alpha; // Return alpha instead of moveVal?
        } else { // min loop
            if(maxDepth <= 0) return (1/(evalBoardSpecial(state)));
            end = System.currentTimeMillis();
            outOfTime = isOutOfTime(start, end, SecPerMove);
            if (outOfTime) maxDepth = 0;
            double moveVal = Double.MAX_VALUE;
            for (int i = 0; i < state.numLegalMoves; i++) {
                State nextState = new State(state);
                PerformMove(nextState, i);
                moveVal = Math.min(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, true, SecPerMove)); // The next player WILL be max player
                beta = Math.min(beta, moveVal);
                if (alpha >= beta) break;
                //beta = Math.min(beta, moveVal); // Update beta to either the latest moveVal reported, or keep beta
            }
            return beta; // Return beta instead of moveVal?
        }
    }
/*
    static double min(State state, int maxDepth, double alpha, double beta) {
        if(maxDepth <= 0) return -evalBoard(state); // Return a negative number since this goes AGAINST the max player (me).
        maxDepth--;
        double bestMoveValue = Double.MAX_VALUE;
        for(int i = 0; i < state.numLegalMoves; i++) {
            State nextState = new State(state); // makes a copy of the current state
            PerformMove(nextState, i);
            double temp = max(nextState, maxDepth, alpha, beta);
            bestMoveValue = Math.min(temp, bestMoveValue);
            beta = Math.min(beta, bestMoveValue);
            if (beta <= alpha) break;
        }
        System.err.println("Alpha = " + alpha + "Beta = " + beta);
        return bestMoveValue;
    }

    static double max(State state, int maxDepth, double alpha, double beta) {
        if(maxDepth <= 0) return evalBoard(state);
        maxDepth--;
        double bestMoveValue = -(Double.MAX_VALUE);
        for(int i = 0; i < state.numLegalMoves; i++) {
            State nextState = new State(state); // makes a copy of the current state
            PerformMove(nextState, i);
            double temp = min(nextState, maxDepth, alpha, beta);
            bestMoveValue = Math.max(temp, bestMoveValue);
            alpha = Math.max(alpha, bestMoveValue);
            if (beta <= alpha) break;
        }
        return bestMoveValue;
    }
*/



    /* Employ your favorite search to find the best move. This code is an example */
    /* of an alpha/beta search, except I have not provided the MinVal,MaxVal,EVAL */
    /*
     * functions. This example code shows you how to call the FindLegalMoves
     * function
     */
    /* and the PerformMove function */

    // Modified the function to take the SecPerMove from PlayerHelper. This way I can run my code for SecPerMove - buffer
    // and if it would still be executing, play the best move found instead of losing by default since I ran out of time.
    public static void FindBestMove(int player, char[][] board, char[] bestmove, float SecPerMove) {
        outOfTime = false; // reset outOfTime to false
        start = System.currentTimeMillis(); // Start tracking the time in milliseconds
        end = System.currentTimeMillis();
        int myBestMoveIndex;
        State state = new State(); // , nextstate;
        setupBoardState(state, player, board);
        myBestMoveIndex = 0;
        int maxDepth = 100;
        double bestMoveValue = -(Double.MAX_VALUE);
        double[] bestMoveID = new double[maxDepth];
        int[] bestIndexID = new int[maxDepth];
        Arrays.fill(bestIndexID, 0);
        Arrays.fill(bestMoveID, -(Double.MAX_VALUE));
        for (int i = 1; i < maxDepth + 1; i++) {
            if (outOfTime) break;
            for (int x = 0; x < state.numLegalMoves; x++) {
                if (outOfTime) break;
                State nextState = new State(state);
                PerformMove(nextState, x);
                //This will eventually hit a terminal node a return a value for this state.
                double temp = minmaxAB(nextState, i, -(Double.MAX_VALUE), Double.MAX_VALUE, false, SecPerMove);
                // Took me way too long to find this but since we already perform the next move above on the copy of the
                // state it would actually be min's turn next not max.
                System.err.println("Move " + state.movelist[x] + " has a value of " + temp + "\n");
                if (temp > bestMoveValue) {
                    myBestMoveIndex = x;
                    bestMoveValue = temp;
                }
                // If the move has the same value as the current best move, flip a coin to add randomness.
                else if (temp == bestMoveValue) {
                    if ((random.nextInt(2)) == 1) myBestMoveIndex = x;
                }
            }
            // Keep track of the best move found at a depth and the corresponding index it was found.
            bestMoveID[i-1] = bestMoveValue;
            bestIndexID[i-1] = myBestMoveIndex;
            System.err.println("For a depth of " + i + " the best move is " + state.movelist[myBestMoveIndex] + " with a value of " + bestMoveValue + "\n");
        }
        myBestMoveIndex = 0;
        bestMoveValue = -(Double.MAX_VALUE);
        for(int i = 0; i < maxDepth; i++) {
            if (bestMoveID[i] > bestMoveValue) {
                bestMoveValue = bestMoveID[i];
                myBestMoveIndex = bestIndexID[i];
            }
        }
        System.err.println("Selecting move " + state.movelist[myBestMoveIndex] + " with a value of " + bestMoveValue + "\n");
        PlayerHelper.memcpy(bestmove, state.movelist[myBestMoveIndex], PlayerHelper.MoveLength(state.movelist[myBestMoveIndex]));
    }

    static void printBoard(State state)
    {
        int y,x;

        for(y=0; y<8; y++)
        {
            for(x=0; x<8; x++)
            {
                if(x%2 != y%2)
                {
                    if(PlayerHelper.empty(state.board[y][x]))
                    {
                        System.err.print(" ");
                    }
                    else if(PlayerHelper.king(state.board[y][x]))
                    {
                        if(PlayerHelper.color(state.board[y][x])==2) System.err.print("B");
                        else System.err.print("A");
                    }
                    else if(PlayerHelper.piece(state.board[y][x]))
                    {
                        if(PlayerHelper.color(state.board[y][x])==2) System.err.print("b");
                        else System.err.print("a");
                    }
                }
                else
                {
                    System.err.print("@");
                }
            }
            System.err.print("\n");
        }
    }



    /* An example of how to walk through a board and determine what pieces are on it*/
    /*
        A different evalBoard function that compares number of pieces instead of material difference. Output is
        me/opponent as I still want to value higher numbers.
     */
    static boolean isMyPiece(State state, int y, int x) {
        if(PlayerHelper.color(state.board[y][x])==2) return true;
        else return false;
    }
    // Determine how many friends are nearby, which add to my score as a piece will be harder to take with friends nearby
    static int numFriends(State state, int y, int x) {
        int friends = 0;
        if(y>= 1 && y<=6 && x>= 1 && x<=6) {
            if ((PlayerHelper.piece(state.board[y+1][x+1]) || PlayerHelper.king(state.board[y+1][x+1])) && isMyPiece(state, y+1, x+1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y-1][x+1]) || PlayerHelper.king(state.board[y-1][x+1])) && isMyPiece(state, y-1, x+1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y+1][x-1]) || PlayerHelper.king(state.board[y+1][x-1])) && isMyPiece(state, y+1, x-1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y-1][x-1]) || PlayerHelper.king(state.board[y-1][x-1])) && isMyPiece(state, y-1, x-1)) friends += 1;
        } else if (y == 0 && x>=1 && x<=6) {
            if ((PlayerHelper.piece(state.board[y+1][x+1]) || PlayerHelper.king(state.board[y+1][x+1])) && isMyPiece(state, y+1, x+1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y+1][x-1]) || PlayerHelper.king(state.board[y+1][x-1])) && isMyPiece(state, y+1, x-1)) friends += 1;
        } else if (y == 7 && x>=1 && x<=6) {
            if ((PlayerHelper.piece(state.board[y-1][x+1]) || PlayerHelper.king(state.board[y-1][x+1])) && isMyPiece(state, y-1, x+1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y-1][x-1]) || PlayerHelper.king(state.board[y-1][x-1])) && isMyPiece(state, y-1, x-1)) friends += 1;
        } else if (x == 7 && y>=1 && y<=6) {
            if ((PlayerHelper.piece(state.board[y-1][x-1]) || PlayerHelper.king(state.board[y-1][x-1])) && isMyPiece(state, y-1, x-1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y+1][x-1]) || PlayerHelper.king(state.board[y+1][x-1])) && isMyPiece(state, y+1, x-1)) friends += 1;
        } else if (x == 0 && y>=1 && y<=6) {
            if ((PlayerHelper.piece(state.board[y-1][x+1]) || PlayerHelper.king(state.board[y-1][x+1])) && isMyPiece(state, y-1, x+1)) friends += 1;
            if ((PlayerHelper.piece(state.board[y+1][x+1]) || PlayerHelper.king(state.board[y+1][x+1])) && isMyPiece(state, y+1, x+1)) friends += 1;
        }
        return friends;
    }

    static boolean canSafelyMoveForward(State state, int y, int x) {
        boolean canSafelyMoveForward = false;
        if(y>=0 && y<=5 && x>=2 && x<=5) {
            if(PlayerHelper.empty(state.board[y+2][x+2]) || PlayerHelper.empty(state.board[y+2][x-2])) canSafelyMoveForward = true;
        } else if(y>=0 && y<=5 && x<=1) {
            if(PlayerHelper.empty(state.board[y+2][x+2])) canSafelyMoveForward = true;
        } else if(y>=0 && y<= 5 && x>= 6) {
            if(PlayerHelper.empty(state.board[y+2][x-2])) canSafelyMoveForward = true;
        }
        return canSafelyMoveForward;
    }


    static double evalBoardSpecial(State state)
    {
        int y,x;
        double scoreMe = 0.0;
        double scoreOpponent = 0.0;
        double score = 0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(PlayerHelper.empty(state.board[y][x]))
                {
                }
                else if(PlayerHelper.king(state.board[y][x]))
                {
                    if(isMyPiece(state, y, x)) scoreMe += 1.7;
                    else scoreOpponent += 1.7;
                }
                else if(PlayerHelper.piece(state.board[y][x]))
                {
                    if(isMyPiece(state, y, x)) {
                        scoreMe += 1.0; // Automatically gain points for being my piece
                        //if(numFriends(state, y, x) >= 2) scoreMe += 0.1; // Slightly extra points for having friends nearby
                        //if(canSafelyMoveForward(state, y, x)) scoreMe += 0.1; // Greatly value safely moving forward.
                    }
                    else scoreOpponent += 1.0; // Automatically gain points for being my piece
                    //if (numFriends(state, y, x) >= 2) scoreOpponent += 0.1; // Extra points for having friends nearby
                    //if(canSafelyMoveForward(state, y, x)) scoreOpponent += 0.1;
                }
            }
        }

        score = scoreMe/scoreOpponent;
        if(state.player==1) score = 1/score;

        return score;

    }

    static double evalBoard(State state)
    {
        int y,x;
        double score, scoreMe, scoreOpponent;
        scoreMe=0.0;
        scoreOpponent = 0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(PlayerHelper.empty(state.board[y][x]))
                {
                }
                else if(PlayerHelper.king(state.board[y][x]))
                {
                    if(PlayerHelper.color(state.board[y][x])==2) scoreMe += 2.0;
                    else scoreOpponent += 2.0;
                }
                else if(PlayerHelper.piece(state.board[y][x]))
                {
                    if(PlayerHelper.color(state.board[y][x])==2) scoreMe += 1.0;
                    else scoreOpponent += 1.0;
                }
            }
        }

        score = scoreMe/scoreOpponent;
        if(state.player==1) score = 1/score;

        return score;

    }

}
