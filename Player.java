import java.util.*;
import java.io.*;

public class Player {

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

        On opponents turn, as soon as beta is

     */
    static double minmaxAB(State state, int maxDepth, double alpha, double beta, boolean isMaxPlayer) {
        maxDepth--;
        if (isMaxPlayer) { // max loop
            if(maxDepth <= 0) return evalBoard(state);
            double moveVal = -(Double.MAX_VALUE);
            for (int i = 0; i < state.numLegalMoves; i++) {
                State nextState = new State(state);
                PerformMove(nextState, i);
                moveVal = Math.max(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, false)); // The next player will NOT be max player
                if (moveVal >= beta) break;
                alpha = Math.max(alpha, moveVal); // Update alpha to either the latest moveVal reported or keep alpha
            }
            return moveVal;
        } else { // min loop
            if(maxDepth <= 0) return 1/evalBoard(state);
            double moveVal = Double.MAX_VALUE;
            for (int i = 0; i < state.numLegalMoves; i++) {
                State nextState = new State(state);
                PerformMove(nextState, i);
                moveVal = Math.min(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, true)); // The next player WILL be max player
                if (alpha >= moveVal) break;
                beta = Math.min(beta, moveVal); // Update beta to either the latest moveVal reported, or keep beta
            }
            return moveVal;
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
    public static void FindBestMove(int player, char[][] board, char[] bestmove) {
        int myBestMoveIndex;
        double bestMoveValue = -(Double.MAX_VALUE);
        State state = new State(); // , nextstate;
        setupBoardState(state, player, board);
        myBestMoveIndex = 0;
        for (int maxDepth = 1; maxDepth < 10; maxDepth++) {
            for (int x = 0; x < state.numLegalMoves; x++) {
                State nextState = new State(state);
                PerformMove(nextState, x);
                //This will eventually hit a terminal node a return a value for this state.
                double temp = minmaxAB(nextState, maxDepth, -(Double.MAX_VALUE), Double.MAX_VALUE, false);
                // Took me way too long to find this but since we already perform the next move above on the copy of the
                // state it would actually be min's turn next not max.
                if (temp > bestMoveValue) {
                    myBestMoveIndex = x;
                    bestMoveValue = temp;
                }
            }
        }
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

    static double evalBoard(State state)
    {
        int y,x;
        double score;
        score=0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(PlayerHelper.empty(state.board[y][x]))
                {
                }
                else if(PlayerHelper.king(state.board[y][x]))
                {
                    if(PlayerHelper.color(state.board[y][x])==2) score += 1.8;
                    else score -= 1.8;
                }
                else if(PlayerHelper.piece(state.board[y][x]))
                {
                    if(PlayerHelper.color(state.board[y][x])==2) score += 1.0;
                    else score -= 1.0;
                }
            }
        }

        if(state.player==1) score = -score;

        return score;

    }

}
