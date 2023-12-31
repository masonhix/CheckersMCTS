import java.util.*;

public class PlayerMason {
    static double start;
    static double end;
    static boolean outOfTime = false;
    static int buffer = 50; // Through MANY trial and error tests 50ms seems to work well for an extra buffer for time constraints.
    static Random random=new Random();
    static class Node {
        mhState state;
        Node[] branches;
        Node parent;
        double won = 0; // num times a node won
        int played = 0; // num times a node has been played
        public boolean gameOver() {
            //length of branches is 0
            if(branches.length == 0) {
                //System.err.println("Game is over");
                return true;
            } else return false;
            //return branches.length == 0;
        }
        public boolean isLeaf() {
            // if there is nothing in the branches array, it is a leaf node. The nodes
            // will automatically load in with null in the branches until expanded. During playout, do an AB pruning evaluation
            // with a depth of 2 or 3 so it's fast on a random move from the movelist to evaluate.
            return (branches[0] == null);
        }
        public Node(mhState state, Node parent) {
            // Generate a new node which contains a state of the board, a parent node, and branches. If this is a leaf node
            // the branches will be null. Also contained in EVERY node is a counter of wins and number of times played.
            this.state = state;
            this.branches = new Node[state.numLegalMoves];
            //Arrays.fill(this.branches, null);
            this.parent = parent;
        }

        public static double utility(Node curr, Node parent) {
            //System.err.println("Trying utility with " + curr.played + " plays and  " + curr.won + " wins.");
            double xi = (curr.played-curr.won); //The immediate reward
            int n = curr.played;
            int N = parent.played;
            double Vn =  (xi*xi)/n - (xi/n)*(xi/n) + Math.sqrt(2*Math.log(N)/n);
            double c = Math.sqrt((Math.log(N)/n)*Math.min(0.25, Vn));
            //double c = Math.sqrt(2);
            if (curr.played < 1) return 500;
            else  return xi/curr.played+c;//*Math.sqrt(Math.log(N)/(n));

            // UCB = average reward + c*sqrt(ln(t)/Nt(a))
            // Alternatively: UCB = average reward + sqrt(c*ln(t)/Nt(a)). C is a constant so being inside or outside the sqrt doesn't make much of a difference (unless c is negative but we will assume this will never happen).
            //
            // The average reward is the number of wins compared to the number of times played, and will be between 0 and 1 inclusive
            // C is the exploration constant which determines how much value exploratory moves should have. A value of 0 will only consider the immediate average reward, and a large value will overvalue exploratory moves
            // t is the timestep, which in this case is how many times the parent was played.
            // Nt(a) is the number of times this action was taken, which is the number of times the current node was played
            /*
                What does this formula mean?
                    The square root pretty much just normalizes the values and can frankly be ignored. The important pieces is ln(t)/Nt(a). If the Nt(a) is low, meaning it either hasn't been played or has only been played a couple times
                    compared to the parent, ln(t)/Nt(a) will grow larger since it is in the denominator. Combined with the exploration constant c, it will overvalue this action which entices it to explore assuming a moderate value of c.
                    As the parent gets played more, ln(t) grows larger *up to a limit* due to the natural log term. As the parent is played more, the Nt(a) will eventually also grow until this eventually converges to 0. It does not
                    converge to 1 because ln(t) will only grow larger to a point, and when the Nt(a) >> ln(t) it will eventually be a very small number.

                    So ln(t)/Nt(a) has a maximum value of ~2.8ish (strangely approximately 2*sqrt(2)). To try and avoid over-exploring, I only want the value of the exploratory term to be at most the same value as the maximum average reward
                    so c will be set to 1/(2*sqrt(2)) or approximately 0.35.
             */

        }

        public static void expand(Node curr) {
            //System.err.println("Expanding");
            // Expand the current node and reveal the leaf nodes under it. When a node is generated we automatically assume it is a leaf node even though it is not.
            // The leaf nodes do not turn into more nodes until they are expanded. They are not expanded until we decide we need to go deeper.
            for(int i = 0; i<curr.state.numLegalMoves; i++) {
                // A new LEAF NODE for each legal move present in the current node, and the parent of each of these nodes is the current node.
                mhState nextState = new mhState(curr.state);
                PerformMove(nextState, i);
                curr.branches[i] = new Node(nextState, curr); // Set the branch at location i from null to the new node we just made. Update isMaxPlayer to the inverse of the current value.
                //curr.played++;
                int score = playout(nextState, 30, 3);
                //if (nextState.numLegalMoves >= 4) score = playout(nextState, 30, 4);
                //else score = playout(nextState, 30, 2);
                if (score > 0) curr.branches[i].won += 1;
                else if (score == 0) curr.branches[i].won += 0.5;
                //curr.won += playout(nextState, 5, 80, !curr.isMaxPlayer)>0?1:0; //playout using AB pruning. If playout is >0 add 1 to the curr.won score, anything else add 0 (draws are basically losses).
                curr.branches[i].played += 1; // Increment the curr.played value by one regardless of a win/loss/draw.
                //System.err.println("Branch " + i + " won " + curr.branches[i].won + " times and was played " + curr.branches[i].played + " times");
                backprop(curr.branches[i]);
            }
            //backprop(curr); // Backpropagate the won/played values to the current nodes parent.
        }
        public static void backprop(Node curr, double won, int played) {

            if(curr == null) return; // If there is no parent then this is the root node in which case return.
            curr.won += won;
            curr.played += played;
            backprop(curr.parent, played-won, played); // Recursive call to update the parent. Same formula as seen in the other backprop function where the parent wins is the child losses, or (played-wins).


        }
        public static void backprop(Node curr) {
            // Backpropagate the values of won/played of the current node to the parent node. The played value gets propagated regardless, but since the parent node will actually be the opposite player
            // the wins for the parent is actually the losses for the current. In other words, parent wins = child.plays - child.wins
            backprop(curr.parent, curr.played-curr.won, curr.played); // Shoot the wins/played values back up to the parent.
            //backprop(curr, curr.won, curr.played);
        }

        public static int playout(mhState myState, int maxMoves, int maxDepth) {
            // Playout method without using recursion. It is impossible to try to compare the value this returns to the recurisve playout method because it shuffles the movelist. Even comparing the recursive function results to itself shows differences.
            // It should still play just as well as before though.
            boolean isMe = false;   // dummy boolean to track whether the state being evaluated is me or the opponent. This is initialized to false since the move is performed before the playout in expand, so I know it is a child nodes (opponents) turn.
            mhState state = new mhState(myState); // make a copy of the state so I don't mess up the original.
            // Just about the only thing I need to do this without recursion is a while loop and a way to track who's turn it is to see if i am returning a positive or a negative 1.
            while(maxMoves > 0) {
                int myBestMoveIndex = 0;
                double bestMoveValue = -Double.MAX_VALUE;
                if(state.numLegalMoves==0) {
                    if (!isMe) return -1;    // i'm going to be honest i don't know why these are swapped but it's 11:30 and i'm tired but this makes me win
                    else return 1;
                }
                shuffle(state.movelist, state.numLegalMoves);
                for (int x = 0; x < state.numLegalMoves; x++) {
                    mhState nextState = new mhState(state);
                    PerformMove(nextState, x);
                    // the false indicates whether this player is the max player. Since perform move is done twice (one here and one later), this is always false.
                    double temp = minmaxAB(nextState, maxDepth, -Double.MAX_VALUE, Double.MAX_VALUE, false, PlayerHelperMason.SecPerMove);
                    if (temp > bestMoveValue) {
                        bestMoveValue = temp;
                        myBestMoveIndex = x;
                    }
                }
                // Perform the move on the copy of our state and redo everything.
                PerformMove(state, myBestMoveIndex);
                isMe = !isMe; // flip flop isMe.
                maxMoves--;
            }
            return 0; // If i never got to a point where the number of legal moves was 0, then it must be a tie.
        }

        public static void select(Node curr) {

            if(curr.gameOver()) { // if the game is over, meaning there are NO branches (no legal moves) then the game is over for the current node. Update the played value and propagate it up the tree.
                curr.played++;
                backprop(curr.parent, 1, 1);
                return;
            }
            if(curr.isLeaf()) { // If we are trying to select a leaf node, it won't have a state. In this case, expand the current node to turn it into a node with branches of new leaf nodes.
                expand(curr);
                return;
            }
            double bestUtility = utility(curr.branches[0], curr); // utility() is the ubc function. Set the utility equal to the first branch, but we will run through all of them.
            Node selected = curr.branches[0]; // Selected node is the first branch of the current node. This branch will be a node at this point, because if it was a leaf then we expanded it earlier. Since it is a node, the utility function can be applied.
            for(int i = 1; i < curr.branches.length; i++) {
                double tempUtility = utility(curr.branches[i], curr); // Utility where the current branch node is the child, and the current node is the parent.
                if(tempUtility>bestUtility) {
                    bestUtility = tempUtility; // Update utility to the best one
                    selected = curr.branches[i]; // Update the selected node to the one with the best utility.
                }
            }
            select(selected); // Recursively call select on the selected node, which was the branch with the best utility, and run through all of this again. The purpose for doing this is to eventually

            // run through a path that has the best utility. This will also give some exploration from the UCB function.


            //select(selected); // Recursively call select on the selected node, which was the branch with the best utility, and run through all of this again. The purpose for doing this is to eventually
            // run through a path that has the best utility. This will also give some exploration from the UCB function.


        }


    }

    static void setupBoardState(mhState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        PlayerHelperMason.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        PlayerHelperMason.FindLegalMoves(state);
    }
    static double minmaxAB(mhState state, int maxDepth, double alpha, double beta, boolean isMaxPlayer, float SecPerMove) {
        maxDepth--;
        if (isMaxPlayer) { // max loop
            if(maxDepth <= 0) return evalBoard(state);
            end = System.currentTimeMillis();
            outOfTime = isOutOfTime(start, end, SecPerMove);
            if (outOfTime) maxDepth = 0;
            double moveVal = -(Double.MAX_VALUE);
            for (int i = 0; i < state.numLegalMoves; i++) {
                mhState nextState = new mhState(state);
                PerformMove(nextState, i);
                moveVal = Math.max(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, false, SecPerMove)); // The next player will NOT be max player
                alpha = Math.max(alpha, moveVal);
                if (alpha >= beta) break;
            }
            return alpha;
        } else { // min loop
            if(maxDepth <= 0) return (1/(evalBoard(state)));
            end = System.currentTimeMillis();
            outOfTime = isOutOfTime(start, end, SecPerMove);
            if (outOfTime) maxDepth = 0;
            double moveVal = Double.MAX_VALUE;
            for (int i = 0; i < state.numLegalMoves; i++) {
                mhState nextState = new mhState(state);
                PerformMove(nextState, i);
                moveVal = Math.min(moveVal, minmaxAB(nextState, maxDepth, alpha, beta, true, SecPerMove)); // The next player WILL be max player
                beta = Math.min(beta, moveVal);
                if (alpha >= beta) break;
            }
            return beta;
        }
    }


    static void PerformMove(mhState state, int moveIndex)
    {
        PlayerHelperMason.PerformMove(state.board, state.movelist[moveIndex], PlayerHelperMason.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        PlayerHelperMason.FindLegalMoves(state);
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

        I have modified this function to also take in the SecPerMove from PlayerHelperMason to track time and make sure
        it never goes over time, even if that means picking a sub-optimal move.

     */

    //max is going to be the length of the array or whatever gets put into there.
    static void shuffle(Object[] arr, int maxi) {
        int i, j;
        for(i=0; i < maxi; i++) {
            j = random.nextInt(maxi);
            Object temp = arr[j];
            arr[j] = arr[i];
            arr[i] = temp;
        }
    }

    public static void FindBestMoveMCTS(int player, char[][] board, char[] bestMove) {
        mhState state = new mhState();
        setupBoardState(state, player, board);
        shuffle(state.movelist, state.numLegalMoves);
        printBoard(state);
        Node root = new Node(state, null); // The root node. It has no parent and contains the state of the board from when the FindBestMoveMCTS function was called. This is NOT always the fresh board.
        //System.err.println("Expanding root...");
        //Node.expand(root);
        //System.err.println("Expanded root node. It has " + root.won + " wins and " + root.played + " plays.");
        start = System.currentTimeMillis();
        end = System.currentTimeMillis();
        while(!isOutOfTime(start, end, PlayerHelperMason.SecPerMove)){
            end = System.currentTimeMillis();
            Node.select(root);
            //System.err.println("Root has been played " + root.played + " times with " + root.won + " wins.");
            // I would expect this message to constantly update with more and more games played and more and more wins as the game goes on.
        }
        /*
        System.err.println("Root was played " + root.played + " times and won " + root.won + " times.");
        for(int i = 0; i < root.branches.length; i++) {
            System.err.println("Branch " + i + " of root was played " + root.branches[i].played + " times and won " + (root.branches[i].played-root.branches[i].won) + " times.");
        }
         */
        double bestUtility = (root.branches[0].played-root.branches[0].won)/root.branches[0].played;
        //double bestUtility = root.branches[0].played;
        int myBestMoveIndex = 0;
        for(int i = 1; i<root.branches.length; i++) { // pick the branch with the most number of plays
            double tempUtility = (root.branches[i].played-root.branches[i].won)/root.branches[i].played;
            //double tempUtility = root.branches[i].played;
            if(tempUtility>bestUtility) {
                bestUtility = tempUtility;
                myBestMoveIndex = i;
            } else if (tempUtility == bestUtility) {
                if (root.branches[i].played > root.branches[myBestMoveIndex].played) {
                    bestUtility = tempUtility;
                    myBestMoveIndex = i;
                }
            }
        }
        //System.err.println("The best move found was " + myBestMoveIndex + " with a utility of " + bestUtility);
        PlayerHelperMason.memcpy(bestMove, state.movelist[myBestMoveIndex], PlayerHelperMason.MoveLength(state.movelist[myBestMoveIndex]));
    }

    /* Employ your favorite search to find the best move. This code is an example */
    /* of an alpha/beta search, except I have not provided the MinVal,MaxVal,EVAL */
    /*
     * functions. This example code shows you how to call the FindLegalMoves
     * function
     */
    /* and the PerformMove function */

    // Modified the function to take the SecPerMove from PlayerHelperMason. This way I can run my code for SecPerMove - buffer
    // and if it would still be executing, play the best move found instead of losing by default since I ran out of time.
    public static void FindBestMove(int player, char[][] board, char[] bestmove, float SecPerMove) {
        boolean killerMove = false; // Used for breaking out if we find a winning move early and don't need to search anymore.
        outOfTime = false; // reset outOfTime to false
        start = System.currentTimeMillis(); // Start tracking the time in milliseconds
        end = System.currentTimeMillis();
        int myBestMoveIndex;
        mhState state = new mhState(); // , nextstate;
        setupBoardState(state, player, board);
        myBestMoveIndex = 0;
        int maxDepth = 100;
        double bestMoveValue = -(Double.MAX_VALUE); // Start at negative infinity for the best move value
        double[] bestMoveID = new double[state.numLegalMoves]; // Used for tracking the iterative deepening best move values
        int[] bestIndexID = new int[state.numLegalMoves]; // Used for tracking the iterative deepening best move indexes
        // Fill the ID arrays with junk data
        Arrays.fill(bestIndexID, 0);
        Arrays.fill(bestMoveID, -(Double.MAX_VALUE));
        // Iterative deepening loop
        for (int i = 1; i < maxDepth + 1; i++) {
            if (outOfTime) break; // Jump out if i run out of time
            // alpha beta pruning loop
            for (int x = 0; x < state.numLegalMoves; x++) {
                if (outOfTime) break; // Jump out if i run out of time
                mhState nextState = new mhState(state);
                PerformMove(nextState, x);
                //This will eventually hit a terminal node a return a value for this state.
                double temp = minmaxAB(nextState, i, -(Double.MAX_VALUE), Double.MAX_VALUE, false, SecPerMove);
                // Since we already perform the next move above on the copy of the state it would actually be min's turn next not max.
                //System.err.println("Move " + state.movelist[x] + " has a value of " + temp + "\n");

                // If the value is absurdly large we have found a winning move and there is no need to do more searching.
                // At this point, jump out of the loops and pick this move.
                if (temp > 1E6) {
                    myBestMoveIndex = x;
                    //System.err.println("Killer move found at " + state.movelist[myBestMoveIndex] +"! Ending early \n");
                    killerMove = true;
                    break;
                }
                // Update the best move value to the most recently found value if and only if it was better
                if (temp > bestMoveValue) {
                    myBestMoveIndex = x;
                    bestMoveValue = temp;
                }
                // If the move has the same value as the current best move, flip a coin to add randomness.
                else if (temp == bestMoveValue) {
                    if ((random.nextInt(2)) == 1) myBestMoveIndex = x;
                }
                // Update iterative deepening arrays.
                bestMoveID[x] = bestMoveValue;
                bestIndexID[x] = myBestMoveIndex;
            }
            // Break out if we found a killer move earlier.
            if (killerMove) break;
            // Keep track of the best move found at a depth and the corresponding index it was found.
            //System.err.println("For a depth of " + i + " the best move is " + state.movelist[myBestMoveIndex] + " with a value of " + bestMoveValue + "\n");
            //System.err.println("Time: " + (end-start) + "\n");
        }

        // If we did not find a winning move, pick the best move found at the deepest level
        if (!killerMove) {
            myBestMoveIndex = 0;
            bestMoveValue = -(Double.MAX_VALUE);
            for (int i = 0; i < state.numLegalMoves; i++) {
                if (bestMoveID[i] > bestMoveValue) {
                    bestMoveValue = bestMoveID[i];
                    myBestMoveIndex = bestIndexID[i];
                } else if (bestMoveID[i] == bestMoveValue) { // Add randomness to confuse opponent
                    if ((random.nextInt(2)) == 1) {
                        bestMoveValue = bestMoveID[i];
                        myBestMoveIndex = bestIndexID[i];
                    }
                }
            }
        }
        //System.err.println("Selecting move " + state.movelist[myBestMoveIndex] + " with a value of " + bestMoveValue + "\n");
        PlayerHelperMason.memcpy(bestmove, state.movelist[myBestMoveIndex], PlayerHelperMason.MoveLength(state.movelist[myBestMoveIndex]));
    }

    static void printBoard(mhState state)
    {
        int y,x;

        for(y=0; y<8; y++)
        {
            for(x=0; x<8; x++)
            {
                if(x%2 != y%2)
                {
                    if(PlayerHelperMason.empty(state.board[y][x]))
                    {
                        System.err.print(" ");
                    }
                    else if(PlayerHelperMason.king(state.board[y][x]))
                    {
                        if(PlayerHelperMason.color(state.board[y][x])==2) System.err.print("B");
                        else System.err.print("A");
                    }
                    else if(PlayerHelperMason.piece(state.board[y][x]))
                    {
                        if(PlayerHelperMason.color(state.board[y][x])==2) System.err.print("b");
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

    static double evalBoard(mhState state)
    {
        int y,x;
        double score, scoreMe, scoreOpponent;
        scoreMe=0.0;
        scoreOpponent = 0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(PlayerHelperMason.empty(state.board[y][x]))
                {
                }
                else if(PlayerHelperMason.king(state.board[y][x]))
                {
                    if(PlayerHelperMason.color(state.board[y][x])==2) scoreMe += 2.1;
                    else scoreOpponent += 2.1;
                }
                else if(PlayerHelperMason.piece(state.board[y][x]))
                {
                    if(PlayerHelperMason.color(state.board[y][x])==2) scoreMe += 1.0;
                    else scoreOpponent += 1.0;
                }
            }
        }

        score = scoreMe/scoreOpponent;
        if(state.player==1) score = 1/score;

        return score;

    }

}
