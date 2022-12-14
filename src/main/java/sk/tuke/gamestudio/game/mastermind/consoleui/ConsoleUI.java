package sk.tuke.gamestudio.game.mastermind.consoleui;

import org.springframework.beans.factory.annotation.Autowired;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.service.*;
import sk.tuke.gamestudio.game.mastermind.core.Hole;
import sk.tuke.gamestudio.game.mastermind.core.Pin;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.mastermind.core.Game;
import sk.tuke.gamestudio.game.mastermind.core.GameState;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;


public class ConsoleUI {
    //COLORS
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREY = "\u001B[37m";

    //other variables
    private final Scanner scanner;
    private final Game game;
    private String playerName;
    private static final String gameName = "Mastermind";

    @Autowired
    private CommentService commentService;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private ScoreService scoreService;


    public ConsoleUI(Game game) {
        scanner = new Scanner(System.in);
        this.game = game;
    }

    /*--------------------------- PLAY + HANDLE + SHOW-------------------------------------------*/
    //Main game loop
    public void play(){
        //load up player name
        gameIntroduction();

        //Game introduction
        System.out.println("\nI am thinking " + game.getNumOfHoles() + " color combination, none of those colors are same. The color combination is represented by pins which can be: " + ANSI_RED + "RED" + ANSI_RESET + ", " + ANSI_BLUE + "BLUE" + ANSI_RESET + ", " + ANSI_GREEN + "GREEN" + ANSI_RESET + ", " + ANSI_YELLOW + "YELLOW" + ANSI_RESET + ", " + ANSI_PURPLE + "PURPLE " + ANSI_RESET + "and " + ANSI_CYAN +  "CYAN" + ANSI_RESET + ".\n" +
                "You have " + game.getNumOfRounds() + " rounds (attempts) to guess the combination. Your guess will be evaluated with" + ANSI_BLACK + " BLACK " + ANSI_RESET + ",if you guessed the color and the position of the pin correctly" + ANSI_GREY + " GREY " + ANSI_RESET + "if you guessed only the color of the pin correctly and WHITE if you guessed none of those correctly.\n" +
                "Please remember, that the position of black pins, grey pins or white pins may has nothing in common with the the position of pins in your guess as in evaluation part the black pins will always be first first, the grey pins second and white pins last.\n" +
                "Good luck and Have fun.\n" +
                ANSI_GREY + "Press ENTER to continue" + ANSI_RESET);
        scanner.nextLine();
        System.out.println(ANSI_GREEN + "THE GAME BEGINS!" + ANSI_RESET);

        //Game Loop
        do {

            System.out.println(ANSI_RED + "\nROUND " + (game.getCurrentRound()+1) + ANSI_RESET);
            handleInput();
        }while (game.getGameState() == GameState.PLAYING);

        //Aftermath
        if(game.getGameState() == GameState.WIN){
            System.out.println(ANSI_GREEN + "Congrats you WON!!!" + ANSI_RESET);
        }
        else {
            System.out.println(ANSI_RED + "You FAILED!" + ANSI_RESET);
        }

        callDatabaseFunctions();
        System.out.println(ANSI_GREEN + "\nThank you for playing.\nTill your next game!\nMastermind" + ANSI_RESET);
    }



    //Dealing with all user input, loads it and take action according to user wish
    private void handleInput(){
        int emptyHoles = 0;
        int print = 0;

        //basic game isntructions
        String input;

        do {
            if (print % 6 == 0){
                System.out.println("\nFollowing options are available for you:\n" +
                        "Add colored pin to certain position [A + Color's first character + Position where you wish" + " to place pin], example: AB1 -> places blue pin to first hole.\n" +
                        "The Colors: " + ANSI_RED + "R " + ANSI_BLUE + "B " + ANSI_GREEN + "G " + ANSI_YELLOW + "Y " + ANSI_PURPLE + "P " + ANSI_CYAN + "C" + ANSI_RESET + ".\n" +
                        "Ask for evaluation of your guess when you filled all places with pins [E].\n" +
                        "Show history of certain attempt/s:\n" +
                        "   [H + number of round, from which you wished to see history], for example: H1 -> shows your guess and evaluation from round 1.\n" +
                        "   [H + number of first round, from which you want too see history + - + number of last round (included), till which you want to see history, for example: H2-3 -> shows your guesses and evaluation from rounds 2 and 3.\n" +
                        "Report bug or send your idea for improvement [C]\n" +
                        "What action do you wish to take?");
            }
            System.out.print(ANSI_YELLOW +  "Round " + (game.getCurrentRound()+1) + ": " + ANSI_RESET);
            show(game.getPlayerHoles(), game.getNumOfHoles());
            System.out.println("\n");
            System.out.print("Your action: ");

            input = scanner.nextLine().toUpperCase();

            //Adding Pin
            if (Pattern.matches("A[RBGYPC][1-"+game.getNumOfHoles()+"]", input)) {
                int index = input.charAt(2) - '1';
                switch (input.charAt(1)) {
                    case 'R':
                        game.putPin(Pin.RED, index);
                        break;
                    case 'B':
                        game.putPin(Pin.BLUE, index);
                        break;
                    case 'G':
                        game.putPin(Pin.GREEN, index);
                        break;
                    case 'Y':
                        game.putPin(Pin.YELLOW, index);
                        break;
                    case 'P':
                        game.putPin(Pin.PURPLE, index);
                        break;
                    case 'C':
                        game.putPin(Pin.CYAN, index);
                        break;
                    default:
                        break;
                }
            }

            //Evaluate guess
            else if (Pattern.matches("E", input)) {
                //checks wether user filled all holes with some pins
                emptyHoles = 0;
                for (Hole hole : game.getPlayerHoles()) {
                    if (hole.getColor() == Pin.EMPTY) {
                        emptyHoles++;
                    }
                }

                if (emptyHoles > 0) {
                    if (emptyHoles == 1)
                        System.out.println("It seems there is " + emptyHoles + " empty hole, please fill it so I can evaluate your guess.");
                    else
                        System.out.println("It seems there are " + emptyHoles + " empty holes, please fill them so I can evaluate your guess.");
                } else {
                    System.out.print("Evaluation - Round " + (game.getCurrentRound() + 1) + ": ");
                    show(game.getPlayerHoles(), game.getNumOfHoles());
                    game.evaluate();

                    System.out.print(" | ");
                    show(game.getEvaluationHoles(), game.getNumOfHoles());
                    System.out.println(ANSI_YELLOW + "\n\n-------------------------------------------------------------------------------------------" + ANSI_RESET);

                    game.reset();
                }
            }

            //shows one line of history
            else if (Pattern.matches("H[1-"+(game.getNumOfRounds()-1)+"]", input)) {
                int index = input.charAt(1) - '1';
                if (index >= game.getCurrentRound()) {
                    System.out.println("This round wasn't played yet, please choose different round or action.");
                } else {
                    printHistory(index);
                }
            }

            //shows multiple line of history
            else if (Pattern.matches("H[1-"+(game.getNumOfRounds()-1)+"]-[1-"+(game.getNumOfRounds()-1)+"]", input)) {
                int index1 = input.charAt(1) - '1';
                int index2 = input.charAt(3) - '1';


                if (index1 >= game.getCurrentRound() || index2 >= game.getCurrentRound()) {
                    System.out.println("At least one round from those you wished to display wasn't played yet, please choose different rounds or action.");
                }
                else if (index1 == index2) {
                    printHistory(index1);
                } else {
                    for (int i = index1; i <= index2; i++) {
                        printHistory(i);
                    }
                }
            }

            else if (Pattern.matches("C", input)){
                comment();
            }

            //unrecognized input handler
            else {
                System.out.println("Sorry, I don't quite understand your actions. Could you said it again?");
            }

            //helps with reprinting guidelines for rounds after certian amount of rounds passed
            print++;
        } while (!Pattern.matches("E", input) || emptyHoles != 0);

        //just a fancy wait for user confirmation that he wants to continue
        if(game.getGameState() == GameState.PLAYING) {
            System.out.print(ANSI_GREY + "Press ENTER to continue" + ANSI_RESET);
            scanner.nextLine();
        }
    }

    //Prints various type of fields during game
    private void show(Hole[] holeField, int range){
        for(int i = 0; i < range; i++){
            System.out.printf("%s ", printPins(holeField[i].getColor()));
        }
    }

    //used to print History field
    private void show(Hole[] holeField, int begin, int end){
        for(int i = begin; i < end; i++){
            System.out.printf("%s ", printPins(holeField[i].getColor()));
        }
    }

    /* ---------------------------------------- HELP FUNCTIONS --------------------------------------------*/

    //Loads player name + some default options for player
    private void  gameIntroduction(){
        String input;

        //Load stuff from player
        System.out.println(ANSI_GREEN + "\nWELCOME TO MASTERMIND!" + ANSI_RESET);
        System.out.print("How shall I call you player?\n" +
                 ANSI_RED + "Please don't use name 'O' or 'o' as that is reserved for some game functionality\n" + ANSI_RESET + "Insert name: ");
        playerName = scanner.nextLine();
        while (Pattern.matches("O|o", playerName)){
            System.out.print("Sorry this name is reserved because of some game functions. Try something else, please\nYour name: ");
            playerName = scanner.nextLine();
        }

        System.out.println("\nHello " + ANSI_BLUE +playerName + ANSI_RESET + "!");
        System.out.println("There are few options that you may be interested in before starting the game:\n" +
                "   If you want to see average rating of game " + gameName + " [AR]\n" +
                "   If you want to see others comments about game [SC]\n" +
                "   If you are not interested in any of the mentioned above [E]");
        do {
            System.out.print("Your decision: ");
            input = scanner.nextLine().toUpperCase();
            if (Pattern.matches("AR", input)) showAverageRating();
            else if(Pattern.matches("SC", input)) showComments();
            else if(!Pattern.matches("E", input)) {
                System.out.println("\nSorry I don't know what you mean. Could you say it again, please?");
            }
        }while (!Pattern.matches("E", input));
    }

    //Converts ColorPin enums to colored 0 returns ? if something impossible happens
    private String printPins(Pin pin){
        switch (pin){
            case RED: return (ANSI_RED + "O" + ANSI_RESET);
            case BLUE: return (ANSI_BLUE + "O" + ANSI_RESET);
            case GREEN: return (ANSI_GREEN + "O" + ANSI_RESET);
            case YELLOW: return (ANSI_YELLOW + "O" + ANSI_RESET);
            case PURPLE: return (ANSI_PURPLE + "O" + ANSI_RESET);
            case CYAN: return (ANSI_CYAN + "O" + ANSI_RESET);
            case BLACK: return (ANSI_BLACK + "O" + ANSI_RESET);
            case GREY: return (ANSI_GREY + "O" + ANSI_RESET);
            case EMPTY: return "O";
            default: break;
        }
        return "?";
    }

    //formats printing of history
    private void printHistory(int index){
        System.out.print("History - Round " + (index+1) + ": ");
        show(game.getHistory()[index], 0, game.getNumOfHistoryHoles()/2);
        System.out.print(" | ");
        show(game.getHistory()[index], game.getNumOfHistoryHoles()/2, game.getNumOfHistoryHoles());
        System.out.print("\n");
    }

    /*-------------------------------------------- DATABASE FUNCTIONS --------------------------------------------*/

    //creates comment to database
    private void comment(){
        System.out.print("\nWhat do you wish to share with me?\nYour comment: ");
        String content = scanner.nextLine();
        commentService.addComment(new Comment(gameName, playerName, content, new Timestamp(System.currentTimeMillis())));

        System.out.println("Thank you for your contribution.");
    }

    //prints comments from database
    private void showComments(){
        List<Comment> comments = commentService.getComments(gameName);
        if(comments.size() == 0){
            System.out.println("\nIt seems nobody gave a comment to this game yet.");
        }
        else {
            System.out.println();
            for (Comment comment : comments) {
                System.out.println("Player: " + comment.getPlayer());
                System.out.println("Comment: " + comment.getComment());
                System.out.println("Time: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(comment.getCommentedOn()) + "\n");
            }
        }
    }

    //create rating to database
    private void rate(){
        String rating;
        System.out.print("\nPlease rate us with number between 1-10 (included, only non decimal values).\nYour rating: ");
        while (!Pattern.matches("[1-9]|10", rating = scanner.nextLine())){
            System.out.print("Sorry, I don't understand this type of rating. Could you write it again, please?\nYour rating: ");
        }
        ratingService.setRating(new Rating(gameName, playerName, Integer.parseInt(rating), new Timestamp(System.currentTimeMillis())));
        System.out.println("Thank you for your rating!");
    }

    //shows avg rating of game
    private void showAverageRating (){
        int rating = ratingService.getAverageRating(gameName);

        if(rating == 0) System.out.println("It seems that nobody rated this game yet.");
        else System.out.println("The average rating of game is " + rating);
    }

    //shows player rating
    private void playerRating(){
        int friendRating;
        String friendName;

        System.out.print("Tell me your friends name: ");
        friendName = scanner.nextLine();
        friendRating = ratingService.getRating(gameName, friendName);
        do {

            if (friendRating == -1) {
                System.out.print("\nIt seems player " + ANSI_BLUE + friendName + ANSI_RESET + " didn't played this game yet. Did you misspelled?\n" +
                        "If you don't want to try again [O]\nYour decision: ");
            } else {
                System.out.println("\n" + ANSI_BLUE + friendName + ANSI_RESET + "'s rating is " + friendRating);
                System.out.print("If you want to see someone others rating, please write down their name, if not, write down [O]\nYour decision: ");
            }

            friendName = scanner.nextLine();
            friendRating = ratingService.getRating(gameName, friendName);
        } while (!Pattern.matches("O|o", friendName));
    }

    //generates score of player
    private void score(){
        int[] bestRound = findBestRound();
        int score = (10 - game.getCurrentRound()) * (bestRound[0]*10 + bestRound[1]*5);
        System.out.println("Your score is: " + score + "\n");
        scoreService.addScore(new Score(gameName, playerName, score, new Timestamp(System.currentTimeMillis())));

        System.out.print("Do you wish to see also top players? [Y/N]\nYour decision: ");
        String input;
        do {
            input = scanner.nextLine().toUpperCase();
            if(Pattern.matches("Y", input)){
                List<Score> playersScore = scoreService.getTopScores(gameName);
                System.out.println();
                for (int i = 0; i < playersScore.size(); i++) {
                        System.out.println((i + 1) + ".Player: " + playersScore.get(i).getPlayer());
                        System.out.println("  Score: " + playersScore.get(i).getPoints());
                        System.out.println("  Played at: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(playersScore.get(i).getPlayedOn()) + "\n");
                    }
            }
            else if (!Pattern.matches("[YN]", input)){
                System.out.print("\nSorry I don't know what you mean. Could you say it again, please?\nYour decision: ");
            }
        } while (!Pattern.matches("[YN]", input));
    }

    /*-------------------------------- DATABASE HELP FUNCTIONS -------------------------------------*/
    //helps with generating score by founding the round that has the most black and then most grey pins in evaluation
    public int[] findBestRound(){
        int[] bestRound = new int[2];
        int bestBlack = 0;
        int bestGrey = 0;
        for (Hole[] round: game.getHistory()){
            int black = 0;
            int grey = 0;

            for (Hole hole : round){
                if(hole.getColor() == Pin.BLACK){
                    black++;
                }
                else if(hole.getColor() == Pin.GREY){
                    grey++;
                }
            }
            if (black > bestBlack || (black == bestBlack && grey > bestGrey)){
                bestBlack = black;
                bestGrey = grey;
            }
        }
        bestRound[0] = bestBlack;
        bestRound[1] = bestGrey;

        return bestRound;
    }

    //the prints after game where player can choose from database functions
    private void callDatabaseFunctions(){
        score();
        String input;
        printForCDF();
        int countUserInputs = 1;

        do {
            input = scanner.nextLine().toUpperCase();
            if(Pattern.matches("F", input)) comment();
            else if (Pattern.matches("SC", input)) showComments();
            else if (Pattern.matches("R", input)) rate();
            else if (Pattern.matches("AR", input)) showAverageRating();
            else if (Pattern.matches("FR", input)) playerRating();
            else if (!Pattern.matches("E", input)) {
                System.out.println("Sorry I don't know what you mean. Could you say it again, please?");
            }

            if(Pattern.matches("SC|FR", input) || (countUserInputs % 5 == 0)) {
                printForCDF();
                countUserInputs = 0;
            }
            else if (!Pattern.matches("SC|E", input)) System.out.print("\nYour decision: ");

            countUserInputs++;
        }while (!Pattern.matches("E", input));
    }

    //just printing stuff for callDatabaseDunctions - just for reuseubality
    private void printForCDF(){
        System.out.print("\nThere are few more options available for you:\n" +
                "   If you want to share some feedback with us [F]\n" +
                "   If you want to see others comments about game " + gameName + " [SC]\n" +
                "   If you want to rate the game [R]\n" +
                "   If you want to see average rating of the game [AR]\n" +
                "   If you want to see your friend rating of the game [FR]\n" +
                "   If you are not interested in any of the mentioned above [E]\n" +
                "Your decision: ");
    }
}
