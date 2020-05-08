import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

// TODO: Player collisions
// TODO: Take care of wrap grid
// TODO: Have an updated map with pellets (remove pellets from map when eat)
// TODO: Switch form when facing an enemy
// TODO: Have an updated map with unvisited floors (go here instead of random move)
class Player {

    public static final String SCISSORS = "SCISSORS";
    public static final String ROCK = "ROCK";
    public static final String PAPER = "PAPER";
    static int myScore = 0;
    static int opponentScore = 0;
    static int width; // size of the grid
    static int height; // top left corner is (x=0, y=0)
    static int visiblePacCount; // all your pacs and enemy pacs in sight
    static int visiblePelletCount; // all pellets in sight
    static int[][] map;
    static List<Pellet> pelletsMap;
    static String move;
    static boolean firstTurn = true;

    static Map<Integer, Pac> allyPacs = new HashMap<>();
    static Map<Integer, Pac> enemyPacs = new HashMap<>();
    static Map<Integer, Pac> allyPacsLastMove = new HashMap<>();
    static Map<Integer, Pac> enemyPacsLastMove = new HashMap<>();

    static Map<Integer, Direction> directions = new HashMap<>();
    static int turn;
    static int pacWhoUsedSpeed;
    static boolean speedUsedThisTurn;

    public static void main(final String[] args) {
        final Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt();
        map = new int[width][height];

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            final String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < row.length(); j++) {
                map[j][i] = getMapInt(String.valueOf(row.charAt(j)));
            }
        }

        // game loop
        for (turn = 0; turn < 200 ; turn ++) {
            speedUsedThisTurn = false;
            allyPacs = new HashMap<>();
            enemyPacs = new HashMap<>();
            myScore = in.nextInt();
            opponentScore = in.nextInt();
            visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            for (int i = 0; i < visiblePacCount; i++) {
                final int pacId = in.nextInt(); // pac number (unique within a team)
                final boolean mine = in.nextInt() != 0; // true if this pac is yours
                final Pac pac = new Pac();
                pac.id = pacId;
                pac.x = in.nextInt();
                pac.y = in.nextInt();
                pac.typeId = in.next();
                pac.speedTurnsLeft = in.nextInt();
                pac.abilityCooldown = in.nextInt();
                pac.ally = mine;
                if (mine) {
                    allyPacs.put(pacId, pac);
                    setDirection(pac);
                } else {
                    enemyPacs.put(pacId, pac);
                }
            }
            visiblePelletCount = in.nextInt(); // all pellets in sight
            pelletsMap = new ArrayList<>();
            for (int i = 0; i < visiblePelletCount; i++) {
                final int x = in.nextInt();
                final int y = in.nextInt();
                final int value = in.nextInt(); // amount of points this pellet is worth
                pelletsMap.add(new Pellet(x, y, value));
            }
            move = "";
            for (final Pac pac: allyPacs.values()) {
                chooseMove(pac);
            }
            System.out.println(move.substring(1));
            allyPacsLastMove = allyPacs;
            enemyPacsLastMove = enemyPacs;
            firstTurn = false;
        }
    }

    // ALGORITHMS

    static void setDirection(final Pac pac) {
        final Pac lastPac;
        lastPac = allyPacsLastMove.get(pac.id);
        if (lastPac == null) {
            return;
        }

        if (pac.x > lastPac.x) {
            directions.put(pac.id, Direction.EAST);
        }
        if (pac.x < lastPac.x) {
            directions.put(pac.id, Direction.WEST);
        }
        if (pac.y > lastPac.y) {
            directions.put(pac.id, Direction.SOUTH);
        }
        if (pac.y < lastPac.y) {
            directions.put(pac.id, Direction.NORTH);
        }
    }

    static void chooseMove(final Pac pac) {
        if (!firstTurn && !(pacWhoUsedSpeed == pac.id) && (pac.x == allyPacsLastMove.get(pac.id).x && pac.y == allyPacsLastMove.get(pac.id).y)) {
            isBlocked(pac);
        } else {
            findNextPellet(pac);
        }
    }

    static String switchFormToKill(final Pac enemyPac) {
        switch (enemyPac.typeId) {
            case SCISSORS:
                return ROCK;
            case PAPER:
                return SCISSORS;
            case ROCK:
                return PAPER;
            default:
                return "";
        }
    }

    static boolean canKill(final Pac enemyPac, final Pac pac) {
        return (SCISSORS.equals(enemyPac.typeId) && ROCK.equals(pac.typeId)) || (PAPER.equals(enemyPac.typeId) && SCISSORS.equals(pac.typeId)) ||(ROCK.equals(enemyPac.typeId) && PAPER.equals(pac.typeId));
    }

    static void isBlocked(final Pac pac) {
        final Pac enemyPac = isBlockedByEnemy(pac);
        if (enemyPac != null && canKill(enemyPac, pac)) {
            final Pellet pellet = goForward(pac);
            move += "|MOVE " + pac.id + " " + pellet.x + " " + pellet.y;
        } else if (enemyPac != null) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(enemyPac);
        } else if (pac.abilityCooldown == 0) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(pac);
        } else {
            final Pellet pellet = goBack(pac);
            move += "|MOVE " + pac.id + " " + pellet.x + " " + pellet.y;
        }
    }

    static Pac isBlockedByEnemy(final Pac pac) {
        final int x = (directions.get(pac.id) == Direction.WEST) ? pac.x - 1 : (directions.get(pac.id) == Direction.EAST) ? pac.x + 1 : pac.x;
        final int x2 = (directions.get(pac.id) == Direction.WEST) ? pac.x - 2 : (directions.get(pac.id) == Direction.EAST) ? pac.x + 2 : pac.x;
        final int y = (directions.get(pac.id) == Direction.NORTH) ? pac.y - 1 : (directions.get(pac.id) == Direction.SOUTH) ? pac.y + 1 : pac.y;
        final int y2 = (directions.get(pac.id) == Direction.NORTH) ? pac.y - 2 : (directions.get(pac.id) == Direction.SOUTH) ? pac.y + 2 : pac.y;
        for (final Pac enemyPac: enemyPacs.values()) {
            if ((x == enemyPac.x && y == enemyPac.y) || (x2 == enemyPac.x && y2 == enemyPac.y)) {
                return enemyPac;
            }
        }
        return null;
    }

    static void findNextPellet(final Pac pac) {
        if (pac.abilityCooldown == 0 && !speedUsedThisTurn && giveMeAChance(4)) {
            move += "|SPEED " + pac.id;
            pacWhoUsedSpeed = pac.id;
            speedUsedThisTurn = true;
        } else {
            final Pellet pelletTogo = pelletsMap
                    .stream()
                    .max(Comparator.comparing(p -> p.isWorth(pac.x, pac.y)))
                    .orElseGet(() -> noPelletInSight(pac));
            move += "|MOVE " + pac.id + " " + pelletTogo.x + " " + pelletTogo.y;
        }
    }

    static boolean giveMeAChance(int rand) {
        return new Random().nextInt(rand) < 1;
    }

    static Pellet noPelletInSight(final Pac pac) {
        final Direction direction = directions.get(pac.id);
        // I can go in a direction without backtrack
        if (direction != Direction.WEST && (pac.x+1 < width) && (map[pac.x + 1][pac.y] == -1)) {
            return new Pellet(pac.x + 1, pac.y);
        }
        if (direction != Direction.NORTH && (pac.y+1 < height) && (map[pac.x][pac.y + 1] == -1)) {
            return new Pellet(pac.x, pac.y + 1);
        }
        if (direction != Direction.EAST && (pac.x-1 > 0) && (map[pac.x - 1][pac.y] == -1)) {
            return new Pellet(pac.x - 1, pac.y);
        }
        if (direction != Direction.SOUTH && (pac.y-1 > 0) && (map[pac.x][pac.y - 1] == -1)) {
            return new Pellet(pac.x, pac.y - 1);
        }
        // I'm forced to backtrack
        return goBack(pac);
    }

    static Pellet goBack(final Pac pac) {
        switch (directions.get(pac.id)) {
            case WEST:
                return new Pellet(pac.x + 1, pac.y);
            case EAST:
                return new Pellet(pac.x - 1, pac.y);
            case SOUTH:
                return new Pellet(pac.x, pac.y - 1);
            case NORTH:
            default:
                return new Pellet(pac.x, pac.y + 1);

        }
    }

    static Pellet goForward(final Pac pac) {
        switch (directions.get(pac.id)) {
            case WEST:
                return new Pellet(pac.x - 1, pac.y);
            case EAST:
                return new Pellet(pac.x + 1, pac.y);
            case SOUTH:
                return new Pellet(pac.x, pac.y + 1);
            case NORTH:
            default:
                return new Pellet(pac.x, pac.y - 1);

        }
    }

    // UTILS

    static int getMapInt(final String mapValue) {
        switch (mapValue) {
            case " ":
                return -1;
            case "#":
                return -2;
            default:
                return Integer.parseInt(mapValue);
        }
    }

    enum Grid {
        FLOOR,
        WALL
    }

    enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    // OBJECTS

    static class Pellet {
        int id;
        int x;
        int y;
        int value;

        public Pellet(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public Pellet(final int x, final int y, final int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public int isWorth(final int playerX, final int playerY) {
            int worth = this.value >= 10 ? 1000 : this.value;
            worth -= Math.abs(this.x - playerX);
            worth -= Math.abs(this.y - playerY);
            return worth;
        }
    }

    static class Pac {
        int id;
        int x;
        int y;
        String typeId;
        int speedTurnsLeft;
        int abilityCooldown;
        boolean ally;

        @Override
        public String toString() {
            return "Pac{" +
                    "id=" + this.id +
                    ", x=" + this.x +
                    ", y=" + this.y +
                    ", typeId='" + this.typeId + '\'' +
                    ", speedTurnsLeft=" + this.speedTurnsLeft +
                    ", abilityCooldown=" + this.abilityCooldown +
                    '}';
        }
    }
}
