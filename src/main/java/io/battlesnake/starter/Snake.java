 package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import java.util.ArrayList; 
import java.util.Random;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            LOG.info("Using default port: {}", port);
            port = "8080";
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Battlesnake documentation can be found at " + 
            "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>.");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @param pingRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return an empty response.
         */
        public Map<String, String> ping() {
            return EMPTY;
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            Map<String, String> response = new HashMap<>();
            response.put("color", "#00ffff");
            response.put("headType", "beluga");
            response.put("tailType", "pixel");
            return response;
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        public Map<String, String> move(JsonNode moveRequest) {
            Map<String, String> response = new HashMap<>();
            int xLocation = moveRequest.get("you").get("body").elements().next().get("x").asInt();
            int yLocation = moveRequest.get("you").get("body").elements().next().get("y").asInt();
            System.out.println("Location is: " + xLocation + ", " + yLocation);
            int width = moveRequest.get("board").get("width").asInt();
            int height = moveRequest.get("board").get("height").asInt();

            ArrayList<String> dangerMoves = new ArrayList<String>();
            final int SNAKE = 1;
            final int MYHEAD = 2;
            String move = "right";
            //don't hit walls
            // if(xLocation<=0)
            // {
            //     dangerMoves.add("left");
            // }

            // if(xLocation>=width-1)
            // {
            //     dangerMoves.add("right");
            // }

            // if(yLocation<=0)
            // {
            //     dangerMoves.add("up");
            // }

            // if(yLocation>=height-1)
            // {
            //     dangerMoves.add("down");
            // }

            //don't hit self
            int[][] searchBoard = new int[height][width];
            
            for(JsonNode snake: moveRequest.get("board").get("snakes"))
            {
                for(JsonNode snakeBody: snake.get("body"))
                {
                    int sCoordX = snakeBody.get("x").asInt();
                    int sCoordY = snakeBody.get("y").asInt();
                    searchBoard[sCoordY][sCoordX] = SNAKE;
                }
            }

            int health =  moveRequest.get("you").get("body").get("health");
            System.out.println("Health is: " + health);

            searchBoard[yLocation][xLocation] = MYHEAD;
            int wallBuffer = 1;

            //handles wall or snake one x to the right
            if(xLocation>=width-1 - wallBuffer || searchBoard[yLocation][xLocation + 1] == SNAKE)
            {
                dangerMoves.add("right");
            }
            //handles wall or snake one x to the left
            if(xLocation<=0 + wallBuffer || searchBoard[yLocation][xLocation - 1] == SNAKE)
            {
                dangerMoves.add("left");
            }
            //handles wall or snake one y to the bottom
            if(yLocation>=height-1 - wallBuffer || searchBoard[yLocation + 1][xLocation] == SNAKE)
            {
                //handles curling into it'self to the right (looking ahead by 2 squares)
                if(searchBoard[yLocation][xLocation+1] == SNAKE)
                {
                    dangerMoves.add("right");
                }
                //handles curling into it'self to the left (looking ahead by 2 squares)
                if(searchBoard[yLocation][xLocation-1] == SNAKE)
                {
                    dangerMoves.add("left");
                }

                dangerMoves.add("down");
            }
            //handles wall or snake one y to the top
            if(yLocation<=0 + wallBuffer || searchBoard[yLocation - 1][xLocation] == SNAKE)
            {   
                //handles curling into it'self to the right (looking ahead by 2 squares)
                if(searchBoard[yLocation][xLocation+1] == SNAKE)
                {
                    dangerMoves.add("right");
                }
                //handles curling into it'self to the left (looking ahead by 2 squares)
                if(searchBoard[yLocation][xLocation-1] == SNAKE)
                {
                    dangerMoves.add("left");
                }
                
                dangerMoves.add("up");
            }

            for(int y = 0; y<height; y++)
            {
                for(int x = 0; x<width; x++)
                {
                    System.out.print(searchBoard[y][x]);

                }
                System.out.println();
            }
            System.out.println(dangerMoves);

            String[] posMoves = {"up", "down", "left", "right"};
            Random rand = new Random();
            while(dangerMoves.contains(move))
            {
                move = posMoves[rand.nextInt(4)];
            }

            int health = moveRequest.get("you").get("health").asInt();
            //System.out.println("Health is: " + health);

            response.put("move", move);
            return response;
        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            Map<String, String> response = new HashMap<>();
            return response;
        }
    }

}
