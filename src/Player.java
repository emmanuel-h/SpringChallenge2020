import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Player collisions
// TODO: Take care of wrap grid
// TODO: When blocked, don't use direction to test if an enemy is nearby
// TODO: Don't separate pellets of possiblePellets. Just add a pound on pellets in sight. (Almost done, now merge sets)
// TODO: Remove some useless Cases (how to know cases without pellets ?).
// TODO: Improve heuristic : Increase when a pac is nearby pellet
// TODO: Change Manhattan distance
// TODO: Decrease pound of pellet next to another pac
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
    static Set<Case> potentialPellets = new HashSet<>();
//    static Set<Case> pelletsReserved;
    static Set<Case> superPellets;
    static String move;

    static Map<Integer, Pac> allyPacs = new HashMap<>();
    static Map<Integer, Pac> enemyPacs = new HashMap<>();
    static Map<Integer, Pac> allyPacsLastMove = new HashMap<>();
    static Map<Integer, Pac> enemyPacsLastMove = new HashMap<>();

    static Map<Integer, Direction> directions = new HashMap<>();
    static int turn;

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
            allyPacs = new HashMap<>();
            enemyPacs = new HashMap<>();
            final Set<Case> pellets = new HashSet<>();
//            pelletsReserved = new HashSet<>();
            superPellets = new HashSet<>();
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
                if (value == 10) {
                    superPellets.add(aCase);
                } else {
                    pellets.add(aCase);
                }
            }
            move = "";
            potentialPellets = updatePelletsList(pellets);
            for (final Pac pac: allyPacs.values()) {
                chooseMove(pac);
            }
            System.out.println(move.substring(1));
            incrementLastSeen();
            allyPacsLastMove = allyPacs;
            enemyPacsLastMove = enemyPacs;
        }
    }

    // ALGORITHMS

    static void incrementLastSeen() {
        potentialPellets = potentialPellets.stream()
                .map(p -> {
                    p.turnLastSeen ++;
                    return p;
                })
                .collect(Collectors.toSet());
    }

    static Set<Case> updatePelletsList(final Set<Case> pellets) {
        final Set<Case> allPellets = new HashSet<>(pellets);
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

    static void chooseMove(final Pac pac) {
        final Pac enemyPac = enemyNearby(pac);
        if (enemyPac != null && !canKill(enemyPac, pac)) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(enemyPac);
        } else if (!(turn == 0) && (pac.x == allyPacsLastMove.get(pac.id).x && pac.y == allyPacsLastMove.get(pac.id).y) && !pac.hasUsedCooldownLastTurn()) {
            final Case aCase = goBackward(pac);
            move += "|MOVE " + pac.id + " " + aCase.x + " " + aCase.y;
        } else {
            final Case caseTogo = findNextPellet(pac);
            move += "|MOVE " + pac.id + " " + caseTogo.x + " " + caseTogo.y;
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

    static Pac enemyNearby(final Pac pac) {
        Pac nearestEnemy = null;
        for (final Pac enemyPac: enemyPacs.values()) {
            if ((enemyPac.x >= pac.x - 2 && enemyPac.x <= pac.x + 2) && (enemyPac.y >= pac.y - 2 && enemyPac.y <= pac.y + 2)) {
                nearestEnemy = enemyPac;
            }
        }
        return nearestEnemy;
    }

    static Case findNextPellet(final Pac pac) {
        final Case caseTogo = Stream.of(potentialPellets, superPellets)
                .flatMap(Collection::stream)
//                .filter(p -> !pelletsReserved.contains(p))
                .min(Comparator.comparing(p -> p.isWorth(pac.x, pac.y)))
                .get();
//        pelletsReserved.add(caseTogo);
        return caseTogo;
    }

    // TODO: Take care when back is a wall (side case)
    static Case goBackward(final Pac pac) {
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

        public boolean hasUsedCooldownLastTurn() {
            return this.abilityCooldown == 9;
        }

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

        public Case(final Grid type, final int x, final int y, final int value, final int turnLastSeen) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.value = value;
            this.turnLastSeen = turnLastSeen;
        }

        public int isWorth(final int playerX, final int playerY) {
            return Math.abs(playerX - this.x) + Math.abs(playerY - this.y) - this.value + 2*(turn - this.turnLastSeen);
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
