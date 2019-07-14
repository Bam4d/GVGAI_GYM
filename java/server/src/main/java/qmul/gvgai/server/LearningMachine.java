package qmul.gvgai.server;

import qmul.gvgai.engine.core.competition.CompetitionParameters;
import qmul.gvgai.engine.core.game.Game;
import qmul.gvgai.engine.core.game.StateObservation;
import qmul.gvgai.engine.core.game.StateObservationMulti;
import qmul.gvgai.engine.core.player.LearningPlayer;
import qmul.gvgai.engine.core.player.Player;
import qmul.gvgai.engine.core.vgdl.VGDLFactory;
import qmul.gvgai.engine.core.vgdl.VGDLParser;
import qmul.gvgai.engine.core.vgdl.VGDLRegistry;
import qmul.gvgai.engine.ontology.Types;
import qmul.gvgai.engine.tools.ElapsedCpuTimer;
import qmul.gvgai.engine.tools.StatSummary;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Daniel on 07.03.2017.
 */
public class LearningMachine {
    public static final boolean VERBOSE = false;

    /**
     * Reads and launches a game for an agent to be played. Graphics can be on or off.
     *
     * @param game_file  game description file.
     * @param level_file file with the level to be played.
     * @param visuals    true to show the graphics, false otherwise. Training games have never graphics set to ON.
     * @param cmd  array with name of the script file to run for the client, plus agent and port
     * @param actionFile filename of the file where the actions of this player, for this game, should be recorded.
     * @param randomSeed sampleRandom seed for the sampleRandom generator.
     */
    public static double[] runOneGame(String game_file, String level_file, boolean visuals,
                                      String[] cmd, String actionFile, int randomSeed) throws IOException {
        VGDLFactory.GetInstance().init(); //This always first thing to do.
        VGDLRegistry.GetInstance().init();
        CompetitionParameters.IS_LEARNING = true;

        System.out.println(" ** Playing game " + game_file + ", level " + level_file + " **");

        //1. Create the player.
        LearningPlayer player = LearningMachine.createPlayer(cmd);
//
        //2. Play the training games.
        double[] finalScore = playOnce(player, actionFile, game_file, level_file, visuals, randomSeed);

        return finalScore;
    }

    /**
     * Reads and launches a game to be played on a series of both pre-determined and non
     * pre-determined levels.
     *
     * @param game_file  game description file.
     * @param level_files file with the level to be played.
     * @param cmd  array with name of the script file to run for the client, plus agent and port
     * @param actionFiles filename of the file where the actions of this player, for this game, should be recorded.
     */
    public static void runMultipleGames(String game_file, String[] level_files,
                                        String cmd[], String[] actionFiles, boolean visuals) throws IOException {
        VGDLFactory.GetInstance().init(); //This always first thing to do.
        VGDLRegistry.GetInstance().init();
        CompetitionParameters.IS_LEARNING = true;
        //Create the player.
        LearningPlayer player = LearningMachine.createPlayer(cmd);

        // Play the training games.
        //Playable levels somehow governed by Types.NUM_TRAINABLE_LEVELS (not in this file or LearningPlayer)
        runGymGames(game_file, level_files, 1, player, actionFiles, visuals);
    }

    /**
     * Play a given level of a given game once using a given player
     * @param player
     * @param actionFile
     * @param game_file
     * @param level_file
     * @param visuals
     * @param randomSeed
     * @return Score of players in the game (one player in a single player case)
     * @throws IOException
     */
    private static double[] playOnce(LearningPlayer player, String actionFile, String game_file, String level_file,
                                     boolean visuals, int randomSeed) throws IOException {
        //Create the game.
        Game toPlay = new VGDLParser().parseGame(game_file);
        toPlay.buildLevel(level_file, randomSeed);

        //Init the player for the game.
        if (player == null || LearningMachine.initPlayer(player, actionFile, randomSeed, false, toPlay.getObservation()) == null) {
            //Something went wrong in the constructor, controller disqualified
            toPlay.disqualify();
            //Get the score for the result.
            return toPlay.handleResult();
        }

        //Then, play the game.
        double[] score;

        Player[] players = new Player[]{player};
        if (visuals)
            score = toPlay.playGame(players, randomSeed, true, 0);
        else
            score = toPlay.runGame(players, randomSeed);

        //Finally, when the game is over, we need to tear the player down.
        LearningMachine.tearPlayerDown(player, toPlay);

        return score;
    }

/**
     * Reads and launches a game for a bot to be played. It specifies which levels to play and how many times.
     * Filenames for saving actions can be specified. Graphics always on.
     * @param game_file game description file.
     * @param level_files array of level file names to play.
     * @param level_times how many times each level has to be played.
     * @param actionFiles names of the files where the actions of this player, for this game, should be recorded. Accepts
     *                    null if no recording is desired. If not null, this array must contain as much String objects as
     *                    level_files.length*level_times.
     */
    public static void runGymGames(LearningPlayer player) throws IOException {
        VGDLFactory.GetInstance().init(); //This always first thing to do.
        VGDLRegistry.GetInstance().init();


        Game toPlay = new VGDLParser().parseGame(game_file);
        int levelOutcome = 0;


        StatSummary[] victories = new StatSummary[toPlay.getNoPlayers()];
        StatSummary[] scores = new StatSummary[toPlay.getNoPlayers()];
        victories[0] = new StatSummary();
        scores[0] = new StatSummary();

        // Player array to hold the single player
        LearningPlayer[] players = new LearningPlayer[]{player};

        // Initialize the player
        boolean initSuccesful = players[0].startPlayerCommunication();
        if (!initSuccesful) {
            return;
        }

        int level = players[0].chooseLevel();

        //We only continue playing if the round is not over.
        System.out.println("[PHASE] Start training on selected levels.");
        while (level >= 0) {

            // Play the selected level once
            System.out.println("Level:" + level + ", Size: " + level_files.length);
            playOneLevel(game_file, level_files[level], 0, false, visual, recordActions,
                    level, players, actionFiles, toPlay, scores, victories);

            level = players[0].chooseLevel();
        }

        System.out.println("[PHASE] End Training.");
        String vict = "", sc = "";
        for (int i = 0; i < toPlay.no_players; i++) {
            vict += victories[i].mean();
            sc += scores[i].mean();
            if (i != toPlay.no_players - 1) {
                vict += ", ";
                sc += ", ";
            }
        }

        player.finishPlayerCommunication();
    }



    /**
     * Method used to play a single given level. It is also used to request player input in regards
     * to the next game to be played.
     *
     * @param game_file Game file to be used to play the game. Is sent by parent method.
     * @param level_file Level file to be used to play the game. Is sent by parent method.
     * @param level_time Integer denominating how many times the current level has been played in a row.
     *                   Is also sent from the exterior, and exists for debugging only.
     * @param isValidation Indicates if the level being played is a validation level
     * @param recordActions Boolean determining whether the actions should be recorded.
     * @param levelIdx Level index. Used for debugging.
     * @param players Array of Player-type objects. Used to play the game
     * @param actionFiles Files used to record the actions in for logging purposes.
     * @param toPlay The game to be played. Must be pre-initialized.
     * @param scores Array of scores to be modified. Is modified at the end of the level.
     * @param victories Array of victories to be modified. Is modified at the end of the level.
     */
    public static void playOneLevel(String game_file, String level_file, int level_time, boolean isValidation, boolean isVisual, boolean recordActions,
                                   int levelIdx, LearningPlayer[] players, String[] actionFiles, Game toPlay, StatSummary[] scores,
                                   StatSummary[] victories) throws IOException{
        if (VERBOSE)
            System.out.println(" ** Playing game " + game_file + ", level " + level_file + " (" + level_time + ") **");

        // Create a new random seed for the next level.
        int randomSeed = new Random().nextInt();

        //build the level in the game.
        toPlay.buildLevel(level_file, randomSeed);

        String filename = recordActions ? actionFiles[levelIdx * level_time] : null; // TODO: 22/05/17 check this

        // Score array to hold handled results.
        double[] score;

        // Initialize the new learningPlayer instance.
        LearningPlayer learningPlayer = LearningMachine.initPlayer(players[0], actionFiles[0], randomSeed, isValidation, toPlay.getObservation());

        // If the player cannot be initialized, disqualify the controller
        if (learningPlayer == null) {
            System.out.println("Something went wrong in the constructor, controller disqualified");
            //Something went wrong in the constructor, controller disqualified
            toPlay.getAvatars()[0].disqualify(true);
            toPlay.handleResult();
            toPlay.printLearningResult(levelIdx, isValidation);
            return;
        }
        players[0] = learningPlayer;


        score = toPlay.playOnlineGame(players, randomSeed, false, 0);
//
        toPlay.printLearningResult(levelIdx, isValidation);

        //Finally, when the game is over, we need to tear the player down.
        LearningMachine.tearPlayerDown(players[0], toPlay);

        //Get player stats
        if (players[0] != null) {
            scores[0].add(score[0]);
            victories[0].add(toPlay.getWinner(0) == Types.WINNER.PLAYER_WINS ? 1 : 0);
        }

        // Send results to player and save their choice of next level to be played
        // First create a new observation
        StateObservation so = toPlay.getObservation();

        // Sends results to player and retrieve the next level to be played
        players[0].result(so);
//        System.out.println("LearningMachine required level="+level);
        //reset the game.
        toPlay.reset();
    }

    /**
     * Creates a player given its name. This method starts the process that runs this client.
     *
     * @param cmd name of the script to execute, with parameters (agent name and port).
     *            If cmd[0] is null, we (the server) is not starting the communication, the client is, via sockets.
     * @return the player, created but NOT initialized, ready to start playing the game.
     */
    private static LearningPlayer createPlayer(String[] cmd) throws IOException {
        String scriptName = cmd[0];

        if(scriptName != null) {
            Process client;
            ProcessBuilder builder;
            if (cmd.length == 5) {
                builder = new ProcessBuilder(cmd[0], cmd[1], cmd[2], cmd[3], cmd[4]);
            } else {
                builder = new ProcessBuilder(cmd[0], cmd[1], cmd[2]);
            }
            builder.redirectErrorStream(true);
            client = builder.start();
            return new LearningPlayer(client, cmd[2]);
        }else{
            assert (CompetitionParameters.USE_SOCKETS);
            return new LearningPlayer(null, cmd[2]);
        }

    }

    /**
     * Inits the player for a given game.
     *
     * @param player     Player to start.
     * @param actionFile filename of the file where the actions of this player, for this game, should be recorded.
     * @param randomSeed Seed for the sampleRandom generator of the game to be played.
     * @param isValidation true if playing a validation level.
     * @return the player, created and initialized, ready to start playing the game.
     */
    private static LearningPlayer initPlayer(LearningPlayer player, String actionFile, int randomSeed, boolean isValidation, StateObservation so) {
        //If we have a player, set it up for action recording.
        if (player != null)
            player.setup(actionFile, randomSeed, false);

        //Send Init message.
        if(player.init(so, isValidation))
            return player;

        return null;//Disqualified.
    }

    /**
     * Creates a player given its name with package for multiplayer. This class calls the constructor of the agent
     * and initializes the action recording procedure. PlayerID used is 0, default for single player games.
     * @param playerName name of the agent to create. It must be of the type "<agentPackage>.Agent".
     * @param actionFile filename of the file where the actions of this player, for this game, should be recorded.
     * @param so Initial state of the game to be played by the agent.
     * @param randomSeed Seed for the sampleRandom generator of the game to be played.
     * @param isHuman Indicates if the player is human
     * @return the player, created and initialized, ready to start playing the game.
     */
    // Not useful for singleLearning
    private static LearningPlayer initMultiPlayer(LearningPlayer playerName, String actionFile, StateObservationMulti so, int randomSeed, int id, boolean isHuman)
    {
        return playerName;
    }

    /**
     * Tears the player down. This initiates the saving of actions to file.
     * It should be called when the game played is over.
     *
     * @param player player to be closed.
     */
    private static void tearPlayerDown(LearningPlayer player, Game toPlay) throws IOException {
        player.teardown(toPlay);
    }

    /**
     * Tears multiple players down. This initiates the saving of actions to file.
     * It should be called when the game played is over.
     * Not useful for singleLearning
     * @param players list of players to be closed.
     */
    private static boolean tearMultiPlayerDown(Player[] players, Game toPlay) throws IOException {
        for (Player p : players) {
            //Determine the time due for the controller close up.
            ElapsedCpuTimer ect = new ElapsedCpuTimer();
            ect.setMaxTimeMillis(CompetitionParameters.TEAR_DOWN_TIME);

            //Inform about the result and the final game state.
            if (toPlay.no_players > 1)
                p.resultMulti(toPlay.getObservationMulti(p.getPlayerID()).copy(), ect);
            else
                p.result(toPlay.getObservation(), ect);

            //Check if we returned on time, and act in consequence.
            long timeTaken = ect.elapsedMillis();
            if (ect.exceededMaxTime()) {
                long exceeded = -ect.remainingTimeMillis();
                System.out.println("Controller tear down time out (" + exceeded + ").");

                toPlay.disqualify(p.getPlayerID());
                return false;
            }

            if (VERBOSE)
                System.out.println("Controller tear down time: " + timeTaken + " ms.");
            return true;
        }

        return true;
    }


}