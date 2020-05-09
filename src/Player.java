import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: Player collisions
// TODO: Take care of wrap grid
// TODO: When blocked, don't use direction to test if an enemy is nearby
// TODO: Don't separate pellets of possiblePellets. Just add a pound on pellets in sight.
// TODO: Remove some useless Cases (how to know cases without pellets ?).
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
    static Set<Case> pellets;
    static Set<Case> potentialPellets = new HashSet<>();
    static String move;

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

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            final String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < row.length(); j++) {
                if (' ' == (row.charAt(j))) {
                    potentialPellets.add(new Case(Grid.FLOOR, j, i, 1, 0));
                }
            }
        }

        // game loop
        for (turn = 0; turn < 200 ; turn ++) {
            speedUsedThisTurn = false;
            allyPacs = new HashMap<>();
            enemyPacs = new HashMap<>();
            pellets = new HashSet<>();
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
                if (mine) {
                    allyPacs.put(pacId, pac);
                    setDirection(pac);
                } else {
                    enemyPacs.put(pacId, pac);
                }
                potentialPellets.remove(new Case(pac.x, pac.y));
            }
            visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                final int x = in.nextInt();
                final int y = in.nextInt();
                final int value = in.nextInt(); // amount of points this pellet is worth
                final Case aCase = new Case(Grid.FLOOR, x, y, value, turn);
                pellets.add(aCase);
            }
            move = "";
            final Set<Case> allPellets = allPellets();
            for (final Pac pac: allyPacs.values()) {
                chooseMove(pac, allPellets);
            }
            System.out.println(move.substring(1));
            allyPacsLastMove = allyPacs;
            enemyPacsLastMove = enemyPacs;
        }
    }

    // ALGORITHMS

    static Set<Case> allPellets() {
        final Set<Case> allPellets = pellets.stream()
                .map(p -> {
                    p.value += 5;
                    return p;
                })
                .collect(Collectors.toSet());
        allPellets.addAll(potentialPellets);
        return allPellets;
    }

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

    static void chooseMove(final Pac pac, final Set<Case> allPellets) {
        if (!(turn == 0) && !(pacWhoUsedSpeed == pac.id) && (pac.x == allyPacsLastMove.get(pac.id).x && pac.y == allyPacsLastMove.get(pac.id).y)) {
            isBlocked(pac);
        } else {
            findNextPellet(pac, allPellets);
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
            final Case aCase = goForward(pac);
            move += "|MOVE " + pac.id + " " + aCase.x + " " + aCase.y;
        } else if (enemyPac != null) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(enemyPac);
        } else if (pac.abilityCooldown == 0) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(pac);
        } else {
            final Case aCase = goBack(pac);
            move += "|MOVE " + pac.id + " " + aCase.x + " " + aCase.y;
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

    static void findNextPellet(final Pac pac, final Set<Case> allPellets) {
        if (pac.abilityCooldown == 0 && !speedUsedThisTurn && !(turn == 0)) {
            move += "|SPEED " + pac.id;
            pacWhoUsedSpeed = pac.id;
            speedUsedThisTurn = true;
        } else{
            final Case caseTogo = allPellets.stream()
                    .max(Comparator.comparing(p -> p.isWorth(pac.x, pac.y)))
//                    .orElseGet(() ->
//                            potentialPellets.stream().max(Comparator.comparing(p -> p.isWorth(pac.x, pac.y)))
                            .get();
            pellets.remove(caseTogo);
            move += "|MOVE " + pac.id + " " + caseTogo.x + " " + caseTogo.y;
        }
    }

    // TODO: Take care when back is a wall
    static Case goBack(final Pac pac) {
        switch (directions.get(pac.id)) {
            case WEST:
                return new Case(pac.x + 1, pac.y);
            case EAST:
                return new Case(pac.x - 1, pac.y);
            case SOUTH:
                return new Case(pac.x, pac.y - 1);
            case NORTH:
            default:
                return new Case(pac.x, pac.y + 1);

        }
    }

    static Case goForward(final Pac pac) {
        switch (directions.get(pac.id)) {
            case WEST:
                return new Case(pac.x - 1, pac.y);
            case EAST:
                return new Case(pac.x + 1, pac.y);
            case SOUTH:
                return new Case(pac.x, pac.y + 1);
            case NORTH:
            default:
                return new Case(pac.x, pac.y - 1);

        }
    }

    // UTILS

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

    static boolean giveMeAChance(final int rand) {
        return new Random().nextInt(rand) < 1;
    }

    // OBJECTS

    static class Pac {
        int id;
        int x;
        int y;
        String typeId;
        int speedTurnsLeft;
        int abilityCooldown;

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

    static class Case {
        Grid type;
        int x;
        int y;
        int value;
        int turnLastSeen;

        public Case(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public Case(final Grid type, final int x, final int y, final int value, final int turnLastSee) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public int isWorth(final int playerX, final int playerY) {
            int worth = this.value >= 10 ? 1000 : this.value;
            worth -= Math.abs(playerX - this.x);
            worth -= Math.abs(playerY - this.y);
            worth -= 2 * (turn - this.turnLastSeen);
            return worth;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            final Case aCase = (Case) o;

            if (this.x != aCase.x) return false;
            return this.y == aCase.y;
        }

        @Override
        public int hashCode() {
            int result = this.x;
            result = 31 * result + this.y;
            return result;
        }

        @Override
        public String toString() {
            return "Case{" +
                    "type=" + this.type +
                    ", x=" + this.x +
                    ", y=" + this.y +
                    ", value=" + this.value +
                    ", turnLastSeen=" + this.turnLastSeen +
                    '}';
        }
    }
}
